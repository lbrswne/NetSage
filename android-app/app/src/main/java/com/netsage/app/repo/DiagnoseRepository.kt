package com.netsage.app.repo

import com.netsage.app.model.CauseItem
import com.netsage.app.model.DiagnoseResponse

class DiagnoseRepository {
    suspend fun diagnose(logText: String): DiagnoseResponse {
        val text = logText.lowercase()
        val causes = mutableListOf<CauseItem>()

        fun hasAny(keys: List<String>): Boolean = keys.any { text.contains(it.lowercase()) }

        fun addCause(
            name: String,
            confidence: Double,
            fix: String,
            evidence: List<String>
        ) {
            causes += CauseItem(
                name = name,
                confidence = confidence,
                fix = fix,
                evidence = evidence.distinct()
            )
        }

        if (hasAny(listOf("dns", "nxdomain", "server can't find", "temporary failure in name resolution", "name resolution"))) {
            val evidence = mutableListOf("检测到 DNS 解析失败相关关键字")
            if (hasAny(listOf("nxdomain", "server can't find"))) evidence += "出现 NXDOMAIN / server can't find 等解析失败表现"
            if (hasAny(listOf("temporary failure in name resolution"))) evidence += "出现临时解析失败，可能与本地 DNS 或上游递归链路有关"
            addCause(
                name = "DNS 配置异常",
                confidence = if (hasAny(listOf("nxdomain", "server can't find"))) 0.82 else 0.78,
                fix = "检查 DNS 服务器地址与连通性，尝试 223.5.5.5 / 8.8.8.8 对照测试，并核对 hosts 与权威解析记录",
                evidence = evidence
            )
        }

        if (hasAny(listOf("dhcp", "no dhcp offers", "timeout waiting for dhcp", "dhcp discover", "dhcp offer"))) {
            val evidence = mutableListOf("检测到 DHCP 超时/无响应关键字")
            if (hasAny(listOf("no dhcp offers", "timeout waiting for dhcp"))) evidence += "客户端未成功收到 DHCP Offer"
            addCause(
                name = "DHCP 获取失败",
                confidence = 0.68,
                fix = "释放/续租 IP，检查 DHCP 服务、地址池、VLAN 与接入端口配置",
                evidence = evidence
            )
        }

        if (hasAny(listOf("default gateway", "destination host unreachable", "no route to host", "network is unreachable", "gateway unreachable"))) {
            val evidence = mutableListOf("检测到网关不可达或路由异常关键字")
            if (hasAny(listOf("no route to host", "network is unreachable"))) evidence += "更偏向三层路由缺失或目标网段不可达"
            if (hasAny(listOf("destination host unreachable", "default gateway"))) evidence += "更偏向默认网关或本地出口链路异常"
            addCause(
                name = "默认网关不可达",
                confidence = 0.66,
                fix = "检查网关地址、ARP、三层路由与链路状态，必要时结合 traceroute 定位断点",
                evidence = evidence
            )
        }

        if (hasAny(listOf("acl", "administratively prohibited", "access denied", "icmp admin prohibited", "forbidden by rule"))) {
            addCause(
                name = "ACL/安全策略拦截",
                confidence = 0.57,
                fix = "核对 ACL 命中规则、源/目的地址与端口范围，并检查防火墙/安全组策略",
                evidence = listOf("检测到访问被策略阻断关键字")
            )
        }

        if (hasAny(listOf("tls", "handshake failure", "certificate unknown", "protocol_version", "ssl handshake", "x509"))) {
            val evidence = mutableListOf("检测到 TLS/证书异常关键字")
            if (hasAny(listOf("certificate unknown", "x509"))) evidence += "更偏向证书链、信任链或证书域名不匹配"
            if (hasAny(listOf("protocol_version", "handshake failure", "ssl handshake"))) evidence += "更偏向 TLS 版本/握手参数不兼容"
            addCause(
                name = "TLS 握手或证书异常",
                confidence = 0.8,
                fix = "检查证书链完整性、证书域名、系统时间及 TLS 版本兼容性，必要时排查中间代理改写",
                evidence = evidence
            )
        }

        if (hasAny(listOf("502", "504", "bad gateway", "upstream", "gateway timeout", "upstream connect error"))) {
            val evidence = mutableListOf("检测到网关/上游异常关键字")
            if (hasAny(listOf("502", "bad gateway", "upstream connect error"))) evidence += "更偏向上游服务无效响应或连接失败"
            if (hasAny(listOf("504", "gateway timeout"))) evidence += "更偏向上游处理过慢或网关等待超时"
            addCause(
                name = "HTTP 网关层异常",
                confidence = 0.79,
                fix = "区分网关层与上游服务层，检查 upstream 存活、超时、重试与连接池配置",
                evidence = evidence
            )
        }

        if (hasAny(listOf("connection timed out", "err_connection_timed_out", "timeout", "i/o timeout"))) {
            val evidence = mutableListOf("检测到超时相关关键字")
            if (hasAny(listOf("i/o timeout", "connection timed out"))) evidence += "更偏向链路不通、目标端口无响应或服务处理过慢"
            addCause(
                name = "链路超时或目标服务无响应",
                confidence = 0.62,
                fix = "先确认目标地址与端口连通性，再检查链路质量、服务监听状态与上游响应耗时",
                evidence = evidence
            )
        }

        if (hasAny(listOf("packet loss", "丢包", "request timeout", "icmp_seq", "100% packet loss"))) {
            val evidence = mutableListOf("检测到丢包或探测超时关键字")
            if (hasAny(listOf("100% packet loss", "request timeout"))) evidence += "丢包表现较重，需优先排查链路中断或严重干扰"
            addCause(
                name = "链路丢包或无线干扰",
                confidence = 0.61,
                fix = "对比多终端与多位置结果，检查物理链路、交换机端口错误、无线信道与干扰源",
                evidence = evidence
            )
        }

        if (causes.isEmpty()) {
            addCause(
                name = "日志特征不足",
                confidence = 0.31,
                fix = "补充 ping/nslookup/traceroute、错误码、发生时间点与网络环境信息后重试",
                evidence = listOf("未命中核心规则")
            )
        }

        return DiagnoseResponse(top_causes = causes.sortedByDescending { it.confidence }.take(3))
    }
}
