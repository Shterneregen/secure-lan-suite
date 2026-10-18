package com.shterneregen.securelan.desktop.network.discovery

import com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement
import com.shterneregen.securelan.chat.discovery.mdns.DnsSdRecordCodec
import com.shterneregen.securelan.common.net.interfaces.NetworkBindingSelector
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicit two-process multicast qualification entry point, excluded from deterministic unit suites. */
object MdnsRuntimeProbe {
    @JvmStatic fun main(args: Array<String>) {
        val bindings = NetworkBindingSelector.select(DesktopNetworkMonitor().snapshot())
        check(bindings.isNotEmpty()) { "NO_ELIGIBLE_LAN_BINDING" }
        val room = HostedRoomAdvertisement("91d44e18-c494-40b6-81c0-edc4d5539c53", "5477fc9b-d3b0-4db3-90d6-90807fb9410d", "LAN test", 5098, null, true)
        val found = CountDownLatch(1)
        val removed = CountDownLatch(1)
        val facades = bindings.map(::RealJmDnsFacade)
        try {
            if (args.single() == "host") {
                facades.forEach { it.advertise(room) }
                println("REGISTERED")
                Thread.sleep(8000)
                facades.forEach { it.advertise(null) }
                println("WITHDRAWN")
            } else {
                facades.forEach { it.browse({ info ->
                    if (DnsSdRecordCodec.decode(info.txt, info.port)?.roomInstanceId == room.roomInstanceId) found.countDown()
                }, { name -> if (name.startsWith("SecureLan-5477fc9bd3b0-")) removed.countDown() }) }
                println("BROWSING")
                check(found.await(25, TimeUnit.SECONDS)) { "RESOLVE_TIMEOUT" }
                println("RESOLVED")
                check(removed.await(20, TimeUnit.SECONDS)) { "GOODBYE_TIMEOUT" }
                println("REMOVED")
            }
        } finally { facades.forEach { it.close() }; println("CLOSED") }
    }
}
