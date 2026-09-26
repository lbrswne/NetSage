package com.netsage.app.ui

import com.netsage.app.diagnostic.session.ProbeObservation
import com.netsage.app.diagnostic.session.ProbeStatus
import com.netsage.app.diagnostic.session.ProbeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DnsDetailsFormatterTest {
    @Test
    fun `groups stored addresses by IP family without implying service reachability`() {
        val lines = formatDnsDetails(ProbeObservation(
            type = ProbeType.DNS,
            status = ProbeStatus.SUCCESS,
            attributes = mapOf(
                "addresses" to "2001:db8::1, 192.0.2.1, 192.0.2.2, 2001:db8::2",
                "resolver" to "DnsResolver",
                "dnsRcodeAvailable" to "available",
            ),
            durationMillis = 34,
        ))

        assertEquals(listOf(
            "DNS 解析成功",
            "IPv4：192.0.2.1、192.0.2.2",
            "IPv6：2001:db8::1、2001:db8::2",
            "解析器：DnsResolver",
            "RCODE：可获取，未记录具体值",
            "耗时：34 ms",
        ), lines)
        assertFalse(lines.any { it.contains("服务可达") || it.contains("网络可达") })
    }

    @Test
    fun `shows actual RCODE and error on failed lookup`() {
        val lines = formatDnsDetails(ProbeObservation(
            type = ProbeType.DNS,
            status = ProbeStatus.FAILED,
            attributes = mapOf(
                "dnsRcodeAvailable" to "available",
                "dnsRcode" to "3",
                "resolver" to "DnsResolver",
            ),
            errorCode = "DNS_NXDOMAIN",
            durationMillis = 18,
        ))

        assertEquals(listOf(
            "DNS 解析失败",
            "解析地址：未记录",
            "解析器：DnsResolver",
            "RCODE：3",
            "错误代码：DNS_NXDOMAIN",
            "耗时：18 ms",
        ), lines)
    }

    @Test
    fun `fallback failure does not invent an RCODE`() {
        val lines = formatDnsDetails(ProbeObservation(
            type = ProbeType.DNS,
            status = ProbeStatus.FAILED,
            attributes = mapOf(
                "resolver" to "InetAddressFallback",
                "dnsRcodeAvailable" to "unavailable",
            ),
            errorCode = "DNS_LOOKUP_FAILED",
        ))

        assertEquals(listOf(
            "DNS 解析失败",
            "解析地址：未记录",
            "解析器：InetAddressFallback",
            "RCODE：不可获取",
            "错误代码：DNS_LOOKUP_FAILED",
        ), lines)
    }

    @Test
    fun `numeric target is shown as an address without claiming a DNS query`() {
        val lines = formatDnsDetails(ProbeObservation(
            type = ProbeType.DNS,
            status = ProbeStatus.SUCCESS,
            attributes = mapOf(
                "addresses" to "192.0.2.1",
                "resolver" to "NumericAddress",
                "dnsRcodeAvailable" to "not_applicable",
            ),
            durationMillis = 0,
        ))

        assertEquals(listOf(
            "数字地址（未执行 DNS 查询）",
            "IPv4：192.0.2.1",
            "解析器：NumericAddress",
            "RCODE：不适用",
            "耗时：0 ms",
        ), lines)
    }

    @Test
    fun `missing evidence stays explicitly unrecorded`() {
        val lines = formatDnsDetails(ProbeObservation(type = ProbeType.DNS, status = ProbeStatus.SKIPPED))

        assertEquals(listOf(
            "DNS 未执行",
            "解析地址：未记录",
            "解析器：未记录",
            "RCODE：未记录",
        ), lines)
    }
}
