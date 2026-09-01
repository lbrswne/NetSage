package com.netsage.app.diagnostic.parser

/**
 * Deterministic, offline parser for common Chinese and English network diagnostics.
 * It extracts observations only; it does not assign causes or probabilities.
 */
class ObservationParser {
    fun parse(logText: String): List<Observation> {
        if (logText.isBlank()) return emptyList()

        val observations = linkedMapOf<String, Observation>()

        logText.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed
            val lineNumber = index + 1
            val excerpt = line.take(MAX_EXCERPT_LENGTH)

            fun add(
                kind: ObservationKind,
                value: String? = null,
                numericValue: Double? = null,
            ) {
                val key = listOf(kind.name, lineNumber, value.orEmpty(), numericValue ?: "").joinToString("|")
                observations.putIfAbsent(
                    key,
                    Observation(
                        kind = kind,
                        sourceExcerpt = excerpt,
                        lineNumber = lineNumber,
                        value = value,
                        numericValue = numericValue,
                    ),
                )
            }

            if (DNS_NXDOMAIN.containsMatchIn(line)) add(ObservationKind.DNS_NXDOMAIN)
            if (DNS_SERVFAIL.containsMatchIn(line)) add(ObservationKind.DNS_SERVFAIL)
            if (DNS_TIMEOUT.containsMatchIn(line)) add(ObservationKind.DNS_TIMEOUT)

            if (TCP_TIMEOUT.containsMatchIn(line)) add(ObservationKind.TCP_TIMEOUT)
            if (TCP_REFUSED.containsMatchIn(line)) add(ObservationKind.TCP_REFUSED)

            if (TLS_EXPIRED.containsMatchIn(line)) add(ObservationKind.TLS_EXPIRED)
            if (TLS_HOSTNAME_MISMATCH.containsMatchIn(line)) add(ObservationKind.TLS_HOSTNAME_MISMATCH)
            if (TLS_CHAIN_ERROR.containsMatchIn(line)) add(ObservationKind.TLS_CHAIN_ERROR)
            if (TLS_HANDSHAKE_FAILURE.containsMatchIn(line)) add(ObservationKind.TLS_HANDSHAKE_FAILURE)

            HTTP_STATUS.findAll(line).forEach { match ->
                val statusCode = match.groupValues[1].toIntOrNull() ?: return@forEach
                if (statusCode in 100..599) {
                    add(
                        kind = ObservationKind.HTTP_STATUS,
                        value = statusCode.toString(),
                        numericValue = statusCode.toDouble(),
                    )
                }
            }

            PACKET_LOSS_PATTERNS.forEach { pattern ->
                pattern.findAll(line).forEach matchLoop@ { match ->
                    val percentage = match.groupValues[1].toDoubleOrNull() ?: return@matchLoop
                    if (percentage in 0.0..100.0) {
                        add(
                            kind = ObservationKind.PACKET_LOSS,
                            value = "${percentage}%",
                            numericValue = percentage,
                        )
                    }
                }
            }

            if (PROXY_MENTION.containsMatchIn(line) && !PROXY_NEGATED.containsMatchIn(line)) {
                add(ObservationKind.PROXY_DETECTED)
            }
            if (CAPTIVE_PORTAL.containsMatchIn(line) && !CAPTIVE_PORTAL_NEGATED.containsMatchIn(line)) {
                add(ObservationKind.CAPTIVE_PORTAL)
            }
            if (DEFAULT_GATEWAY_MISSING.containsMatchIn(line)) {
                add(ObservationKind.DEFAULT_GATEWAY_MISSING)
            }
            if (DNS_CONFIGURATION_MISSING.containsMatchIn(line)) {
                add(ObservationKind.DNS_CONFIGURATION_MISSING)
            }
            if (INTERMITTENT_CONNECTIVITY.containsMatchIn(line)) {
                add(ObservationKind.INTERMITTENT_CONNECTIVITY)
            }
            if (IP_VERSION_MISMATCH.containsMatchIn(line)) {
                add(ObservationKind.IP_VERSION_MISMATCH)
            }

            val ipv4Addresses = IPV4.findAll(line)
                .map { it.value }
                .filter(::isValidIpv4)
                .distinct()
                .toList()
            ipv4Addresses.forEach { add(ObservationKind.IPV4_CLUE, value = it) }
            if (ipv4Addresses.isEmpty() && IPV4_MARKER.containsMatchIn(line)) {
                add(ObservationKind.IPV4_CLUE, value = "IPv4")
            }

            val ipv6Addresses = IPV6.findAll(line)
                .map { it.value }
                .filter { candidate -> candidate == "::1" || candidate.contains("::") || candidate.count { it == ':' } >= 4 }
                .distinct()
                .toList()
            ipv6Addresses.forEach { add(ObservationKind.IPV6_CLUE, value = it) }
            if (ipv6Addresses.isEmpty() && IPV6_MARKER.containsMatchIn(line)) {
                add(ObservationKind.IPV6_CLUE, value = "IPv6")
            }
        }

        return observations.values.toList()
    }

    private fun isValidIpv4(candidate: String): Boolean {
        val parts = candidate.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.toIntOrNull() in 0..255
        }
    }

    private companion object {
        const val MAX_EXCERPT_LENGTH = 240

        val DNS_NXDOMAIN = Regex(
            pattern = """\bNXDOMAIN\b|non[- ]existent domain|server can(?:not|'t) find|域名不存在|不存在的域名|找不到主机""",
            option = RegexOption.IGNORE_CASE,
        )
        val DNS_SERVFAIL = Regex(
            pattern = """\bSERVFAIL\b|DNS server failure|DNS 服务器(?:故障|失败)|DNS 服务失败|解析服务器失败""",
            option = RegexOption.IGNORE_CASE,
        )
        val DNS_TIMEOUT = Regex(
            pattern = """(?:DNS|name resolution|域名解析|解析请求|解析服务器).{0,40}(?:timed? out|timeout|超时)|(?:timed? out|timeout|超时).{0,40}(?:DNS|name resolution|域名解析|解析请求)|temporary failure in name resolution""",
            option = RegexOption.IGNORE_CASE,
        )
        val TCP_TIMEOUT = Regex(
            pattern = """(?:TCP|socket|connect(?:ion)?|连接|套接字).{0,35}(?:timed? out|timeout|超时)|(?:timed? out|timeout|超时).{0,20}(?:TCP|socket|connect(?:ion)?|连接|套接字)|ERR_CONNECTION_TIMED_OUT|ETIMEDOUT""",
            option = RegexOption.IGNORE_CASE,
        )
        val TCP_REFUSED = Regex(
            pattern = """connection refused|connect failed: refused|ECONNREFUSED|actively refused|连接被拒绝|拒绝连接|目标主机拒绝""",
            option = RegexOption.IGNORE_CASE,
        )
        val TLS_EXPIRED = Regex(
            pattern = """certificate (?:has )?expired|CERT_HAS_EXPIRED|expired certificate|证书(?:已)?过期|过期证书""",
            option = RegexOption.IGNORE_CASE,
        )
        val TLS_HOSTNAME_MISMATCH = Regex(
            pattern = """hostname mismatch|certificate.{0,40}not valid for|no subject alternative names? matching|ERR_CERT_COMMON_NAME_INVALID|主机名不匹配|域名不匹配|证书.{0,20}域名.{0,20}不匹配""",
            option = RegexOption.IGNORE_CASE,
        )
        val TLS_CHAIN_ERROR = Regex(
            pattern = """unable to get local issuer certificate|unknown ca|certificate chain|CertPathValidatorException|unable to find valid certification path|证书链(?:异常|无效|不完整|验证失败)?|无法验证证书链|不受信任的证书颁发机构|未知证书颁发机构""",
            option = RegexOption.IGNORE_CASE,
        )
        val TLS_HANDSHAKE_FAILURE = Regex(
            pattern = """(?:TLS|SSL).{0,25}(?:handshake failure|handshake failed|握手失败|握手异常)|SSLHandshakeException|handshake_failure""",
            option = RegexOption.IGNORE_CASE,
        )
        val HTTP_STATUS = Regex(
            pattern = """(?:HTTP(?:/\d(?:\.\d)?)?\s+|HTTP\s*status(?:\s*code)?\s*[:=]?\s*|status(?:\s*code)?\s*[:=]\s*|状态码\s*[:：]?\s*)([1-5]\d{2})\b""",
            option = RegexOption.IGNORE_CASE,
        )
        val PACKET_LOSS_PATTERNS = listOf(
            Regex("""(\d+(?:\.\d+)?)\s*%\s*(?:packet loss|loss)""", RegexOption.IGNORE_CASE),
            Regex("""(?:packet loss|丢包率?|丢包)\s*[:：=]?\s*(\d+(?:\.\d+)?)\s*%""", RegexOption.IGNORE_CASE),
        )
        val PROXY_MENTION = Regex(
            pattern = """\bproxy\b|HTTP_PROXY|HTTPS_PROXY|代理服务器|代理认证|经由代理|使用代理""",
            option = RegexOption.IGNORE_CASE,
        )
        val PROXY_NEGATED = Regex(
            pattern = """no proxy|proxy disabled|without proxy|未使用代理|未配置代理|禁用代理|无代理""",
            option = RegexOption.IGNORE_CASE,
        )
        val CAPTIVE_PORTAL = Regex(
            pattern = """captive portal|login portal|portal detected|认证门户|强制门户|需要网页(?:登录|认证)|校园网认证(?:页面|门户)|重定向到登录页""",
            option = RegexOption.IGNORE_CASE,
        )
        val CAPTIVE_PORTAL_NEGATED = Regex(
            pattern = """no captive portal|portal not detected|未检测到(?:强制)?门户|无需网页认证""",
            option = RegexOption.IGNORE_CASE,
        )
        val DEFAULT_GATEWAY_MISSING = Regex(
            pattern = """default gateway (?:is )?(?:missing|unavailable|not configured)|no default (?:gateway|route)|默认网关(?:缺失|不可用|未配置)|未找到默认(?:网关|路由)""",
            option = RegexOption.IGNORE_CASE,
        )
        val DNS_CONFIGURATION_MISSING = Regex(
            pattern = """DNS (?:configuration|server(?:s)?|resolver(?:s)?) (?:is )?(?:missing|empty|not configured)|no DNS (?:server|resolver)(?:s)? configured|DNS 配置(?:缺失|为空|未配置)|未配置 DNS(?: 服务器|解析器)?|未找到 DNS(?: 服务器|解析器)?""",
            option = RegexOption.IGNORE_CASE,
        )
        val INTERMITTENT_CONNECTIVITY = Regex(
            pattern = """\bintermittent(?:ly)?\b|\bsporadic(?:ally)?\b|\bflaky\b|时好时坏|间歇性(?:网络)?(?:故障|中断|超时|连接失败|不可用)?|偶发(?:断开|中断|掉线|连接失败|超时)?|偶尔(?:断开|中断|掉线|连接失败|超时)""",
            option = RegexOption.IGNORE_CASE,
        )
        val IP_VERSION_MISMATCH = Regex(
            pattern = """address family not supported|protocol family unavailable|IP version mismatch|IPv4.?IPv6.{0,20}(?:mismatch|不匹配)|IPv6.{0,20}(?:unreachable|failed|timeout|不可达|不可用|失败|超时|无路由).{0,40}IPv4.{0,20}(?:works?|正常|可用)|IPv4.{0,20}(?:works?|正常|可用).{0,40}IPv6.{0,20}(?:unreachable|failed|timeout|不可达|不可用|失败|超时|无路由)|地址族不支持|协议栈不匹配""",
            option = RegexOption.IGNORE_CASE,
        )
        val IPV4 = Regex("""(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])""")
        val IPV6 = Regex("""(?<![0-9A-Fa-f:])(?:[0-9A-Fa-f]{0,4}:){2,7}[0-9A-Fa-f]{0,4}(?![0-9A-Fa-f:])""")
        val IPV4_MARKER = Regex("""\bIPv4\b|\binet\s""", RegexOption.IGNORE_CASE)
        val IPV6_MARKER = Regex("""\bIPv6\b|\binet6\b""", RegexOption.IGNORE_CASE)
    }
}
