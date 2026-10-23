package com.example

import com.example.data.repository.DownloadDiagnostics
import com.example.data.repository.WebScraper
import com.example.ui.components.formatBytes
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    private val scraper = WebScraper()

    @Test
    fun testValidUrlDetectionAndNormalization() {
        // Direct http/https
        assertTrue(scraper.isValidUrl("https://example.com/comic/ch1"))
        assertTrue(scraper.isValidUrl("http://manga-site.org/series/solo"))

        // Automatic normalization for URLs entered without scheme
        assertEquals("https://example.com/comic", scraper.normalizeUrl("example.com/comic"))
        assertEquals("https://asura.gg/series", scraper.normalizeUrl("   asura.gg/series   "))
        assertTrue(scraper.isValidUrl("asuracomic.net/series/tbate"))

        // Invalid inputs
        assertFalse(scraper.isValidUrl("ftp://invalid.com"))
        assertFalse(scraper.isValidUrl("not a url"))
        assertFalse(scraper.isValidUrl(""))
    }

    @Test
    fun testDiagnosticLogging() {
        DownloadDiagnostics.clear()
        DownloadDiagnostics.log("TEST_TAG", "Testing diagnostic logging")
        DownloadDiagnostics.log("TEST_ERR", "Simulated error", isError = true, details = "Code 403")

        val summary = DownloadDiagnostics.getFormattedSummary()
        assertTrue(summary.contains("ComicGrab Diagnostic Report"))
        assertTrue(summary.contains("TEST_TAG"))
        assertTrue(summary.contains("TEST_ERR"))
        assertTrue(summary.contains("Code 403"))
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", formatBytes(0L))
        assertEquals("500 B", formatBytes(500L))
        assertEquals("100 KB", formatBytes(102400L))
        assertEquals("10.0 MB", formatBytes(10485760L))
        assertEquals("1.5 GB", formatBytes(1610612736L))
    }
}
