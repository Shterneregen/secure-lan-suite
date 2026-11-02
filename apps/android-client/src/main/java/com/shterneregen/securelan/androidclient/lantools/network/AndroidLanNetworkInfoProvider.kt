package com.shterneregen.securelan.androidclient.lantools.network

import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*

/** One-shot local facts from the foreground tool's subscription; never reads a hardware MAC. */
class AndroidLanNetworkInfoProvider(private val platform: AndroidLanPlatform, private val io: CoroutineDispatcher = Dispatchers.IO) : LanNetworkInfoProvider {
    override suspend fun snapshot(): LanResult<LanNetworkSnapshot> = when (val result = read()) {
        is LanResult.Failure -> result
        is LanResult.Success -> LanResult.Success(LanNetworkSnapshot(result.value.map(::source)))
    }
    suspend fun diagnostics(): LanResult<DiagnosticsNetworkSnapshot> = when (val result = read()) {
        is LanResult.Failure -> result
        is LanResult.Success -> LanResult.Success(DiagnosticsNetworkSnapshot(result.value.map { link ->
            DiagnosticInterface(source(link), link.gateways.toList(), link.dns.toList(),
                if (link.metadataAvailable) MetadataAvailability.AVAILABLE else MetadataAvailability.UNAVAILABLE)
        }, "Android LinkProperties"))
    }
    private fun source(link: AndroidLanLink) = LanInterfaceSnapshot(link.binding, link.name, link.broadcast != null, link.broadcast)
    private suspend fun read(): LanResult<List<AndroidLanLink>> = withContext(io) {
        try {
            currentCoroutineContext().ensureActive()
            val links = platform.links()
            if (links.size > 512 || links.any { it.dns.size > 32 || it.gateways.size > 32 }) LanResult.Failure(LanError.RESOURCE_LIMIT)
            else LanResult.Success(links)
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: SecurityException) { LanResult.Failure(LanError.PERMISSION_DENIED) }
        catch (_: Exception) { LanResult.Failure(LanError.IO) }
    }
}
