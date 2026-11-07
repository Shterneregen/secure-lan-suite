package com.shterneregen.securelan.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.WakeRequest

internal fun ip(value: String): IpLiteral = requireNotNull(IpLiteral.parse(value))
internal val mac = MacAddress.parse("02:ab:cd:ef:12:34")!!
internal fun network(name: String = "eth0", address: String = "192.168.10.42", prefix: Int = 24) = LanInterfaceSnapshot(
    NetworkBinding("$name:$address", name, ip(address), prefix, multicast = false),
    name, true, ip("192.168.10.255"),
)
internal fun request() = WakeRequest(WakeConfiguration(mac))
internal fun networks(vararg entries: LanInterfaceSnapshot) = LanNetworkInfoProvider {
    LanResult.Success(LanNetworkSnapshot(entries.toList()))
}
