package com.netsage.app.diagnostic.probe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidProbeRunnerTest {
    @Test
    fun `http client errors are warnings and server errors fail`() {
        assertEquals("SUCCESS", httpResultStatus(200))
        assertEquals("WARNING", httpResultStatus(404))
        assertEquals("FAILED", httpResultStatus(503))
    }

    @Test
    fun `same host redirect comparison is case insensitive and blocks a different host`() {
        assertTrue(isSameRedirectHost("Example.COM.", "example.com"))
        assertFalse(isSameRedirectHost("example.com", "redirect.example.net"))
    }
}
