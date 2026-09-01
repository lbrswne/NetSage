package com.netsage.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDocumentIoTest {
    @Test
    fun `log at the limit is retained without a truncation flag`() {
        val result = LocalDocumentIo.limitLogText("x".repeat(LocalDocumentIo.MAX_LOG_CHARS))

        assertEquals(LocalDocumentIo.MAX_LOG_CHARS, result.text.length)
        assertFalse(result.truncated)
    }

    @Test
    fun `log above the limit is marked as truncated`() {
        val result = LocalDocumentIo.limitLogText("x".repeat(LocalDocumentIo.MAX_LOG_CHARS + 1))

        assertEquals(LocalDocumentIo.MAX_LOG_CHARS, result.text.length)
        assertTrue(result.truncated)
    }
}
