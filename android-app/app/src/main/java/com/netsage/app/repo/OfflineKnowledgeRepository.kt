package com.netsage.app.repo

import com.netsage.app.model.FaultCategory
import com.netsage.app.model.FaultScenario
import com.netsage.app.model.QuickRefCategory
import com.netsage.app.model.QuickRefItem
import com.netsage.app.model.SampleLogItem
import com.netsage.app.model.TroubleshootingChecklist

object OfflineKnowledgeRepository {
    val scenarios: List<FaultScenario> = listOf(
        FaultScenario(
            id = "dns_nxdomain",
            category = FaultCategory.DNS,
            title = "域名解析 NXDOMAIN",
            symptoms = "nslookup 返回 NXDOMAIN 或 server can't find",
            checks = listOf("检查域名是否拼写错误", "切换公共 DNS 复测", "排查本地 hosts 污染"),
            fixHints = listOf("优先恢复权威解析记录", "客户端临时更换 DNS 作为绕行")
        ),
        FaultScenario(
            id = "conn_no_route",
            category = FaultCategory.CONNECTION,
            title = "No route to host / 网关不可达",
            symptoms = "ping 提示 destination host unreachable 或 no route",
            checks = listOf("确认网关 IP 与网段", "traceroute 定位断点", "检查交换机/VLAN"),
            fixHints = listOf("补齐路由项", "修正网关与三层接口配置")
        ),
        FaultScenario(
            id = "tls_handshake",
            category = FaultCategory.TLS,
            title = "TLS 握手失败",
            symptoms = "handshake_failure、certificate unknown、protocol_version",
            checks = listOf("核查证书链完整性", "检查系统时间漂移", "确认 TLS 版本兼容"),
            fixHints = listOf("更新证书链", "统一启用 TLS1.2+", "排查中间盒劫持")
        ),
        FaultScenario(
            id = "http_502",
            category = FaultCategory.HTTP,
            title = "HTTP 502/504 网关错误",
            symptoms = "业务域名间歇返回 502/504",
            checks = listOf("区分网关层与上游应用层", "查看 upstream 超时配置", "比对健康检查结果"),
            fixHints = listOf("扩大超时与连接池", "修复后端实例故障")
        ),
        FaultScenario(
            id = "packet_loss_wireless",
            category = FaultCategory.PACKET_LOSS,
            title = "无线网络高丢包",
            symptoms = "ping 波动大且伴随 5%+ 丢包",
            checks = listOf("同地点多终端对比", "切换 2.4G/5G 信道", "排查干扰源"),
            fixHints = listOf("优化 AP 位置", "降低干扰并调整功率")
        )
    )

    val quickRefs: List<QuickRefItem> = listOf(
        QuickRefItem("NXDOMAIN", "DNS 查询不到该域名记录", "优先检查域名拼写、权威 DNS 记录与缓存", QuickRefCategory.DNS),
        QuickRefItem("ERR_CONNECTION_TIMED_OUT", "连接超时，常见于链路阻断或服务无响应", "先 ping/traceroute，再看目标端口连通性", QuickRefCategory.CONNECTION),
        QuickRefItem("TLS handshake failure", "TLS 协商参数不兼容或证书异常", "核对证书链、SNI、TLS 版本和系统时间", QuickRefCategory.TLS),
        QuickRefItem("HTTP 401", "未认证", "检查 token 是否过期、认证头是否缺失", QuickRefCategory.HTTP),
        QuickRefItem("HTTP 403", "已认证但无权限", "核对账号权限、ACL 或 WAF 策略", QuickRefCategory.HTTP),
        QuickRefItem("HTTP 404", "资源不存在", "确认路径、路由发布状态、反向代理转发规则", QuickRefCategory.HTTP),
        QuickRefItem("HTTP 429", "请求过多被限流", "降低频率，使用退避重试，检查限流配置", QuickRefCategory.HTTP),
        QuickRefItem("HTTP 500", "服务内部错误", "优先看应用日志与依赖健康状态", QuickRefCategory.HTTP),
        QuickRefItem("HTTP 502", "网关从上游收到无效响应", "检查 upstream 存活、协议匹配、超时", QuickRefCategory.HTTP),
        QuickRefItem("HTTP 504", "网关等待上游超时", "优化上游响应时延或调大网关超时", QuickRefCategory.HTTP)
    )

    val sampleLogs: List<SampleLogItem> = listOf(
        SampleLogItem(
            id = "sample_dns_01",
            title = "DNS 解析失败样例",
            category = FaultCategory.DNS,
            content = "nslookup failed: NXDOMAIN; server can't find api.netsage.local",
            hint = "适合演示 DNS 诊断"
        ),
        SampleLogItem(
            id = "sample_dns_02",
            title = "本地 DNS 超时样例",
            category = FaultCategory.DNS,
            content = "dig api.example.com @192.168.1.1 -> connection timed out; no servers could be reached",
            hint = "适合演示本地 DNS 无响应"
        ),
        SampleLogItem(
            id = "sample_dns_03",
            title = "hosts 污染样例",
            category = FaultCategory.DNS,
            content = "curl https://api.example.com failed; resolved to 127.0.0.1 from hosts entry",
            hint = "适合演示本地解析被错误覆盖"
        ),
        SampleLogItem(
            id = "sample_http_01",
            title = "HTTP 网关错误样例",
            category = FaultCategory.HTTP,
            content = "GET /api/v1/report -> 502 Bad Gateway; upstream connect error or timeout",
            hint = "适合演示网关/上游异常"
        ),
        SampleLogItem(
            id = "sample_http_02",
            title = "HTTP 上游超时样例",
            category = FaultCategory.HTTP,
            content = "POST /checkout -> 504 Gateway Timeout; upstream request timed out after 30s",
            hint = "适合演示长耗时请求超时"
        ),
        SampleLogItem(
            id = "sample_http_03",
            title = "HTTP 限流样例",
            category = FaultCategory.HTTP,
            content = "GET /api/feed -> 429 Too Many Requests; rate limit exceeded for client 10.0.0.25",
            hint = "适合演示限流类问题"
        ),
        SampleLogItem(
            id = "sample_tls_01",
            title = "TLS 握手失败样例",
            category = FaultCategory.TLS,
            content = "tls handshake failure: certificate unknown; protocol_version mismatch",
            hint = "适合演示证书链/TLS 版本问题"
        ),
        SampleLogItem(
            id = "sample_tls_02",
            title = "证书过期样例",
            category = FaultCategory.TLS,
            content = "x509: certificate has expired or is not yet valid; current time drift detected",
            hint = "适合演示证书有效期与系统时间异常"
        ),
        SampleLogItem(
            id = "sample_tls_03",
            title = "SNI 不匹配样例",
            category = FaultCategory.TLS,
            content = "SSL: no alternative certificate subject name matches target host name 'api.demo.com'",
            hint = "适合演示证书域名不匹配"
        ),
        SampleLogItem(
            id = "sample_conn_01",
            title = "网关不可达样例",
            category = FaultCategory.CONNECTION,
            content = "ping 10.10.0.1 -> Destination Host Unreachable; no route to host",
            hint = "适合演示路由/网关异常"
        ),
        SampleLogItem(
            id = "sample_conn_02",
            title = "TCP 连接超时样例",
            category = FaultCategory.CONNECTION,
            content = "dial tcp 172.16.8.20:443: i/o timeout; SYN retransmits exceeded",
            hint = "适合演示端口不通或链路阻断"
        ),
        SampleLogItem(
            id = "sample_conn_03",
            title = "连接被拒绝样例",
            category = FaultCategory.CONNECTION,
            content = "connect to 10.0.12.9:8080 failed: connection refused",
            hint = "适合演示服务未监听或被防火墙拒绝"
        ),
        SampleLogItem(
            id = "sample_loss_01",
            title = "无线高丢包样例",
            category = FaultCategory.PACKET_LOSS,
            content = "ping gateway avg=186ms loss=12%; wifi RSSI unstable on channel 149",
            hint = "适合演示无线干扰与高丢包"
        ),
        SampleLogItem(
            id = "sample_loss_02",
            title = "跨区域链路抖动样例",
            category = FaultCategory.PACKET_LOSS,
            content = "mtr to 8.8.8.8 shows 18% packet loss on hop 7 and latency spike to 320ms",
            hint = "适合演示中间链路波动"
        )
    )

    val checklists: List<TroubleshootingChecklist> = listOf(
        TroubleshootingChecklist(
            id = "check_dns_basic",
            title = "DNS 异常基础排障清单",
            category = FaultCategory.DNS,
            steps = listOf(
                "确认域名拼写与后缀是否正确",
                "切换到公共 DNS（如 223.5.5.5 / 8.8.8.8）复测",
                "检查 hosts 文件是否有错误映射",
                "对比多台设备解析结果是否一致"
            ),
            notes = "适用于 NXDOMAIN、server can't find、解析缓慢等问题。"
        ),
        TroubleshootingChecklist(
            id = "check_http_gateway",
            title = "HTTP 502/504 网关错误排障清单",
            category = FaultCategory.HTTP,
            steps = listOf(
                "确认错误发生在网关层还是上游服务层",
                "检查 upstream 服务是否存活",
                "核对网关超时、重试与连接池配置",
                "观察最近是否有发布或配置变更"
            ),
            notes = "适用于 Nginx/网关代理后的 502、504 问题。"
        ),
        TroubleshootingChecklist(
            id = "check_tls_handshake",
            title = "TLS 握手失败排障清单",
            category = FaultCategory.TLS,
            steps = listOf(
                "检查证书链是否完整",
                "核对客户端与服务端 TLS 版本",
                "确认系统时间无明显偏差",
                "排查是否存在中间盒或代理改写"
            ),
            notes = "适用于 certificate unknown、protocol_version、handshake failure。"
        )
    )
}
