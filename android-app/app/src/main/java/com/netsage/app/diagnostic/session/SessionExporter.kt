package com.netsage.app.diagnostic.session

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Produces export content only. The UI layer decides whether and where to save or share it. */
class SessionExporter(
    private val gson: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun toJson(session: DiagnosticSession): String = gson.toJson(
        SessionExportEnvelope(
            schemaVersion = DIAGNOSTIC_SESSION_SCHEMA_VERSION,
            exportedAtEpochMillis = clock(),
            session = session,
        )
    )

    fun toMarkdown(session: DiagnosticSession): String = buildString {
        appendLine("# NetSage 诊断报告")
        appendLine()
        appendLine("- 会话 ID：`${inlineCode(session.id)}`")
        appendLine("- 模式：`${session.mode.name}`")
        appendLine("- 状态：`${session.status.name}`")
        appendLine("- 创建时间：${formatTimestamp(session.createdAtEpochMillis)}")
        appendLine("- 更新时间：${formatTimestamp(session.updatedAtEpochMillis)}")
        if (session.title.isNotBlank()) appendLine("- 标题：${markdownText(session.title)}")
        appendTarget(session)
        session.referenceComparisonSummary()?.let { summary ->
            appendLine("- 对照目标：`${inlineCode(session.metadata[REFERENCE_HOST_KEY].orEmpty())}`")
            appendLine("- 对照结论：${markdownText(summary)}")
        }
        if (session.observations.any { it.type == ProbeType.TCP && it.attributes.containsKey("tcpAttemptsPerAddress") }) {
            appendLine("- TCP 建连计数：每个解析到的地址族选择一个地址进行少量采样；不是丢包率或持续可用性。")
        }

        appendLine()
        appendLine("## 网络快照")
        appendNetworkSnapshot(session.networkSnapshot)

        appendLine()
        appendLine("## 检测时间线")
        appendObservations(session.observations)

        appendLine()
        appendLine("## 诊断假设")
        appendHypotheses(session.hypotheses)

        session.retestComparison?.let { comparison ->
            appendLine()
            appendLine("## 修复后复测")
            appendRetestComparison(comparison)
        }

        session.inputLog?.takeIf(String::isNotBlank)?.let { inputLog ->
            appendLine()
            appendLine("## 原始输入")
            appendLine(codeBlock(inputLog))
        }

        if (session.tags.isNotEmpty()) {
            appendLine()
            appendLine("## 标签")
            appendLine(session.tags.joinToString(" · ") { "`${inlineCode(it)}`" })
        }

        appendLine()
        appendLine("---")
        appendLine("此报告由 NetSage 在本机生成；导出内容不会自动上传到 NetSage 服务器。")
    }

    private fun StringBuilder.appendTarget(session: DiagnosticSession) {
        if (session.targetHost.isBlank()) return
        val scheme = session.targetScheme.takeIf(String::isNotBlank)?.let { "$it://" }.orEmpty()
        val port = session.targetPort?.let { ":$it" }.orEmpty()
        appendLine("- 检测目标：`${inlineCode("$scheme${session.targetHost}$port")}`")
    }

    private fun StringBuilder.appendNetworkSnapshot(snapshot: NetworkSnapshot?) {
        if (snapshot == null) {
            appendLine("未记录网络快照。")
            return
        }
        appendLine("- 连接状态：${if (snapshot.connected) "已连接" else "未连接"}")
        appendLine("- 连接类型：`${snapshot.transport.name}`")
        appendLine("- IPv4 / IPv6：${yesNo(snapshot.hasIpv4)} / ${yesNo(snapshot.hasIpv6)}")
        appendOptionalBoolean("计费网络", snapshot.metered)
        appendOptionalBoolean("系统验证联网", snapshot.validated)
        appendOptionalBoolean("检测到认证门户", snapshot.captivePortalDetected)
        appendStringList("本地地址", snapshot.localAddresses)
        appendStringList("网关", snapshot.gatewayAddresses)
        appendStringList("DNS", snapshot.dnsServers)
        snapshot.attributes.toSortedMap().forEach { (key, value) ->
            appendLine("- ${markdownText(key)}：`${inlineCode(value)}`")
        }
    }

    private fun StringBuilder.appendObservations(observations: List<ProbeObservation>) {
        if (observations.isEmpty()) {
            appendLine("没有检测记录。")
            return
        }
        observations
            .sortedWith(compareBy<ProbeObservation> { it.sequence }.thenBy { it.startedAtEpochMillis }.thenBy { it.id })
            .forEachIndexed { index, observation ->
                val target = observation.target.takeIf(String::isNotBlank)?.let { " · `${inlineCode(it)}`" }.orEmpty()
                val role = if (observation.attributes[TARGET_ROLE_KEY] == REFERENCE_ROLE) "对照目标" else "主目标"
                appendLine("### ${index + 1}. $role · ${observation.type.name} · ${observation.status.name}$target")
                if (observation.summary.isNotBlank()) appendLine(markdownText(observation.summary))
                observation.durationMillis?.let { appendLine("- 耗时：`${it} ms`") }
                observation.errorCode?.let { appendLine("- 错误代码：`${inlineCode(it)}`") }
                observation.errorMessage?.let { appendLine("- 错误信息：${markdownText(it)}") }
                observation.evidence.forEach { appendLine("- 证据：${markdownText(it)}") }
                observation.attributes.toSortedMap().forEach { (key, value) ->
                    appendLine("- ${markdownText(key)}：`${inlineCode(value)}`")
                }
                appendLine()
            }
    }

    private fun StringBuilder.appendHypotheses(hypotheses: List<DiagnosticHypothesis>) {
        if (hypotheses.isEmpty()) {
            appendLine("没有生成诊断假设。")
            return
        }
        hypotheses
            .sortedWith(compareByDescending<DiagnosticHypothesis> { it.priority }.thenBy { it.code }.thenBy { it.id })
            .forEachIndexed { index, hypothesis ->
                appendLine("### ${index + 1}. ${markdownText(hypothesis.title.ifBlank { hypothesis.code.ifBlank { "未命名原因" } })}")
                appendLine("- 诊断优先级：`${hypothesis.priority}`")
                appendLine("- 证据强度：`${hypothesis.evidenceStrength.name}`")
                if (hypothesis.category.isNotBlank()) appendLine("- 分类：`${inlineCode(hypothesis.category)}`")
                if (hypothesis.rationale.isNotBlank()) appendLine("- 判断依据：${markdownText(hypothesis.rationale)}")
                hypothesis.matchedEvidence.forEach { appendLine("- 匹配证据：${markdownText(it)}") }
                hypothesis.conflictingEvidence.forEach { appendLine("- 冲突证据：${markdownText(it)}") }
                if (hypothesis.recommendedActions.isNotEmpty()) {
                    appendLine("- 建议步骤：")
                    hypothesis.recommendedActions.forEachIndexed { actionIndex, action ->
                        appendLine("  ${actionIndex + 1}. ${markdownText(action)}")
                    }
                }
                appendLine()
            }
    }

    private fun StringBuilder.appendRetestComparison(comparison: RetestComparison) {
        appendLine("- 总体结果：`${comparison.outcome.name}`")
        appendLine("- 摘要：${markdownText(comparison.summary)}")
        comparison.networkChanged?.let { appendLine("- 网络环境发生变化：${yesNo(it)}") }
        comparison.probeComparisons.forEach { probe ->
            val target = probe.target.takeIf(String::isNotBlank)?.let { " `${inlineCode(it)}`" }.orEmpty()
            val duration = probe.durationDeltaMillis?.let { ", duration delta ${it} ms" }.orEmpty()
            appendLine("- `${probe.probeType.name}`$target：`${probe.change.name}`$duration")
        }
        appendStringList("已消失原因", comparison.resolvedHypothesisCodes)
        appendStringList("仍存在原因", comparison.remainingHypothesisCodes)
        appendStringList("新增原因", comparison.newHypothesisCodes)
    }

    private fun StringBuilder.appendStringList(label: String, values: List<String>) {
        if (values.isNotEmpty()) appendLine("- $label：${values.joinToString(", ") { "`${inlineCode(it)}`" }}")
    }

    private fun StringBuilder.appendOptionalBoolean(label: String, value: Boolean?) {
        value?.let { appendLine("- $label：${yesNo(it)}") }
    }

    private fun formatTimestamp(epochMillis: Long): String {
        if (epochMillis <= 0L) return "未记录"
        return runCatching {
            TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(epochMillis))
        }.getOrDefault(epochMillis.toString())
    }

    private fun yesNo(value: Boolean): String = if (value) "是" else "否"

    private fun markdownText(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\r\n", " ")
        .replace("\n", " ")
        .replace("|", "\\|")

    private fun inlineCode(value: String): String = value.replace("`", "\\`")

    private fun codeBlock(value: String): String {
        val longestRun = Regex("`+").findAll(value).maxOfOrNull { it.value.length } ?: 0
        val fence = "`".repeat(maxOf(3, longestRun + 1))
        return "$fence\n$value\n$fence"
    }

    private data class SessionExportEnvelope(
        val schemaVersion: Int,
        val exportedAtEpochMillis: Long,
        val session: DiagnosticSession,
    )

    companion object {
        private val TIMESTAMP_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
    }
}
