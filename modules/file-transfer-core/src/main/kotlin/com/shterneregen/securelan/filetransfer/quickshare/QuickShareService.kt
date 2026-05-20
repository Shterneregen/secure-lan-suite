package com.shterneregen.securelan.filetransfer.quickshare

import java.io.IOException
import java.util.Optional

interface QuickShareService : AutoCloseable {
    @Throws(IOException::class)
    fun start(config: QuickShareServerConfig)

    fun stop()

    fun isRunning(): Boolean

    fun port(): Int

    @Throws(IOException::class)
    fun share(request: QuickShareCreateRequest): QuickShareSnapshot

    fun findShare(id: String): Optional<QuickShareSnapshot>

    fun shares(): List<QuickShareSnapshot>

    fun stopShare(id: String): Boolean

    fun landingUrls(): List<String>

    override fun close() = stop()
}
