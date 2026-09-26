package com.netsage.app.ui

import com.netsage.app.diagnostic.session.ProbeObservation
import com.netsage.app.diagnostic.session.ProbeStatus

/** Formats stored DNS probe evidence; this function performs no lookup. */
fun formatDnsDetails(observation: ProbeObservation): List<String> {
    val attributes = observation.attributes
    val resolver = attributes["resolver"]?.trim().orEmpty()
    val addresses = attributes["addresses"].orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
    val ipv4 = addresses.filterNot { ':' in it }
    val ipv6 = addresses.filter { ':' in it }

    return buildList {
        add(when {
            resolver == "NumericAddress" -> "数字地址（未执行 DNS 查询）"
            observation.status == ProbeStatus.SUCCESS -> "DNS 解析成功"
            observation.status == ProbeStatus.FAILED -> "DNS 解析失败"
            observation.status == ProbeStatus.TIMEOUT -> "DNS 解析超时"
            observation.status == ProbeStatus.SKIPPED -> "DNS 未执行"
            else -> "DNS 状态：${observation.status.name}"
        })
        if (addresses.isEmpty()) add("解析地址：未记录")
        if (ipv4.isNotEmpty()) add("IPv4：${ipv4.joinToString("、")}")
        if (ipv6.isNotEmpty()) add("IPv6：${ipv6.joinToString("、")}")
        add("解析器：${resolver.ifEmpty { "未记录" }}")
        val rcode = attributes["dnsRcode"]?.trim().orEmpty()
        add("RCODE：${when {
            rcode.isNotEmpty() -> rcode
            attributes["dnsRcodeAvailable"] == "available" -> "可获取，未记录具体值"
            attributes["dnsRcodeAvailable"] == "unavailable" -> "不可获取"
            attributes["dnsRcodeAvailable"] == "not_applicable" -> "不适用"
            else -> "未记录"
        }}")
        observation.errorCode?.takeIf(String::isNotBlank)?.let { add("错误代码：$it") }
        observation.durationMillis?.let { add("耗时：$it ms") }
    }
}
