package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.MacAddress

/** Bounded result of one local neighbor-table lookup. A found MAC is an observation, not identity. */
sealed interface NeighborMacResult {
    data class Found(val mac: MacAddress) : NeighborMacResult

    data object NotFound : NeighborMacResult

    data object OffLink : NeighborMacResult

    data object InvalidTarget : NeighborMacResult

    data object Unsupported : NeighborMacResult

    data class Failed(val reason: NeighborFailure) : NeighborMacResult
}

enum class NeighborFailure {
    PERMISSION_DENIED,
    INTERFACE_UNAVAILABLE,
    IO,
    TIMEOUT,
    UNEXPECTED,
}
