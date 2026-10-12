package com.shterneregen.securelan.filetransfer.quickshare

import java.util.Locale

/**
 * HTML page rendering templates for the Quick Share local web server.
 *
 * Generates lightweight, styled responsive web pages shared between desktop and Android
 * clients without external HTTP template or framework dependencies.
 */
object QuickShareHtml {
    /**
     * Renders the root index HTML page displaying all active shares.
     *
     * @param snapshots List of current share snapshots.
     * @return Full HTML string.
     */
    fun indexHtml(snapshots: List<QuickShareSnapshot>): String {
        val activeShares = snapshots.filter { it.active() }
        val body = StringBuilder(baseHtmlStart("SecureLanSuite quick shares"))
        body.append("<h1>SecureLanSuite quick shares</h1>")
        body.append("<p class='warn'>No authorization is required. Anyone on this LAN who opens this page can access active shares.</p>")
        if (activeShares.isEmpty()) {
            body.append("<p>No active shares.</p>")
        } else {
            body.append("<ul>")
            for (snapshot in activeShares) {
                body.append("<li><a href='/").append(escapeHtml(snapshot.id())).append("'>")
                    .append(escapeHtml(snapshot.displayName()))
                    .append("</a> <span>")
                    .append(escapeHtml(snapshot.type().name.lowercase(Locale.ROOT)))
                    .append(" — ").append(escapeHtml(accessSummary(snapshot)))
                    .append("</span></li>")
            }
            body.append("</ul>")
        }
        body.append(baseHtmlEnd())
        return body.toString()
    }

    /**
     * Renders the details page for a shared file with a download button.
     *
     * @param snapshot Share snapshot.
     * @return Full HTML string.
     */
    fun fileItemHtml(snapshot: QuickShareSnapshot): String = baseHtmlStart(snapshot.displayName()) +
        "<h1>${escapeHtml(snapshot.displayName())}</h1>" +
        "<p>File: <strong>${escapeHtml(snapshot.fileName())}</strong></p>" +
        "<p>Size: ${snapshot.fileSize()} bytes</p>" +
        "<p>${escapeHtml(accessSummary(snapshot))}</p>" +
        "<a class='button' href='/${escapeHtml(snapshot.id())}/download'>Download file</a>" +
        "<p><a href='/'>Back to active shares</a></p>" +
        baseHtmlEnd()

    /**
     * Renders the details page for a shared text snippet with a copy button.
     *
     * @param snapshot Share snapshot.
     * @param text Shared text content.
     * @return Full HTML string.
     */
    fun textItemHtml(snapshot: QuickShareSnapshot, text: String): String = baseHtmlStart(snapshot.displayName()) +
        "<h1>${escapeHtml(snapshot.displayName())}</h1>" +
        "<p>${escapeHtml(accessSummary(snapshot))}</p>" +
        "<textarea id='sharedText' readonly>${escapeHtml(text)}</textarea>" +
        "<p><button class='button' onclick='navigator.clipboard.writeText(document.getElementById(\"sharedText\").value)'>Copy text</button></p>" +
        "<p><a href='/'>Back to active shares</a></p>" +
        baseHtmlEnd()

    /**
     * Renders an error page when a share is expired, limit-reached, or stopped.
     *
     * @param snapshot Optional share snapshot.
     * @return Full HTML string.
     */
    fun unavailableHtml(snapshot: QuickShareSnapshot?): String {
        val status = snapshot?.status()?.name?.lowercase(Locale.ROOT)?.replace('_', ' ') ?: "unavailable"
        return baseHtmlStart("Share unavailable") +
            "<h1>Share unavailable</h1>" +
            "<p>This share is ${escapeHtml(status)}.</p>" +
            "<p><a href='/'>Back to active shares</a></p>" +
            baseHtmlEnd()
    }

    /**
     * Renders a 404 Not Found HTML page.
     *
     * @return Full HTML string.
     */
    fun notFoundHtml(): String = baseHtmlStart("Not found") +
        "<h1>Not found</h1><p>The requested share does not exist.</p><p><a href='/'>Back to active shares</a></p>" +
        baseHtmlEnd()

    private fun accessSummary(snapshot: QuickShareSnapshot): String {
        val access = snapshot.accessLimit()?.let { "${snapshot.accessCount()} / $it accesses" }
            ?: "${snapshot.accessCount()} accesses · unlimited"
        val expiration = snapshot.expiresAt()?.let { "expires $it" } ?: "available until stopped"
        return "$access · $expiration"
    }

    private fun baseHtmlStart(title: String): String =
        "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>${escapeHtml(title)}</title>" +
            "<style>body{font-family:system-ui,sans-serif;max-width:820px;margin:32px auto;padding:0 16px;background:#0f172a;color:#e2e8f0}" +
            "a{color:#93c5fd}.button,button{display:inline-block;background:#2563eb;color:white;border:0;border-radius:8px;padding:10px 14px;text-decoration:none;cursor:pointer}" +
            "textarea{width:100%;min-height:220px;border-radius:8px;padding:12px;background:#020617;color:#e2e8f0;border:1px solid #334155}" +
            ".warn{background:#451a03;color:#fed7aa;padding:10px 12px;border-radius:8px}li{margin:10px 0}</style></head><body>"

    private fun baseHtmlEnd(): String = "</body></html>"

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
