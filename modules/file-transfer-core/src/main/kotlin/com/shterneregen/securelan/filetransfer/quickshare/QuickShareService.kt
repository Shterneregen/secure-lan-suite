package com.shterneregen.securelan.filetransfer.quickshare

import java.io.IOException
import java.util.Optional

/**
 * Service managing temporary, unauthenticated local HTTP shares (files and text snippets)
 * across the local area network.
 *
 * Designed to provide quick one-off transfers to peers on a trusted LAN without requiring
 * client pairing, symmetric keys, or dedicated client apps.
 */
interface QuickShareService : AutoCloseable {
    /**
     * Starts the Quick Share HTTP server with the provided configuration.
     *
     * @param config Server binding and host configuration.
     * @throws IOException If the server cannot bind to an available port.
     */
    @Throws(IOException::class)
    fun start(config: QuickShareServerConfig)

    /**
     * Stops the running HTTP server and terminates all active shares.
     */
    fun stop()

    /**
     * Returns `true` if the HTTP server is currently running.
     */
    fun isRunning(): Boolean

    /**
     * Returns the bound TCP port of the running server, or `0` if stopped.
     */
    fun port(): Int

    /**
     * Creates and publishes a new temporary share.
     *
     * @param request Share definition containing payload, duration, and limit.
     * @return Snapshot of the newly created share.
     * @throws IOException If file inspection or initial registration fails.
     * @throws IllegalStateException If the server is not currently running.
     */
    @Throws(IOException::class)
    fun share(request: QuickShareCreateRequest): QuickShareSnapshot

    /**
     * Looks up a share by its unique identifier.
     *
     * @param id Unique share identifier.
     * @return Optional containing the share snapshot if found, or empty.
     */
    fun findShare(id: String): Optional<QuickShareSnapshot>

    /**
     * Returns all registered shares (active and expired/stopped), ordered newest first.
     */
    fun shares(): List<QuickShareSnapshot>

    /**
     * Manually terminates an active share by its identifier.
     *
     * @param id Identifier of the share to stop.
     * @return `true` if the share was found and stopped; `false` otherwise.
     */
    fun stopShare(id: String): Boolean

    /**
     * Returns base landing URLs for the root index page on each advertised network interface.
     */
    fun landingUrls(): List<String>

    /**
     * Stops the service. Equivalent to [stop].
     */
    override fun close() = stop()
}
