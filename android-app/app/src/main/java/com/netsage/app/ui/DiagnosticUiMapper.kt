package com.netsage.app.ui

import com.netsage.app.diagnostic.session.DiagnosticSession
import com.netsage.app.diagnostic.session.DiagnosticSessionMode
import com.netsage.app.diagnostic.session.EvidenceStrength
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
            target = observation.target,
            status = observation.status.name,
            durationMs = observation.durationMillis ?: 0,
            evidence = observation.evidence.take(8),
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
            action = hypothesis.recommendedActions.firstOrNull()
                ?: hypothesis.rationale.ifBlank { "补充证据后重新诊断" },
        )
    },
    comparisonLines = retestComparison?.let { comparison ->
        buildList {
            add("总体：${comparison.outcome.name} · ${comparison.summary}")
            comparison.networkChanged?.let { add("网络环境变化：${if (it) "是" else "否"}") }
            comparison.probeComparisons.forEach { probe ->
                add("${probe.probeType.name} ${probe.target}：${probe.change.name}，${probe.beforeStatus ?: "无"} → ${probe.afterStatus ?: "无"}")
            }
            if (comparison.resolvedHypothesisCodes.isNotEmpty()) add("已消失：${comparison.resolvedHypothesisCodes.joinToString()}")
            if (comparison.remainingHypothesisCodes.isNotEmpty()) add("仍存在：${comparison.remainingHypothesisCodes.joinToString()}")
            if (comparison.newHypothesisCodes.isNotEmpty()) add("新增：${comparison.newHypothesisCodes.joinToString()}")
        }
    }.orEmpty(),
)

fun DiagnosticSession.toHistoryUi(): DiagnosticHistoryItemUi {
    val succeeded = observations.count { it.status.name == "SUCCESS" }
    val abnormal = observations.size - succeeded
    return DiagnosticHistoryItemUi(
        id = id,
        createdAt = createdAtEpochMillis,
        mode = mode.label(),
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
