package com.shterneregen.securelan.androidclient.network

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.DatagramSocket

class PeerDiscoveryRepositoryTest {
    @Test
    fun cancellingDiscoveryClosesSocketWithoutFailure() = runBlocking {
        val port = DatagramSocket(0).use { it.localPort }
        val discoveryJob = launch {
            PeerDiscoveryRepository(port).discoverPeers().collect()
        }

        discoveryJob.cancelAndJoin()
    }
}
