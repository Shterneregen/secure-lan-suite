package com.shterneregen.securelan.neighbor

/** Blocking, user-triggered lookup. Callers run it off the UI thread and enforce their own timeout. */
fun interface NeighborMacResolver {
    fun resolve(query: NeighborQuery): NeighborMacResult
}
