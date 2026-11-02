package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.diagnostics.MetadataAvailability
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Validated numeric route/DNS metadata keyed by a local IPv4 address, never by a friendly alias. */
data class PlatformNetworkRecord(val gateways: List<IpLiteral>, val dnsServers: List<String>)
data class PlatformNetworkInfo(val availability: MetadataAvailability, val records: Map<String, PlatformNetworkRecord> = emptyMap())

/**
 * Optional local Windows facts. The fixed script emits numeric fields in a private line protocol;
 * it never interpolates user input, scans the LAN or relies on localized display-table parsing.
 * Missing cmdlets/permissions and invalid/oversized output are explicitly unavailable, not invented.
 */
class WindowsNetworkInfoBackend(
    private val osName: String = System.getProperty("os.name", ""),
    private val run: suspend () -> String = { runWindowsQuery() },
) {
    suspend fun read(): PlatformNetworkInfo {
        if (!osName.startsWith("Windows", ignoreCase = true)) return PlatformNetworkInfo(MetadataAvailability.UNSUPPORTED)
        return try {
            withTimeoutOrNull(5_000) { parse(run()) } ?: PlatformNetworkInfo(MetadataAvailability.TIMEOUT)
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: SecurityException) { PlatformNetworkInfo(MetadataAvailability.PERMISSION_DENIED) }
        catch (_: Exception) { PlatformNetworkInfo(MetadataAvailability.UNAVAILABLE) }
    }

    companion object {
        /** Strict bounded protocol decoder; a bad record invalidates the snapshot, never a partial match. */
        internal fun parse(text: String): PlatformNetworkInfo {
            fun failed() = PlatformNetworkInfo(MetadataAvailability.UNAVAILABLE)
            if (text.length > 131_072 || text.any { it.code > 127 }) return failed()
            val lines = text.lineSequence().filter(String::isNotBlank).toList()
            if (lines.firstOrNull() != "SecureLanNetworkInfo/1" || lines.size > 513) return failed()
            val records = linkedMapOf<String, PlatformNetworkRecord>()
            for (line in lines.drop(1)) {
                val fields = line.split('|')
                if (fields.size != 3) return failed()
                val local = IpLiteral.parse(fields[0])?.takeIf { it.isIpv4 && it.isUnicast } ?: return failed()
                fun values(raw: String): List<String>? {
                    if (raw.isEmpty()) return emptyList()
                    val values = raw.split(',')
                    if (values.size > 32 || values.any { value ->
                        val parts = value.split('%')
                        val ip = IpLiteral.parse(parts[0])
                        ip == null || !(ip.isUnicast || ip.value == "::1" || ip.isIpv4 && parts[0].startsWith("127.")) ||
                            parts.size > 2 || parts.size == 2 && (ip.isIpv4 || parts[1].toIntOrNull()?.let { it > 0 } != true)
                    }) return null
                    return values.distinct()
                }
                val gateways = (values(fields[1]) ?: return failed()).map {
                    IpLiteral.parse(it)?.takeIf { value -> value.isIpv4 && value.isUnicast } ?: return failed()
                }
                val dns = values(fields[2]) ?: return failed()
                val record = PlatformNetworkRecord(gateways, dns)
                if (records[local.value]?.let { it != record } == true) return failed()
                records[local.value] = record
            }
            return PlatformNetworkInfo(MetadataAvailability.AVAILABLE, records.toMap())
        }
    }
}

/** Polls a bounded pipe instead of blocking a reader thread indefinitely on process output. */
private suspend fun runWindowsQuery(): String = withContext(Dispatchers.IO) {
    val root = System.getenv("SystemRoot") ?: throw IOException("System root unavailable")
    val executable = Path.of(root, "System32", "WindowsPowerShell", "v1.0", "powershell.exe")
    runBoundedPlatformProcess({ ProcessBuilder(executable.toString(), "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", WINDOWS_QUERY)
        .redirectErrorStream(true).start() })
}

/** Internal process seam for deterministic deadline/output/cleanup tests. No command text is built here. */
internal suspend fun runBoundedPlatformProcess(start: () -> Process, dispatcher: CoroutineDispatcher = Dispatchers.IO): String = withContext(dispatcher) {
    currentCoroutineContext().ensureActive()
    val process = start()
    try {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            currentCoroutineContext().ensureActive()
            val available = process.inputStream.available()
            if (available > 0) {
                val read = process.inputStream.read(buffer, 0, minOf(buffer.size, available))
                if (read > 0) output.write(buffer, 0, read)
                if (output.size() > 131_072) throw IOException("Output limit")
            } else if (!process.isAlive) break else delay(10)
        }
        if (process.exitValue() != 0) throw IOException("Platform query failed")
        String(output.toByteArray(), StandardCharsets.US_ASCII)
    } finally {
        if (process.isAlive) runCatching { process.destroyForcibly(); process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS) }
        // Cleanup failures must not mask the coroutine's timeout/cancellation.
        runCatching { process.inputStream.close() }; runCatching { process.errorStream.close() }; runCatching { process.outputStream.close() }
    }
}

// Only local configuration is requested. No hostname, port, address or credential is interpolated.
private const val WINDOWS_QUERY = """
${'$'}ErrorActionPreference = 'Stop'; ${'$'}ProgressPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::ASCII
${'$'}configurations = @(Get-NetIPConfiguration -All)
Write-Output 'SecureLanNetworkInfo/1'
foreach (${'$'}c in ${'$'}configurations) {
    ${'$'}gateways = @(${'$'}c.IPv4DefaultGateway | ForEach-Object { ${'$'}_.NextHop }) -join ','
    ${'$'}dns = @(${'$'}c.DNSServer | ForEach-Object { ${'$'}_.ServerAddresses } | Select-Object -Unique) -join ','
    foreach (${'$'}a in ${'$'}c.IPv4Address) {
        if (${'$'}a.IPAddress -ne '127.0.0.1') { Write-Output (${'$'}a.IPAddress + '|' + ${'$'}gateways + '|' + ${'$'}dns) }
    }
}
"""
