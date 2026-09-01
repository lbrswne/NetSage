package com.netsage.app.viewmodel

import com.netsage.app.diagnostic.probe.NetworkSnapshot
import com.netsage.app.diagnostic.probe.NetworkTransport
import com.netsage.app.diagnostic.session.EvidenceStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticFlowViewModelTest {
    @Test
    fun `disconnected snapshot produces explicit offline hypothesis`() {
        val hypothesis = disconnectedNetworkHypothesis(snapshot(connected = false))

        requireNotNull(hypothesis)
        assertEquals("network.device_offline", hypothesis.code)
        assertEquals(95, hypothesis.priority)
        assertEquals(EvidenceStrength.HIGH, hypothesis.evidenceStrength)
    }

    @Test
    fun `connected snapshot does not produce offline hypothesis`() {
        assertNull(disconnectedNetworkHypothesis(snapshot(connected = true)))
    }

    @Test
    fun `connected snapshot emits missing gateway and DNS evidence`() {
        val evidence = snapshotEvidenceLines(snapshot(connected = true))

        assertTrue("default gateway missing" in evidence)
        assertTrue("DNS configuration missing" in evidence)
    }

    @Test
    fun `disconnected snapshot does not emit configuration evidence`() {
        val evidence = snapshotEvidenceLines(snapshot(connected = false))

        assertFalse("default gateway missing" in evidence)
        assertFalse("DNS configuration missing" in evidence)
    }

    private fun snapshot(connected: Boolean) = NetworkSnapshot(
        capturedAtEpochMillis = 1L,
        connected = connected,
        internetCapable = connected,
        validated = connected,
        captivePortal = false,
        metered = false,
        transports = if (connected) setOf(NetworkTransport.WIFI) else emptySet(),
        interfaceName = null,
        ipAddresses = emptyList(),
        gateways = emptyList(),
        dnsServers = emptyList(),
        mtu = null,
        privateDnsActive = false,
        privateDnsServerName = null,
        proxyHost = null,
        proxyPort = null,
    )
}
