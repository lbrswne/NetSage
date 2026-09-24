package com.netsage.app.diagnostic.probe

enum class NetworkTransport {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    BLUETOOTH,
    WIFI_AWARE,
    LOWPAN,
    USB,
    OTHER,
}

enum class ProbeKind {
    DNS,
    TCP,
    TLS,
    HTTP,
}

enum class ProbeStatus {
    SUCCESS,
    FAILURE,
    TIMEOUT,
    CANCELLED,
    SKIPPED,
}

enum class ProbeScheme(val value: String) {
    HTTP("http"),
    HTTPS("https"),
}

data class NetworkSnapshot(
    val capturedAtEpochMillis: Long,
    val connected: Boolean,
    val internetCapable: Boolean,
    val validated: Boolean,
    val captivePortal: Boolean,
    val metered: Boolean,
    val transports: Set<NetworkTransport>,
    val interfaceName: String?,
    val ipAddresses: List<String>,
    val gateways: List<String>,
    val dnsServers: List<String>,
    val mtu: Int?,
    val privateDnsActive: Boolean,
    val privateDnsServerName: String?,
    val proxyHost: String?,
    val proxyPort: Int?,
    val error: String? = null,
)

data class ProbeTarget(
    val host: String,
    val port: Int? = null,
    val scheme: ProbeScheme = ProbeScheme.HTTPS,
    val path: String = "/",
    val followRedirects: Boolean = true,
    val maxRedirects: Int = 5,
) {
    val effectivePort: Int
        get() = port ?: if (scheme == ProbeScheme.HTTPS) 443 else 80

    fun normalizedHost(): String = host.trim().removeSurrounding("[", "]")

    fun normalizedPath(): String {
        val trimmed = path.trim()
        return when {
            trimmed.isEmpty() -> "/"
            trimmed.startsWith("/") -> trimmed
            else -> "/$trimmed"
        }
    }
}

data class ProbeTimeouts(
    val dnsMillis: Long = 5_000,
    val tcpMillis: Long = 5_000,
    val tlsMillis: Long = 7_000,
    val httpMillis: Long = 10_000,
)

data class ProbeRequest(
    val target: ProbeTarget,
    val timeouts: ProbeTimeouts = ProbeTimeouts(),
    val tcpAttempts: Int = 1,
)

data class ProbeError(
    val code: String,
    val message: String,
    val causeType: String? = null,
)

data class ProbeObservation(
    val kind: ProbeKind,
    val status: ProbeStatus,
    val target: String,
    val startedAtEpochMillis: Long,
    val durationMillis: Long,
    val evidence: Map<String, String> = emptyMap(),
    val rawEvidence: String? = null,
    val error: ProbeError? = null,
)

data class ProbeRunResult(
    val snapshot: NetworkSnapshot,
    val observations: List<ProbeObservation>,
    val startedAtEpochMillis: Long,
    val durationMillis: Long,
)
