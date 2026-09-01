package com.netsage.app.diagnostic.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservationParserTest {
    private val parser = ObservationParser()

    @Test
    fun `parses representative Chinese and English failures`() {
        val observations = parser.parse(
            """
            nslookup api.example.test returned NXDOMAIN
            DNS 请求超时，server=223.5.5.5
            connect to 10.0.0.8:443 failed: connection refused
            SSLHandshakeException: certificate has expired
            HTTP/1.1 502 Bad Gateway
            丢包率：37.5%
            检测到认证门户，需要网页登录
            """.trimIndent(),
        )

        val kinds = observations.map { it.kind }.toSet()
        assertTrue(ObservationKind.DNS_NXDOMAIN in kinds)
        assertTrue(ObservationKind.DNS_TIMEOUT in kinds)
        assertTrue(ObservationKind.TCP_REFUSED in kinds)
        assertTrue(ObservationKind.TLS_EXPIRED in kinds)
        assertTrue(ObservationKind.HTTP_STATUS in kinds)
        assertTrue(ObservationKind.PACKET_LOSS in kinds)
        assertTrue(ObservationKind.CAPTIVE_PORTAL in kinds)
        assertTrue(ObservationKind.IPV4_CLUE in kinds)
        assertEquals(
            502.0,
            observations.single { it.kind == ObservationKind.HTTP_STATUS }.numericValue
                ?: error("HTTP status value missing"),
            0.0,
        )
        assertEquals(
            37.5,
            observations.single { it.kind == ObservationKind.PACKET_LOSS }.numericValue
                ?: error("Packet loss value missing"),
            0.0,
        )
    }

    @Test
    fun `parses TLS variants and IP version clues`() {
        val observations = parser.parse(
            """
            hostname mismatch: certificate is not valid for api.example.test
            unable to get local issuer certificate
            TLS handshake failed: handshake_failure
            IPv4 192.168.1.20 works but IPv6 2001:db8::20 is unreachable
            """.trimIndent(),
        )

        val kinds = observations.map { it.kind }.toSet()
        assertTrue(ObservationKind.TLS_HOSTNAME_MISMATCH in kinds)
        assertTrue(ObservationKind.TLS_CHAIN_ERROR in kinds)
        assertTrue(ObservationKind.TLS_HANDSHAKE_FAILURE in kinds)
        assertTrue(ObservationKind.IPV4_CLUE in kinds)
        assertTrue(ObservationKind.IPV6_CLUE in kinds)
        assertTrue(ObservationKind.IP_VERSION_MISMATCH in kinds)
    }

    @Test
    fun `does not turn successful or explicitly disabled features into failures`() {
        val observations = parser.parse(
            """
            TLS handshake completed successfully
            no proxy configured
            no captive portal detected
            HTTP/2 204
            """.trimIndent(),
        )

        val kinds = observations.map { it.kind }.toSet()
        assertFalse(ObservationKind.TLS_HANDSHAKE_FAILURE in kinds)
        assertFalse(ObservationKind.PROXY_DETECTED in kinds)
        assertFalse(ObservationKind.CAPTIVE_PORTAL in kinds)
        assertEquals("204", observations.single { it.kind == ObservationKind.HTTP_STATUS }.value)
    }

    @Test
    fun `returns no observations for blank input`() {
        assertTrue(parser.parse(" \n\t").isEmpty())
    }
}
