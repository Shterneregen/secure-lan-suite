package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.diagnostics.HostResolver
import com.shterneregen.securelan.lantools.diagnostics.JvmHostResolver
import java.net.InetAddress

/** Desktop owner of the shared bounded resolver, also used on Android before API 29. */
class DesktopHostResolver(lookup: (String) -> List<IpLiteral> = { host ->
    InetAddress.getAllByName(host).mapNotNull { IpLiteral.parse(it.hostAddress) }
}) : HostResolver, AutoCloseable {
    private val delegate = JvmHostResolver(lookup)
    override suspend fun resolve(host: String) = delegate.resolve(host)
    override fun close() = delegate.close()
}
