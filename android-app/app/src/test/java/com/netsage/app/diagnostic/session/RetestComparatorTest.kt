package com.netsage.app.diagnostic.session

import org.junit.Assert.assertEquals
import org.junit.Test

class RetestComparatorTest {
    @Test
    fun `tcp comparison preserves before and after attempts for both address families`() {
        val before = sessionWithTcp("1/3", "0/3")
        val after = sessionWithTcp("3/3", "2/3")

        val comparison = RetestComparator.compare(before, after)

        assertEquals(
            "IPv4 TCP 建连成功：前 1/3 → 后 3/3；IPv6 TCP 建连成功：前 0/3 → 后 2/3",
            comparison.probeComparisons.single().summary,
        )
    }

    @Test
    fun `tcp comparison does not invent counts for unavailable family`() {
        val before = sessionWithTcp("unavailable", "0/3")
        val after = sessionWithTcp("unavailable", "1/3")

        val comparison = RetestComparator.compare(before, after)

        assertEquals("IPv6 TCP 建连成功：前 0/3 → 后 1/3", comparison.probeComparisons.single().summary)
    }

    @Test
    fun `http status code improvement is detected even when observation status is unchanged`() {
        val before = sessionWithHttp(503, ProbeStatus.FAILED)
        val after = sessionWithHttp(404, ProbeStatus.WARNING)

        val comparison = RetestComparator.compare(before, after)

        assertEquals(ProbeChange.IMPROVED, comparison.probeComparisons.single().change)
    }

    @Test
    fun `different http client errors are not reported unchanged`() {
        val before = sessionWithHttp(404, ProbeStatus.WARNING)
        val after = sessionWithHttp(401, ProbeStatus.WARNING)

        val comparison = RetestComparator.compare(before, after)

        assertEquals(ProbeChange.INCONCLUSIVE, comparison.probeComparisons.single().change)
    }

    private fun sessionWithHttp(statusCode: Int, status: ProbeStatus) = DiagnosticSession(
        observations = listOf(
            ProbeObservation(
                sequence = 1,
                type = ProbeType.HTTP,
                status = status,
                target = "https://example.com:443/",
                attributes = mapOf("statusCode" to statusCode.toString()),
            )
        )
    )

    private fun sessionWithTcp(ipv4: String, ipv6: String) = DiagnosticSession(
        observations = listOf(ProbeObservation(
            sequence = 1,
            type = ProbeType.TCP,
            status = ProbeStatus.FAILED,
            target = "example.com:443",
            attributes = mapOf("ipv4Connections" to ipv4, "ipv6Connections" to ipv6),
        )),
    )
}
