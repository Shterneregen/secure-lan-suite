package com.shterneregen.securelan.filetransfer.quickshare

import com.shterneregen.securelan.common.net.NetworkConstants
import java.util.Objects

/**
 * Configuration options for the local Quick Share HTTP server.
 *
 * @param port TCP port to bind (0 to pick an ephemeral port, defaults to [NetworkConstants.DEFAULT_QUICK_SHARE_PORT]).
 * @param advertisedHosts Explicit hostnames or IP addresses to use when formatting share URLs.
 *                        If empty, active local LAN IPv4 addresses are discovered automatically.
 */
class QuickShareServerConfig @JvmOverloads constructor(
    port: Int = NetworkConstants.DEFAULT_QUICK_SHARE_PORT,
    advertisedHosts: List<String> = listOf(),
) {
    private val portValue: Int
    private val advertisedHostsValue: List<String>

    init {
        val requiredAdvertisedHosts = Objects.requireNonNull(advertisedHosts, "advertisedHosts must not be null")!!
        require(port in 0..65_535) { "port must be between 0 and 65535" }
        portValue = port
        advertisedHostsValue = requiredAdvertisedHosts
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    /**
     * Requested or configured TCP port.
     */
    fun port(): Int = portValue

    /**
     * Configured advertised hosts/IPs.
     */
    fun advertisedHosts(): List<String> = advertisedHostsValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QuickShareServerConfig) return false
        return portValue == other.portValue && advertisedHostsValue == other.advertisedHostsValue
    }

    override fun hashCode(): Int = Objects.hash(portValue, advertisedHostsValue)

    override fun toString(): String = "QuickShareServerConfig[port=$portValue, advertisedHosts=$advertisedHostsValue]"
}
