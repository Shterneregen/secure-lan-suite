package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DesktopHostResolverTest {
    @Test fun resolvesDeduplicatesAndMapsErrorsWithoutExternalDns() = runBlocking {
        DesktopHostResolver { listOf(IpLiteral.parse("127.0.0.1")!!, IpLiteral.parse("127.0.0.1")!!) }.use { resolver ->
            assertEquals(1, (resolver.resolve("test.local") as LanResult.Success).value.size)
        }
        DesktopHostResolver { throw UnknownHostException("private") }.use {
            assertEquals(LanResult.Failure(LanError.RESOLUTION_FAILED), it.resolve("test.local"))
        }
        DesktopHostResolver { throw SecurityException("private") }.use {
            assertEquals(LanResult.Failure(LanError.PERMISSION_DENIED), it.resolve("test.local"))
        }
    }

    @Test fun cancelledNativeLookupsKeepSlotsOccupiedAndDoNotAccumulateThreads() = runBlocking {
        val entered = CountDownLatch(2)
        val release = CountDownLatch(1)
        val resolver = DesktopHostResolver {
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
            listOf(IpLiteral.parse("127.0.0.1")!!)
        }
        try {
            val first = async { resolver.resolve("first.local") }
            val second = async { resolver.resolve("second.local") }
            yield()
            assertTrue(withContext(Dispatchers.IO) { entered.await(2, TimeUnit.SECONDS) })
            first.cancelAndJoin(); second.cancelAndJoin()
            repeat(10) { assertEquals(LanResult.Failure(LanError.RESOURCE_LIMIT), resolver.resolve("next.local")) }
        } finally { release.countDown(); resolver.close() }
    }

    @Test fun shutdownCancelsPendingCallersAndRejectsNewOnes() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val resolver = DesktopHostResolver { entered.countDown(); release.await(5, TimeUnit.SECONDS); emptyList() }
        try {
            val job = async { resolver.resolve("test.local") }
            yield()
            assertTrue(withContext(Dispatchers.IO) { entered.await(2, TimeUnit.SECONDS) })
            resolver.close()
            job.join()
            assertTrue(job.isCancelled)
            assertEquals(LanResult.Failure(LanError.NETWORK_UNAVAILABLE), resolver.resolve("test.local"))
        } finally { release.countDown(); resolver.close() }
    }
}
