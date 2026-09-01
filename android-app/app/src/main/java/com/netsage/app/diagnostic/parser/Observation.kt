package com.netsage.app.diagnostic.parser

/** Structured facts extracted locally from pasted or imported diagnostic text. */
enum class ObservationKind {
    DNS_NXDOMAIN,
    DNS_SERVFAIL,
    DNS_TIMEOUT,
    TCP_TIMEOUT,
    TCP_REFUSED,
    TLS_EXPIRED,
    TLS_HOSTNAME_MISMATCH,
    TLS_CHAIN_ERROR,
    TLS_HANDSHAKE_FAILURE,
    HTTP_STATUS,
    PACKET_LOSS,
    PROXY_DETECTED,
    CAPTIVE_PORTAL,
    IPV4_CLUE,
    IPV6_CLUE,
    IP_VERSION_MISMATCH,
}

data class Observation(
    val kind: ObservationKind,
    val sourceExcerpt: String,
    val lineNumber: Int,
    val value: String? = null,
    val numericValue: Double? = null,
)
