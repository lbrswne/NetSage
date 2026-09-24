package com.netsage.app.diagnostic.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceComparisonTest {
    @Test
    fun `only primary failure with reachable reference points to primary path`() {
        val session = session(ProbeStatus.FAILED, ProbeStatus.SUCCESS)

        assertEquals(
            "对照目标通过检测，主目标未通过；优先检查主目标服务及其访问路径。",
            session.referenceComparisonSummary(),
        )
    }

    @Test
    fun `network switch prevents direct comparison`() {
        val session = session(ProbeStatus.FAILED, ProbeStatus.SUCCESS).copy(
            metadata = mapOf(REFERENCE_HOST_KEY to "reference.test", REFERENCE_NETWORK_CHANGED_KEY to "true"),
        )

        assertEquals(
            "两次检测期间网络环境发生变化，暂不能直接比较；请在同一网络下重新检测。",
            session.referenceComparisonSummary(),
        )
    }

    @Test
    fun `single target has no comparison`() {
        assertNull(DiagnosticSession().referenceComparisonSummary())
    }

    @Test
    fun `incomplete reference is not counted as a failure`() {
        val session = session(ProbeStatus.FAILED, ProbeStatus.SUCCESS).copy(
            observations = listOf(ProbeObservation(type = ProbeType.HTTP, status = ProbeStatus.FAILED)),
        )

        assertEquals("对照检测未完成，暂不能比较。", session.referenceComparisonSummary())
    }

    @Test
    fun `export labels both targets and includes the comparison`() {
        val markdown = SessionExporter().toMarkdown(session(ProbeStatus.FAILED, ProbeStatus.SUCCESS))

        assertTrue(markdown.contains("对照目标：`reference.test`"))
        assertTrue(markdown.contains("对照结论：对照目标通过检测"))
        assertTrue(markdown.contains("主目标 · HTTP"))
        assertTrue(markdown.contains("对照目标 · HTTP"))
    }

    private fun session(primary: ProbeStatus, reference: ProbeStatus) = DiagnosticSession(
        metadata = mapOf(REFERENCE_HOST_KEY to "reference.test", REFERENCE_NETWORK_CHANGED_KEY to "false"),
        observations = listOf(
            ProbeObservation(type = ProbeType.HTTP, status = primary, target = "primary.test"),
            ProbeObservation(
                type = ProbeType.HTTP,
                status = reference,
                target = "reference.test",
                attributes = mapOf(TARGET_ROLE_KEY to REFERENCE_ROLE),
            ),
        ),
    )
}
