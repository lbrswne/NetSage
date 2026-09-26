package com.netsage.app.ui

import com.netsage.app.diagnostic.session.DiagnosticSession
import com.netsage.app.diagnostic.session.DiagnosticSessionMode
import com.netsage.app.diagnostic.session.EvidenceStrength
import com.netsage.app.diagnostic.session.REFERENCE_HOST_KEY
import com.netsage.app.diagnostic.session.REFERENCE_ROLE
import com.netsage.app.diagnostic.session.ProbeType
import com.netsage.app.diagnostic.session.TARGET_ROLE_KEY
import com.netsage.app.diagnostic.session.referenceComparisonSummary
import com.netsage.app.ui.screen.DiagnosticHistoryItemUi
import com.netsage.app.ui.screen.DiagnosticSessionUi
import com.netsage.app.ui.screen.HypothesisUi
import com.netsage.app.ui.screen.ProbeResultUi

fun DiagnosticSession.toResultUi(): DiagnosticSessionUi = DiagnosticSessionUi(
    id = id,
    createdAt = createdAtEpochMillis,
    mode = mode.label(),
    target = if (targetHost.isBlank()) "本地日志" else buildString {
        if (targetScheme.isNotBlank()) append("$targetScheme://")
        append(targetHost)
        targetPort?.let { append(":$it") }
    },
    networkLines = networkSnapshot?.let { snapshot ->
        buildList {
            add("连接：${if (snapshot.connected) "已连接" else "未连接"} · ${snapshot.transport.name}")
            add("IPv4：${if (snapshot.hasIpv4) "有" else "无"} · IPv6：${if (snapshot.hasIpv6) "有" else "无"}")
            snapshot.validated?.let { add("系统验证联网：${if (it) "是" else "否"}") }
            snapshot.captivePortalDetected?.let { if (it) add("系统检测到认证门户") }
            if (snapshot.localAddresses.isNotEmpty()) add("本地地址：${snapshot.localAddresses.joinToString()}")
            if (snapshot.gatewayAddresses.isNotEmpty()) add("网关：${snapshot.gatewayAddresses.joinToString()}")
            if (snapshot.dnsServers.isNotEmpty()) add("DNS：${snapshot.dnsServers.joinToString()}")
            snapshot.attributes.forEach { (key, value) -> add("$key：$value") }
        }
    }.orEmpty(),
    probes = observations.map { observation ->
        ProbeResultUi(
            title = observation.type.name,
            role = if (observation.attributes[TARGET_ROLE_KEY] == REFERENCE_ROLE) "对照目标" else "主目标",
            target = observation.target,
            status = observation.status.name,
            durationMs = observation.durationMillis ?: 0,
            evidence = if (observation.type == ProbeType.DNS) formatDnsDetails(observation) else observation.evidence.take(8),
            error = observation.errorMessage,
        )
    },
    hypotheses = hypotheses.map { hypothesis ->
        HypothesisUi(
            title = hypothesis.title,
            priority = when {
                hypothesis.priority >= 100 -> "P1 高优先级"
                hypothesis.priority >= 75 -> "P2 中高优先级"
                hypothesis.priority >= 50 -> "P3 中优先级"
                else -> "P4 低优先级"
            } + " · " + when (hypothesis.evidenceStrength) {
                EvidenceStrength.HIGH -> "证据强"
                EvidenceStrength.MEDIUM -> "证据中等"
                EvidenceStrength.LOW -> "证据有限"
            },
            score = hypothesis.priority,
            matchedEvidence = hypothesis.matchedEvidence,
            conflictingEvidence = hypothesis.conflictingEvidence,
            actions = hypothesis.recommendedActions.ifEmpty {
                listOf(hypothesis.rationale.ifBlank { "补充证据后重新诊断" })
            },
        )
    },
    comparisonLines = retestComparison?.let { comparison ->
        buildList {
            if (comparison.networkChanged == true) add("⚠ 网络环境已变化：前后差异不能归因于修复操作。")
            add("总体：${comparison.outcome.name} · ${comparison.summary}")
            if (comparison.networkChanged == false) add("网络快照未发现明显变化；仍需结合重复采样判断。")
            comparison.probeComparisons.forEach { probe ->
                val detail = if (probe.probeType == ProbeType.TCP && probe.summary.contains("TCP 建连成功")) " · ${probe.summary}" else ""
                add("${probe.probeType.name} ${probe.target}：${probe.change.name}，${probe.beforeStatus ?: "无"} → ${probe.afterStatus ?: "无"}$detail")
            }
            if (comparison.resolvedHypothesisCodes.isNotEmpty()) add("已消失：${comparison.resolvedHypothesisCodes.joinToString()}")
            if (comparison.remainingHypothesisCodes.isNotEmpty()) add("仍存在：${comparison.remainingHypothesisCodes.joinToString()}")
            if (comparison.newHypothesisCodes.isNotEmpty()) add("新增：${comparison.newHypothesisCodes.joinToString()}")
        }
    }.orEmpty(),
    referenceTarget = metadata[REFERENCE_HOST_KEY],
    referenceSummary = referenceComparisonSummary(),
    tcpLines = observations.filter { it.type == ProbeType.TCP &&
        (it.attributes.containsKey("ipv4Connections") || it.attributes.containsKey("ipv6Connections"))
    }.map { observation ->
        val role = if (observation.attributes[TARGET_ROLE_KEY] == REFERENCE_ROLE) "对照目标" else "主目标"
        val attributes = observation.attributes
        val families = listOf("ipv4" to "IPv4", "ipv6" to "IPv6").map { (key, label) ->
            val count = attributes["${key}Connections"]
            val address = attributes["${key}Address"]
            if (count == null || count == "unavailable") "$label：未解析到地址"
            else "$label ${address.orEmpty()}：$count 次 TCP 建连成功"
        }
        val fallback = attributes["unresolvedConnections"]?.let { "；未解析地址回退：$it 次 TCP 建连成功" }.orEmpty()
        "$role · ${families.joinToString("；")}$fallback"
    },
)

fun DiagnosticSession.toHistoryUi(): DiagnosticHistoryItemUi {
    val succeeded = observations.count { it.status.name == "SUCCESS" }
    val abnormal = observations.size - succeeded
    return DiagnosticHistoryItemUi(
        id = id,
        createdAt = createdAtEpochMillis,
        mode = mode.label(),
        sessionMode = mode,
        target = targetHost.ifBlank { "本地日志" },
        summary = hypotheses.firstOrNull()?.title ?: title.ifBlank { "暂无诊断摘要" },
        probeSummary = if (observations.isEmpty()) {
            "纯日志诊断"
        } else {
            "${observations.size} 项探测 · $succeeded 项成功 · $abnormal 项异常/跳过"
        },
    )
}

private fun DiagnosticSessionMode.label(): String = when (this) {
    DiagnosticSessionMode.QUICK_CHECKUP -> "快速体检"
    DiagnosticSessionMode.LOG_ANALYSIS -> "日志诊断"
    DiagnosticSessionMode.COMBINED -> "组合诊断"
    DiagnosticSessionMode.RETEST -> "修复后复测"
}
