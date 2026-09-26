package com.netsage.app.ui

import com.netsage.app.diagnostic.session.DiagnosticHypothesis
import com.netsage.app.diagnostic.session.DiagnosticSession
import com.netsage.app.diagnostic.session.ProbeObservation
import com.netsage.app.diagnostic.session.ProbeStatus
import com.netsage.app.diagnostic.session.ProbeType
import com.netsage.app.diagnostic.session.ProbeRetestComparison
import com.netsage.app.diagnostic.session.RetestComparison
import com.netsage.app.diagnostic.session.REFERENCE_HOST_KEY
import com.netsage.app.diagnostic.session.REFERENCE_ROLE
import com.netsage.app.diagnostic.session.TARGET_ROLE_KEY
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticUiMapperTest {
    @Test
    fun `DNS result exposes grouped address details from stored evidence`() {
        val result = DiagnosticSession(
            observations = listOf(ProbeObservation(
                type = ProbeType.DNS,
                status = ProbeStatus.SUCCESS,
                attributes = mapOf(
                    "addresses" to "2001:db8::1, 192.0.2.1",
                    "resolver" to "DnsResolver",
                    "dnsRcodeAvailable" to "available",
                ),
            )),
        ).toResultUi()

        assertEquals(
            listOf("DNS 解析成功", "IPv4：192.0.2.1", "IPv6：2001:db8::1", "解析器：DnsResolver", "RCODE：可获取，未记录具体值"),
            result.probes.single().evidence,
        )
    }

    @Test
    fun `changed network warning precedes retest evidence and tcp counts are visible`() {
        val result = DiagnosticSession(
            retestComparison = RetestComparison(
                networkChanged = true,
                probeComparisons = listOf(ProbeRetestComparison(
                    probeType = ProbeType.TCP,
                    target = "example.com:443",
                    beforeStatus = ProbeStatus.FAILED,
                    afterStatus = ProbeStatus.SUCCESS,
                    summary = "IPv4 TCP 建连成功：前 1/3 → 后 3/3；IPv6 TCP 建连成功：前 0/3 → 后 2/3",
                )),
            ),
        ).toResultUi()

        assertEquals("⚠ 网络环境已变化：前后差异不能归因于修复操作。", result.comparisonLines.first())
        assertEquals(
            "TCP example.com:443：INCONCLUSIVE，FAILED → SUCCESS · IPv4 TCP 建连成功：前 1/3 → 后 3/3；IPv6 TCP 建连成功：前 0/3 → 后 2/3",
            result.comparisonLines.last(),
        )
    }

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

    @Test
    fun `result distinguishes reference evidence from primary evidence`() {
        val result = DiagnosticSession(
            metadata = mapOf(REFERENCE_HOST_KEY to "reference.test"),
            observations = listOf(
                ProbeObservation(type = ProbeType.HTTP, status = ProbeStatus.FAILED),
                ProbeObservation(
                    type = ProbeType.HTTP,
                    status = ProbeStatus.SUCCESS,
                    attributes = mapOf(TARGET_ROLE_KEY to REFERENCE_ROLE),
                ),
            ),
        ).toResultUi()

        assertEquals(listOf("主目标", "对照目标"), result.probes.map { it.role })
        assertEquals("reference.test", result.referenceTarget)
        assertEquals(
            "对照目标通过检测，主目标未通过；优先检查主目标服务及其访问路径。",
            result.referenceSummary,
        )
    }

    @Test
    fun `result surfaces per-family TCP counts without a loss claim`() {
        val result = DiagnosticSession(
            observations = listOf(ProbeObservation(
                type = ProbeType.TCP,
                status = ProbeStatus.FAILED,
                attributes = mapOf(
                    "ipv4Address" to "192.0.2.1",
                    "ipv4Connections" to "2/3",
                    "ipv6Connections" to "unavailable",
                ),
            )),
        ).toResultUi()

        assertEquals(listOf("主目标 · IPv4 192.0.2.1：2/3 次 TCP 建连成功；IPv6：未解析到地址"), result.tcpLines)
    }
}
