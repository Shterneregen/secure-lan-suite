package com.shterneregen.securelan.neighbor

/** Selects one platform backend; unsupported systems never attempt a native call. */
object NeighborResolvers {
    fun forPlatform(osName: String = System.getProperty("os.name").orEmpty()): NeighborMacResolver {
        val backend = when {
            osName.startsWith("Windows", ignoreCase = true) -> WindowsSendArpResolver()
            osName.equals("Linux", ignoreCase = true) -> LinuxNeighborResolver()
            osName.startsWith("Mac", ignoreCase = true) -> MacNeighborResolver()
            else -> return NeighborMacResolver { NeighborMacResult.Unsupported }
        }
        return NeighborMacResolver { query -> NeighborMacPolicy.rejection(query) ?: backend.resolve(query) }
    }
}
