package com.shterneregen.securelan.androidclient.lantools

import com.shterneregen.securelan.androidclient.ui.lantools.localCheckTime
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class LanCheckTimeTest {
    @Test fun timeUsesLocalDateBoundaryAndIncludesDateForOlderChecks() {
        val at = Instant.parse("2026-11-04T13:08:20Z")
        val zone = ZoneId.of("Europe/Moscow")
        assertEquals("16:08", localCheckTime(at, at.plusSeconds(60), zone, Locale.UK))
        val old = localCheckTime(at, at.plusSeconds(86400), zone, Locale.UK)
        assertTrue(old.contains("04/11/2026") || old.contains("04/11/26"))
        assertFalse(old.contains("T13:"))
        assertNotEquals(localCheckTime(at, at, zone, Locale.US), localCheckTime(at, at, zone, Locale.UK))
    }
}
