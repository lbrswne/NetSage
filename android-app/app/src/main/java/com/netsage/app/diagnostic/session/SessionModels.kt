package com.netsage.app.diagnostic.session

import java.util.UUID

const val DIAGNOSTIC_SESSION_SCHEMA_VERSION = 1

enum class DiagnosticSessionMode {
    QUICK_CHECKUP,
    LOG_ANALYSIS,
    COMBINED,
    RETEST,
}

enum class DiagnosticSessionStatus {
    DRAFT,
    RUNNING,
    COMPLETED,
    PARTIAL,
    CANCELLED,
    FAILED,
}

enum class NetworkTransport {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    BLUETOOTH,
    OTHER,
    UNKNOWN,
}

enum class ProbeType {
    NETWORK_SNAPSHOT,
    DNS,
    TCP,
    TLS,
    HTTP,
    OTHER,
}

enum class ProbeStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    WARNING,
    TIMEOUT,
    FAILED,
    SKIPPED,
    CANCELLED,
}

enum class EvidenceStrength {
    LOW,
    MEDIUM,
    HIGH,
}

enum class RetestOutcome {
    IMPROVED,
    UNCHANGED,
    REGRESSED,
    MIXED,
    INCONCLUSIVE,
}

enum class ProbeChange {
    IMPROVED,
    UNCHANGED,
    REGRESSED,
    ADDED,
    REMOVED,
    INCONCLUSIVE,
}

/**
 * Android-free snapshot DTO. The network probe layer can map ConnectivityManager and
 * LinkProperties values into this type without leaking framework objects into storage.
 */
data class NetworkSnapshot(
    val capturedAtEpochMillis: Long = 0L,
    val connected: Boolean = false,
    val transport: NetworkTransport = NetworkTransport.UNKNOWN,
    val localAddresses: List<String> = emptyList(),
    val gatewayAddresses: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val hasIpv4: Boolean = false,
    val hasIpv6: Boolean = false,
    val metered: Boolean? = null,
    val validated: Boolean? = null,
    val captivePortalDetected: Boolean? = null,
    val attributes: Map<String, String> = emptyMap(),
)

/** A serializable result emitted by one local probe. */
data class ProbeObservation(
    val id: String = UUID.randomUUID().toString(),
    val sequence: Int = 0,
    val type: ProbeType = ProbeType.OTHER,
    val status: ProbeStatus = ProbeStatus.PENDING,
    val target: String = "",
    val startedAtEpochMillis: Long = 0L,
    val finishedAtEpochMillis: Long? = null,
    val durationMillis: Long? = null,
    val summary: String = "",
    val evidence: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val errorCode: String? = null,
    val errorMessage: String? = null,
)

/**
 * An explainable local diagnosis. Priority is a deterministic sorting score, not a
 * probability or measured accuracy value.
 */
data class DiagnosticHypothesis(
    val id: String = UUID.randomUUID().toString(),
    val code: String = "",
    val title: String = "",
    val category: String = "",
    val priority: Int = 0,
    val evidenceStrength: EvidenceStrength = EvidenceStrength.LOW,
    val matchedEvidence: List<String> = emptyList(),
    val conflictingEvidence: List<String> = emptyList(),
    val rationale: String = "",
    val recommendedActions: List<String> = emptyList(),
    val ruleIds: List<String> = emptyList(),
)

data class ProbeRetestComparison(
    val comparisonKey: String = "",
    val probeType: ProbeType = ProbeType.OTHER,
    val target: String = "",
    val beforeStatus: ProbeStatus? = null,
    val afterStatus: ProbeStatus? = null,
    val beforeDurationMillis: Long? = null,
    val afterDurationMillis: Long? = null,
    val durationDeltaMillis: Long? = null,
    val change: ProbeChange = ProbeChange.INCONCLUSIVE,
    val summary: String = "",
)

data class RetestComparison(
    val baselineSessionId: String = "",
    val retestSessionId: String = "",
    val comparedAtEpochMillis: Long = 0L,
    val outcome: RetestOutcome = RetestOutcome.INCONCLUSIVE,
    val networkChanged: Boolean? = null,
    val probeComparisons: List<ProbeRetestComparison> = emptyList(),
    val resolvedHypothesisCodes: List<String> = emptyList(),
    val remainingHypothesisCodes: List<String> = emptyList(),
    val newHypothesisCodes: List<String> = emptyList(),
    val summary: String = "",
)

data class DiagnosticSession(
    val schemaVersion: Int = DIAGNOSTIC_SESSION_SCHEMA_VERSION,
    val id: String = UUID.randomUUID().toString(),
    val mode: DiagnosticSessionMode = DiagnosticSessionMode.LOG_ANALYSIS,
    val status: DiagnosticSessionStatus = DiagnosticSessionStatus.DRAFT,
    val title: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val startedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    val targetHost: String = "",
    val targetPort: Int? = null,
    val targetScheme: String = "",
    val inputLog: String? = null,
    val networkSnapshot: NetworkSnapshot? = null,
    val observations: List<ProbeObservation> = emptyList(),
    val hypotheses: List<DiagnosticHypothesis> = emptyList(),
    val retestComparison: RetestComparison? = null,
    val tags: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)
