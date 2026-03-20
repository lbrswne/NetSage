package com.netsage.app.repo

import com.netsage.app.model.CauseItem
import com.netsage.app.model.DiagnoseResponse

class DiagnoseRepository {
    suspend fun diagnose(logText: String): DiagnoseResponse {
        val text = logText.lowercase()
        val causes = mutableListOf<CauseItem>()

        fun hasAny(keys: List<String>): Boolean = keys.any { text.contains(it.lowercase()) }

        if (hasAny(listOf("dns", "nxdomain", "server can't find", "temporary failure in name resolution"))) {
            causes += CauseItem(
                name = "DNS 配置异常",
                confidence = 0.78,
                fix = "检查 DNS 服务器地址与连通性，尝试 223.5.5.5 / 8.8.8.8 对照测试",
                evidence = listOf("检测到 DNS 解析失败相关关键字")
            )
        }

        if (hasAny(listOf("dhcp", "no dhcp offers", "discover", "timeout waiting for dhcp"))) {
            causes += CauseItem(
                name = "DHCP 获取失败",
                confidence = 0.64,
                fix = "释放/续租 IP，检查 DHCP 服务与 VLAN 配置",
                evidence = listOf("检测到 DHCP 超时/无响应关键字")
            )
        }

        if (hasAny(listOf("default gateway", "destination host unreachable", "no route to host", "gateway"))) {
            causes += CauseItem(
                name = "默认网关不可达",
                confidence = 0.58,
                fix = "检查网关地址、三层路由与链路状态，必要时 traceroute",
                evidence = listOf("检测到网关不可达或路由异常关键字")
            )
        }

        if (hasAny(listOf("acl", "administratively prohibited", "access denied", "icmp admin prohibited"))) {
            causes += CauseItem(
                name = "ACL/安全策略拦截",
                confidence = 0.52,
                fix = "核对 ACL 命中规则、源/目的地址与端口，检查安全设备策略",
                evidence = listOf("检测到访问被策略阻断关键字")
            )
        }

        if (causes.isEmpty()) {
            causes += CauseItem(
                name = "日志特征不足",
                confidence = 0.31,
                fix = "补充 ping/nslookup/traceroute 与网卡状态信息后重试",
                evidence = listOf("未命中核心规则")
            )
        }

        return DiagnoseResponse(top_causes = causes.sortedByDescending { it.confidence }.take(3))
    }
}
