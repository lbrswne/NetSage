package com.netsage.app.ui

import com.netsage.app.diagnostic.session.DiagnosticHypothesis
import com.netsage.app.diagnostic.session.DiagnosticSession
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticUiMapperTest {
    @Test
    fun `all recommended actions are preserved for the result UI`() {
        val actions = listOf("检查 DNS 设置", "更换 DNS 后复测", "记录复测结果")

        val result = DiagnosticSession(
            hypotheses = listOf(DiagnosticHypothesis(recommendedActions = actions)),
        ).toResultUi()

        assertEquals(actions, result.hypotheses.single().actions)
    }

    @Test
    fun `rationale is shown as a fallback repair step`() {
        val result = DiagnosticSession(
            hypotheses = listOf(DiagnosticHypothesis(rationale = "补充故障时间")),
        ).toResultUi()

        assertEquals(listOf("补充故障时间"), result.hypotheses.single().actions)
    }
}
