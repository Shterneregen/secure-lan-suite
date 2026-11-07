package com.shterneregen.securelan.lantools.wol

import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.BindException
import java.net.NoRouteToHostException
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * One cancellable send, independent of room connectivity.
 *
 * @property configuration Target preferences; all scalar values are revalidated before local IO.
 * @property bindingKey Explicit current source binding, overriding a saved interface preference.
 * @property timeoutMillis Whole-operation deadline in milliseconds, from 1 to 30000 inclusive.
 */
data class WakeRequest(
    val configuration: WakeConfiguration,
    val bindingKey: String? = null,
    val timeoutMillis: Long = 2_000,
)

/**
 * The OS accepted a datagram; delivery and physical wake-up are not confirmed.
 *
 * @property route Actual source binding, target broadcast and UDP port used by the sender.
 * @property bytesSent Standard packet length accepted by the transport.
 * @property sentAt Wall-clock completion time for display, not a monotonic operation timer.
 */
data class WakeReceipt(val route: WakeRoute, val bytesSent: Int, val sentAt: Instant)

/** Application-facing WoL boundary; callers own the coroutine lifetime and user feedback. */
interface WakeOnLanService {
    /** Returns an OS-submission receipt or a typed failure; caller cancellation propagates. */
    suspend fun send(request: WakeRequest): LanResult<WakeReceipt>
}

/**
 * Each call owns and closes its resources; implementations must cooperate with cancellation.
 * Direct callers supply their own deadline. [DefaultWakeOnLanService] applies one automatically.
 */
fun interface DatagramSender {
    /** Submits exactly one complete datagram or throws; never reports successful partial sends. */
    suspend fun send(route: WakeRoute, payload: ByteArray)
}

/**
 * Room-independent orchestration. At most four operations per service; no unbounded waiting queue.
 *
 * @param networks Bounded, cancellation-cooperative local snapshot reader.
 * @param sender Transport owning its per-call resources; Android may inject a network-bound adapter.
 * @param clock Wall clock for receipts only; coroutine deadlines govern operation duration.
 */
class DefaultWakeOnLanService(
    private val networks: LanNetworkInfoProvider,
    private val sender: DatagramSender = JvmDatagramSender(),
    private val clock: Clock = Clock.systemUTC(),
) : WakeOnLanService {
    private val permits = Semaphore(4)

    override suspend fun send(request: WakeRequest): LanResult<WakeReceipt> {
        currentCoroutineContext().ensureActive()
        WakeTargetResolver.validate(request)?.let { return it }
        if (!permits.tryAcquire()) return LanResult.Failure(LanError.RESOURCE_LIMIT)
        try {
            // withTimeoutOrNull handles only this deadline; an enclosing timeout still propagates.
            return withTimeoutOrNull(request.timeoutMillis.milliseconds) { sendMapped(request) }
                ?: LanResult.Failure(LanError.TIMEOUT)
        } finally {
            permits.release()
        }
    }

    private suspend fun sendMapped(request: WakeRequest): LanResult<WakeReceipt> {
        try {
            return sendValidated(request)
        } catch (_: SecurityException) {
            currentCoroutineContext().ensureActive()
            return LanResult.Failure(LanError.PERMISSION_DENIED)
        } catch (_: UnsupportedOperationException) {
            currentCoroutineContext().ensureActive()
            return LanResult.Failure(LanError.UNSUPPORTED)
        } catch (error: IOException) {
            // A socket closed concurrently by cancellation must not turn cancellation into an IO error.
            currentCoroutineContext().ensureActive()
            return LanResult.Failure(when (error) {
                is BindException -> LanError.NETWORK_CHANGED
                is NoRouteToHostException -> LanError.NETWORK_UNAVAILABLE
                else -> LanError.IO
            })
        }
    }

    private suspend fun sendValidated(request: WakeRequest): LanResult<WakeReceipt> {
        val snapshot = when (val result = networks.snapshot()) {
            is LanResult.Failure -> return result
            is LanResult.Success -> result.value
        }
        val route = when (val result = WakeTargetResolver.resolve(request, snapshot)) {
            is LanResult.Failure -> return result
            is LanResult.Success -> result.value
        }
        // Re-resolve just before opening the send resource; never fall back to another NIC.
        val fresh = when (val result = networks.snapshot()) {
            is LanResult.Failure -> return result
            is LanResult.Success -> result.value
        }
        if (fresh.interfaces.none { it == route.network }) return LanResult.Failure(LanError.NETWORK_CHANGED)
        currentCoroutineContext().ensureActive()
        sender.send(route, MagicPacket.create(request.configuration.mac))
        currentCoroutineContext().ensureActive()
        return LanResult.Success(WakeReceipt(route, MagicPacket.SIZE, clock.instant()))
    }
}
