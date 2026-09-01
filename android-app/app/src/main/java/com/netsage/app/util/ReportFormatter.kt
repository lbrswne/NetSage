package com.netsage.app.util

import com.netsage.app.model.CauseItem

fun buildReport(causes: List<CauseItem>): String {
    if (causes.isEmpty()) return "NetSage 报告：暂无诊断结果"

    return buildString {
        appendLine("NetSage 网络故障诊断报告")
        appendLine("====================")
        causes.forEachIndexed { index, c ->
            appendLine("${index + 1}. ${c.name}（规则证据强度 ${(c.confidence * 100).toInt()}/100）")
            appendLine("   建议：${c.fix}")
            if (c.evidence.isNotEmpty()) {
                appendLine("   证据：${c.evidence.joinToString("；")}")
            }
        }
    }
}
