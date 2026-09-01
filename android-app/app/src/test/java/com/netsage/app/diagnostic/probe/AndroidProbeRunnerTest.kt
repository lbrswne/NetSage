package com.netsage.app.diagnostic.probe

import java.net.UnknownHostException
import java.security.cert.CertificateException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLHandshakeException
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

    @Test
    fun `Android 9 InetAddress fallback records unavailable RCODE instead of a synthetic code`() {
        val failure = inetAddressFallbackFailure("missing.example", UnknownHostException("missing.example"))

        assertEquals("DNS_LOOKUP_FAILED", failure.code)
        assertEquals("unavailable", failure.evidence["dnsRcodeAvailable"])
        assertEquals("InetAddressFallback", failure.evidence["resolver"])
        assertTrue(failure.message.orEmpty().contains("RCODE unavailable"))
        assertFalse(failure.code.contains("NXDOMAIN"))
        assertFalse(failure.code.contains("SERVFAIL"))
    }

    @Test
    fun `certificate path cause inside TLS handshake is classified as chain failure`() {
        val error = SSLHandshakeException("TLS handshake failed").apply {
            initCause(CertPathValidatorException("Trust anchor for certification path not found"))
        }

        assertTrue(isTlsChainFailure(error.cause!!))
        assertEquals("TLS_CHAIN_ERROR", error.probeErrorCode(ProbeKind.TLS))
    }

    @Test
    fun `certificate exception and trust anchor text are classified as chain failures`() {
        assertTrue(isTlsChainFailure(CertificateException("untrusted issuer")))
        assertTrue(isTlsChainFailure(IllegalStateException("Unable to get local issuer certificate")))
        assertFalse(isTlsChainFailure(SSLHandshakeException("protocol_version")))
    }
}
