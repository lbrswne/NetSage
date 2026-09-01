package com.netsage.app.diagnostic.session

import org.junit.Assert.assertEquals
import org.junit.Test

class RetestComparatorTest {
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
}
