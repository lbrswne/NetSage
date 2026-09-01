package com.netsage.app.diagnostic.corpus

import com.netsage.app.diagnostic.parser.ObservationParser
import com.netsage.app.diagnostic.rules.LocalRuleEngine
import com.netsage.app.diagnostic.rules.RuleDefinitionLoader
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DiagnosticCorpusRegressionTest(private val fixture: CorpusFixture) {
    @Test
    fun `formal rules keep corpus expectation in top three`() {
        val hypotheses = engine.evaluate(parser.parse(fixture.logText))
        val actualRuleIds = hypotheses.map { it.ruleId }

        if (fixture.expectedRuleId == null) {
            assertTrue(
                "${fixture.name}: expected no diagnosis, but got $actualRuleIds",
                actualRuleIds.isEmpty(),
            )
        } else {
            assertTrue(
                "${fixture.name}: expected ${fixture.expectedRuleId} in Top3, but got $actualRuleIds",
                fixture.expectedRuleId in actualRuleIds,
            )
        }
    }

    data class CorpusFixture(
        val name: String,
        val logText: String,
        val expectedRuleId: String?,
    ) {
        override fun toString(): String = name
    }

    companion object {
        private val parser = ObservationParser()
        private val engine = loadFormalRuleEngine()

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun corpus(): List<Array<Any?>> = fixtures.map { arrayOf(it) }

        private val fixtures = listOf(
            // DNS NXDOMAIN (5)
            case("dns-nxdomain-bind", "named: query failed: NXDOMAIN for api.example.test", "dns.nxdomain"),
            case("dns-nxdomain-nslookup", "Server can't find missing.example.test: NXDOMAIN", "dns.nxdomain"),
            case("dns-nxdomain-browser", "Lookup failed: non-existent domain", "dns.nxdomain"),
            case("dns-nxdomain-chinese", "DNS 返回域名不存在，请检查记录", "dns.nxdomain"),
            case("dns-nxdomain-host-not-found", "解析失败：找不到主机 service.example.test", "dns.nxdomain"),

            // DNS SERVFAIL (5)
            case("dns-servfail-dig", "status: SERVFAIL, id: 2031", "dns.servfail"),
            case("dns-servfail-server", "DNS server failure while resolving api.example.test", "dns.servfail"),
            case("dns-servfail-chinese-server", "DNS 服务器失败，无法完成查询", "dns.servfail"),
            case("dns-servfail-chinese-service", "DNS 服务失败，请稍后重试", "dns.servfail"),
            case("dns-servfail-resolver", "解析服务器失败，未返回 A 记录", "dns.servfail"),

            // DNS timeout (5)
            case("dns-timeout-query", "DNS query timeout after 3000 ms", "dns.timeout"),
            case("dns-timeout-resolution", "name resolution timed out", "dns.timeout"),
            case("dns-timeout-chinese", "域名解析超时 5 秒", "dns.timeout"),
            case("dns-timeout-request", "解析请求 timeout while querying AAAA", "dns.timeout"),
            case("dns-timeout-temporary", "temporary failure in name resolution", "dns.timeout"),

            // TCP timeout (5)
            case("tcp-timeout-connect", "TCP connection timed out after 10 seconds", "tcp.connection_timeout"),
            case("tcp-timeout-socket", "socket timeout connecting to 203.0.113.8:443", "tcp.connection_timeout"),
            case("tcp-timeout-short", "connect timeout for port 8443", "tcp.connection_timeout"),
            case("tcp-timeout-chinese", "连接超时，目标端口 443 无响应", "tcp.connection_timeout"),
            case("tcp-timeout-browser", "net::ERR_CONNECTION_TIMED_OUT", "tcp.connection_timeout"),

            // TCP refused (5)
            case("tcp-refused-posix", "connect: connection refused", "tcp.connection_refused"),
            case("tcp-refused-code", "socket failed with ECONNREFUSED", "tcp.connection_refused"),
            case("tcp-refused-windows", "The target machine actively refused the connection", "tcp.connection_refused"),
            case("tcp-refused-chinese", "连接被拒绝：服务可能未监听", "tcp.connection_refused"),
            case("tcp-refused-host", "目标主机拒绝了端口 8080 的连接", "tcp.connection_refused"),

            // TLS expired certificate (5)
            case("tls-expired-openssl", "verify error: certificate has expired", "tls.certificate_expired"),
            case("tls-expired-node", "Error: CERT_HAS_EXPIRED", "tls.certificate_expired"),
            case("tls-expired-client", "TLS rejected an expired certificate", "tls.certificate_expired"),
            case("tls-expired-chinese", "TLS 证书已过期", "tls.certificate_expired"),
            case("tls-expired-chinese-short", "检测到过期证书", "tls.certificate_expired"),

            // TLS hostname mismatch (5)
            case("tls-hostname-mismatch", "TLS hostname mismatch for api.example.test", "tls.hostname_mismatch"),
            case("tls-hostname-not-valid", "certificate is not valid for requested host", "tls.hostname_mismatch"),
            case("tls-hostname-san", "no subject alternative name matching api.example.test", "tls.hostname_mismatch"),
            case("tls-hostname-browser", "net::ERR_CERT_COMMON_NAME_INVALID", "tls.hostname_mismatch"),
            case("tls-hostname-chinese", "证书域名不匹配，无法建立安全连接", "tls.hostname_mismatch"),

            // TLS chain validation (5)
            case("tls-chain-issuer", "unable to get local issuer certificate", "tls.certificate_chain"),
            case("tls-chain-unknown-ca", "TLS alert: unknown ca", "tls.certificate_chain"),
            case("tls-chain-android", "CertPathValidatorException: trust anchor not found", "tls.certificate_chain"),
            case("tls-chain-java", "unable to find valid certification path to requested target", "tls.certificate_chain"),
            case("tls-chain-chinese", "证书链验证失败：缺少中间证书", "tls.certificate_chain"),

            // TLS generic handshake (5)
            case("tls-handshake-failed", "TLS handshake failed before certificate exchange", "tls.handshake_failure"),
            case("ssl-handshake-failure", "SSL handshake failure", "tls.handshake_failure"),
            case("tls-handshake-android", "javax.net.ssl.SSLHandshakeException", "tls.handshake_failure"),
            case("tls-handshake-alert", "received alert handshake_failure", "tls.handshake_failure"),
            case("tls-handshake-chinese", "TLS 握手失败，连接已关闭", "tls.handshake_failure"),

            // HTTP client errors (5)
            case("http-401", "HTTP/1.1 401 Unauthorized", "http.client_error"),
            case("http-403", "HTTP status code: 403", "http.client_error"),
            case("http-404", "status: 404 route not found", "http.client_error"),
            case("http-429", "HTTP 429 Too Many Requests", "http.client_error"),
            case("http-499", "状态码：499 client closed request", "http.client_error"),

            // HTTP server errors (5)
            case("http-500", "HTTP/1.1 500 Internal Server Error", "http.server_error"),
            case("http-502", "HTTP status: 502 Bad Gateway", "http.server_error"),
            case("http-503", "status code = 503", "http.server_error"),
            case("http-504", "HTTP 504 Gateway Timeout", "http.server_error"),
            case("http-599", "状态码：599 upstream unavailable", "http.server_error"),

            // Packet loss (5)
            case("packet-loss-ping", "5% packet loss", "network.packet_loss"),
            case("packet-loss-summary", "packet loss: 18%", "network.packet_loss"),
            case("packet-loss-decimal", "0.5% loss over 200 packets", "network.packet_loss"),
            case("packet-loss-chinese-rate", "丢包率：12%", "network.packet_loss"),
            case("packet-loss-chinese", "丢包 37.5%", "network.packet_loss"),

            // Proxy (5)
            case("proxy-configured", "proxy configured at 192.0.2.10:8080", "network.proxy_detected"),
            case("proxy-http-env", "HTTP_PROXY=http://proxy.example.test:3128", "network.proxy_detected"),
            case("proxy-https-env", "HTTPS_PROXY is active", "network.proxy_detected"),
            case("proxy-chinese-server", "代理服务器要求重新认证", "network.proxy_detected"),
            case("proxy-chinese-route", "当前连接经由代理访问外网", "network.proxy_detected"),

            // Captive portal (5)
            case("portal-detected", "captive portal detected on current Wi-Fi", "network.captive_portal"),
            case("portal-login", "login portal intercepted the request", "network.captive_portal"),
            case("portal-chinese-auth", "检测到认证门户，需要登录", "network.captive_portal"),
            case("portal-chinese-web", "当前网络需要网页登录", "network.captive_portal"),
            case("portal-chinese-redirect", "请求被重定向到登录页", "network.captive_portal"),

            // IPv4/IPv6 mismatch (5)
            case("ip-family-unsupported", "socket error: address family not supported", "network.ip_version_mismatch"),
            case("ip-family-unavailable", "protocol family unavailable", "network.ip_version_mismatch"),
            case("ip-version-mismatch", "IP version mismatch detected", "network.ip_version_mismatch"),
            case("ip-v6-fails-v4-works", "IPv6 unreachable while IPv4 works", "network.ip_version_mismatch"),
            case("ip-v4-ok-v6-fails", "IPv4 正常，但 IPv6 不可达", "network.ip_version_mismatch"),

            // Blank or normal input (10)
            case("normal-empty", "", null),
            case("normal-whitespace", " \n\t \n", null),
            case("normal-http-200", "HTTP/1.1 200 OK", null),
            case("normal-http-302", "HTTP status: 302 Found", null),
            case("normal-dns-success", "DNS query completed successfully with A record", null),
            case("normal-tcp-connected", "TCP connection established to port 443", null),
            case("normal-tls-complete", "TLS handshake completed successfully", null),
            case("normal-no-proxy", "no proxy configured for this network", null),
            case("normal-no-portal", "no captive portal detected", null),
            case("normal-address-clues", "IPv4 192.0.2.8 and IPv6 2001:db8::8 are both reachable", null),
        )

        private fun case(name: String, logText: String, expectedRuleId: String?) =
            CorpusFixture(name = name, logText = logText, expectedRuleId = expectedRuleId)

        private fun loadFormalRuleEngine(): LocalRuleEngine {
            val asset = listOf(
                File("src/main/assets/${RuleDefinitionLoader.DEFAULT_ASSET_NAME}"),
                File("app/src/main/assets/${RuleDefinitionLoader.DEFAULT_ASSET_NAME}"),
            ).firstOrNull(File::isFile)
                ?: error(
                    "Missing ${RuleDefinitionLoader.DEFAULT_ASSET_NAME} below ${File(".").absolutePath}",
                )
            return asset.inputStream().use(LocalRuleEngine::fromInputStream)
        }
    }
}
