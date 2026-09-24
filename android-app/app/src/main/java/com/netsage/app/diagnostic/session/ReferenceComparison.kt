package com.netsage.app.diagnostic.session

const val REFERENCE_HOST_KEY = "referenceHost"
const val REFERENCE_NETWORK_CHANGED_KEY = "referenceNetworkChanged"
const val TARGET_ROLE_KEY = "targetRole"
const val REFERENCE_ROLE = "reference"

/** A comparison of two HTTP checks, not a claim about the root cause. */
fun DiagnosticSession.referenceComparisonSummary(): String? {
    if (metadata[REFERENCE_HOST_KEY].isNullOrBlank()) return null
    if (metadata[REFERENCE_NETWORK_CHANGED_KEY] == "true") {
        return "两次检测期间网络环境发生变化，暂不能直接比较；请在同一网络下重新检测。"
    }
    val primary = observations.firstOrNull {
        it.type == ProbeType.HTTP && it.attributes[TARGET_ROLE_KEY] != REFERENCE_ROLE
    }
    val reference = observations.firstOrNull {
        it.type == ProbeType.HTTP && it.attributes[TARGET_ROLE_KEY] == REFERENCE_ROLE
    }
    if (primary == null || reference == null) return "对照检测未完成，暂不能比较。"

    val primaryOk = primary.status == ProbeStatus.SUCCESS
    val referenceOk = reference.status == ProbeStatus.SUCCESS
    return when {
        primaryOk && referenceOk -> "主目标与对照目标本次均通过 HTTP 检测。"
        !primaryOk && referenceOk -> "对照目标通过检测，主目标未通过；优先检查主目标服务及其访问路径。"
        primaryOk && !referenceOk -> "主目标通过检测，对照目标未通过；不能将对照目标异常归因于主目标。"
        else -> "两个目标本次均未通过 HTTP 检测；需结合网络快照与逐项证据继续排查。"
    }
}
