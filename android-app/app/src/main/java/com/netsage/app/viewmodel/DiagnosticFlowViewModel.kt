package com.netsage.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netsage.app.diagnostic.parser.ObservationParser
import com.netsage.app.diagnostic.probe.AndroidProbeRunner
import com.netsage.app.diagnostic.probe.NetworkSnapshot as ProbeNetworkSnapshot
import com.netsage.app.diagnostic.probe.NetworkTransport as ProbeNetworkTransport
import com.netsage.app.diagnostic.probe.ProbeKind
import com.netsage.app.diagnostic.probe.ProbeObservation as RuntimeProbeObservation
import com.netsage.app.diagnostic.probe.ProbeRequest
import com.netsage.app.diagnostic.probe.ProbeRunner
import com.netsage.app.diagnostic.probe.ProbeScheme
import com.netsage.app.diagnostic.probe.ProbeStatus as RuntimeProbeStatus
import com.netsage.app.diagnostic.probe.ProbeTarget
import com.netsage.app.diagnostic.rules.DiagnosticHypothesis as RuleHypothesis
import com.netsage.app.diagnostic.rules.DiagnosticPriority
import com.netsage.app.diagnostic.rules.LocalRuleEngine
import com.netsage.app.diagnostic.session.DiagnosticHypothesis
import com.netsage.app.diagnostic.session.DiagnosticSession
import com.netsage.app.diagnostic.session.DiagnosticSessionMode
import com.netsage.app.diagnostic.session.DiagnosticSessionStatus
import com.netsage.app.diagnostic.session.EvidenceStrength
import com.netsage.app.diagnostic.session.NetworkSnapshot
import com.netsage.app.diagnostic.session.NetworkTransport
import com.netsage.app.diagnostic.session.ProbeObservation
import com.netsage.app.diagnostic.session.ProbeStatus
import com.netsage.app.diagnostic.session.ProbeType
import com.netsage.app.diagnostic.session.REFERENCE_HOST_KEY
import com.netsage.app.diagnostic.session.REFERENCE_NETWORK_CHANGED_KEY
import com.netsage.app.diagnostic.session.REFERENCE_ROLE
import com.netsage.app.diagnostic.session.RetestComparator
import com.netsage.app.diagnostic.session.SessionStore
import com.netsage.app.diagnostic.session.TARGET_ROLE_KEY
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TCP_ATTEMPTS_KEY = "tcpAttemptsPerAddress"

data class DiagnosticFlowUiState(
    val running: Boolean = false,
    val progressMessage: String = "",
    val completedSteps: Int = 0,
    val totalSteps: Int = 4,
    val sessions: List<DiagnosticSession> = emptyList(),
    val currentSession: DiagnosticSession? = null,
    val error: String? = null,
)

class DiagnosticFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val parser = ObservationParser()
    private val ruleEngine = LocalRuleEngine.fromAsset(appContext.assets::open)
    private val probeRunner: ProbeRunner = AndroidProbeRunner(appContext)
    private val sessionStore = SessionStore(appContext)
    private var runningJob: Job? = null
    private var activeRunId: String? = null

    private val _uiState = MutableStateFlow(
        DiagnosticFlowUiState(sessions = sessionStore.loadAll())
    )
    val uiState: StateFlow<DiagnosticFlowUiState> = _uiState.asStateFlow()

    fun runLogDiagnosis(logText: String) {
        if (logText.isBlank()) return
        val runId = UUID.randomUUID().toString()
        beginRun(runId)
        runningJob = viewModelScope.launch {
            publishIfActive(runId) {
                it.copy(running = true, progressMessage = "正在解析本地日志…", error = null)
            }
            try {
                val startedAt = System.currentTimeMillis()
                val hypotheses = buildHypotheses(logText, emptyList())
                val session = DiagnosticSession(
                    mode = DiagnosticSessionMode.LOG_ANALYSIS,
                    status = DiagnosticSessionStatus.COMPLETED,
                    title = "日志诊断",
                    createdAtEpochMillis = startedAt,
                    startedAtEpochMillis = startedAt,
                    completedAtEpochMillis = System.currentTimeMillis(),
                    inputLog = logText,
                    hypotheses = hypotheses,
                    tags = hypotheses.map { it.category }.filter(String::isNotBlank).distinct(),
                )
                persistAndPublish(session, runId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                publishIfActive(runId) {
                    it.copy(
                        running = false,
                        progressMessage = "",
                        error = error.message ?: "本地日志诊断失败",
                    )
                }
            }
        }
    }

    fun runCheckup(
        host: String,
        port: Int,
        scheme: ProbeScheme,
        logText: String = "",
        combined: Boolean = false,
        referenceHost: String = "",
        tcpAttempts: Int = 1,
    ) {
        runProbeDiagnostic(
            host = host,
            port = port,
            scheme = scheme,
            inputLog = logText,
            mode = if (combined) DiagnosticSessionMode.COMBINED else DiagnosticSessionMode.QUICK_CHECKUP,
            baseline = null,
            referenceHost = referenceHost,
            tcpAttempts = tcpAttempts,
        )
    }

    fun retest(session: DiagnosticSession) {
        if (session.targetHost.isBlank() || session.targetPort == null) return
        val scheme = if (session.targetScheme.equals("http", ignoreCase = true)) {
            ProbeScheme.HTTP
        } else {
            ProbeScheme.HTTPS
        }
        runProbeDiagnostic(
            host = session.targetHost,
            port = session.targetPort,
            scheme = scheme,
            inputLog = session.inputLog.orEmpty(),
            mode = DiagnosticSessionMode.RETEST,
            baseline = session,
            referenceHost = session.metadata[REFERENCE_HOST_KEY].orEmpty(),
            tcpAttempts = session.metadata[TCP_ATTEMPTS_KEY]?.toIntOrNull()?.takeIf { it in 1..3 } ?: 1,
        )
    }

    fun cancelRun() {
        runningJob?.cancel(CancellationException("User cancelled the local diagnostic run"))
    }

    fun selectSession(id: String) {
        val cached = _uiState.value.sessions.firstOrNull { it.id == id }
        if (cached != null) {
            _uiState.value = _uiState.value.copy(currentSession = cached)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            sessionStore.load(id)?.let { loaded ->
                _uiState.value = _uiState.value.copy(currentSession = loaded)
            }
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionStore.delete(id)
            val sessions = sessionStore.loadAll()
            _uiState.value = _uiState.value.copy(
                sessions = sessions,
                currentSession = _uiState.value.currentSession?.takeUnless { it.id == id },
            )
        }
    }

    fun clearSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionStore.clear()
            _uiState.value = _uiState.value.copy(sessions = emptyList(), currentSession = null)
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun runProbeDiagnostic(
        host: String,
        port: Int,
        scheme: ProbeScheme,
        inputLog: String,
        mode: DiagnosticSessionMode,
        baseline: DiagnosticSession?,
        referenceHost: String,
        tcpAttempts: Int,
    ) {
        val sessionId = UUID.randomUUID().toString()
        beginRun(sessionId)
        val startedAt = System.currentTimeMillis()
        val collected = mutableListOf<RuntimeProbeObservation>()
        val referenceCollected = mutableListOf<RuntimeProbeObservation>()
        val checkedReferenceHost = referenceHost.trim()
        val totalSteps = if (checkedReferenceHost.isBlank()) 4 else 8

        runningJob = viewModelScope.launch {
            publishIfActive(sessionId) {
                it.copy(
                    running = true,
                    progressMessage = "正在读取当前网络状态…",
                    completedSteps = 0,
                    totalSteps = totalSteps,
                    error = null,
                )
            }
            var preflightSnapshot: ProbeNetworkSnapshot? = null
            try {
                val draft = DiagnosticSession(
                    id = sessionId,
                    mode = mode,
                    status = DiagnosticSessionStatus.RUNNING,
                    title = if (mode == DiagnosticSessionMode.RETEST) "修复后复测" else "本地网络体检",
                    createdAtEpochMillis = startedAt,
                    startedAtEpochMillis = startedAt,
                    targetHost = host,
                    targetPort = port,
                    targetScheme = scheme.value,
                    inputLog = inputLog.ifBlank { null },
                    metadata = buildMap {
                        put(TCP_ATTEMPTS_KEY, tcpAttempts.toString())
                        if (checkedReferenceHost.isNotBlank()) put(REFERENCE_HOST_KEY, checkedReferenceHost)
                    },
                )
                withContext(Dispatchers.IO) { sessionStore.save(draft) }

                preflightSnapshot = probeRunner.captureNetworkSnapshot()
                val result = probeRunner.run(
                    ProbeRequest(ProbeTarget(host = host, port = port, scheme = scheme), tcpAttempts = tcpAttempts)
                ) { observation ->
                    collected += observation
                    publishIfActive(sessionId) { it.copy(
                        completedSteps = collected.size,
                        progressMessage = "主目标 ${collected.size}/4：${observation.kind.name} 已完成",
                    ) }
                }

                val referenceResult = if (checkedReferenceHost.isNotBlank()) {
                    publishIfActive(sessionId) { it.copy(progressMessage = "正在检测对照目标…") }
                    probeRunner.run(
                        ProbeRequest(ProbeTarget(host = checkedReferenceHost, port = port, scheme = scheme), tcpAttempts = tcpAttempts)
                    ) { observation ->
                        referenceCollected += observation
                        publishIfActive(sessionId) { it.copy(
                            completedSteps = 4 + referenceCollected.size,
                            progressMessage = "对照目标 ${referenceCollected.size}/4：${observation.kind.name} 已完成",
                        ) }
                    }
                } else null

                val storedObservations = mapRunObservations(result.observations, referenceResult?.observations.orEmpty())
                val hypotheses = buildHypotheses(inputLog, result.observations, result.snapshot)
                val partial = storedObservations.any {
                    it.status == ProbeStatus.WARNING ||
                        it.status == ProbeStatus.FAILED ||
                        it.status == ProbeStatus.TIMEOUT
                }
                var completed = draft.copy(
                    status = if (partial) DiagnosticSessionStatus.PARTIAL else DiagnosticSessionStatus.COMPLETED,
                    completedAtEpochMillis = System.currentTimeMillis(),
                    networkSnapshot = mapSnapshot(result.snapshot),
                    observations = storedObservations,
                    hypotheses = hypotheses,
                    tags = hypotheses.map { it.category }.filter(String::isNotBlank).distinct(),
                    metadata = buildMap {
                        put("probeDurationMillis", (result.durationMillis + (referenceResult?.durationMillis ?: 0)).toString())
                        put(TCP_ATTEMPTS_KEY, tcpAttempts.toString())
                        if (referenceResult != null) {
                            put(REFERENCE_HOST_KEY, checkedReferenceHost)
                            put(REFERENCE_NETWORK_CHANGED_KEY, networkChanged(result.snapshot, referenceResult.snapshot).toString())
                        }
                    },
                )
                if (baseline != null) {
                    completed = completed.copy(
                        retestComparison = RetestComparator.compare(baseline, completed)
                    )
                }
                persistAndPublish(completed, sessionId)
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable + Dispatchers.IO) {
                    val cancelledSession = DiagnosticSession(
                        id = sessionId,
                        mode = mode,
                        status = DiagnosticSessionStatus.CANCELLED,
                        title = "已取消的本地检测",
                        createdAtEpochMillis = startedAt,
                        startedAtEpochMillis = startedAt,
                        completedAtEpochMillis = System.currentTimeMillis(),
                        targetHost = host,
                        targetPort = port,
                        targetScheme = scheme.value,
                        inputLog = inputLog.ifBlank { null },
                        networkSnapshot = preflightSnapshot?.let(::mapSnapshot),
                        observations = mapRunObservations(collected, referenceCollected),
                        metadata = buildMap {
                            put(TCP_ATTEMPTS_KEY, tcpAttempts.toString())
                            if (checkedReferenceHost.isNotBlank()) put(REFERENCE_HOST_KEY, checkedReferenceHost)
                        },
                    )
                    val saved = sessionStore.save(cancelledSession)
                    publishIfActive(sessionId) {
                        it.copy(
                            running = false,
                            progressMessage = "检测已取消，已保留完成的步骤",
                            currentSession = saved,
                            sessions = sessionStore.loadAll(),
                        )
                    }
                }
            } catch (error: Exception) {
                withContext(NonCancellable + Dispatchers.IO) {
                    val failed = DiagnosticSession(
                        id = sessionId,
                        mode = mode,
                        status = DiagnosticSessionStatus.FAILED,
                        title = "未完成的本地检测",
                        createdAtEpochMillis = startedAt,
                        startedAtEpochMillis = startedAt,
                        completedAtEpochMillis = System.currentTimeMillis(),
                        targetHost = host,
                        targetPort = port,
                        targetScheme = scheme.value,
                        inputLog = inputLog.ifBlank { null },
                        networkSnapshot = preflightSnapshot?.let(::mapSnapshot),
                        observations = mapRunObservations(collected, referenceCollected),
                        metadata = buildMap {
                            put("failure", error.message ?: error::class.java.simpleName)
                            put(TCP_ATTEMPTS_KEY, tcpAttempts.toString())
                            if (checkedReferenceHost.isNotBlank()) put(REFERENCE_HOST_KEY, checkedReferenceHost)
                        },
                    )
                    val saved = sessionStore.save(failed)
                    publishIfActive(sessionId) {
                        it.copy(
                            running = false,
                            progressMessage = "",
                            currentSession = saved,
                            sessions = sessionStore.loadAll(),
                            error = error.message ?: "本地检测失败",
                        )
                    }
                }
            }
        }
    }

    private fun beginRun(runId: String) {
        activeRunId = runId
        runningJob?.cancel()
    }

    private fun isActiveRun(runId: String): Boolean = activeRunId == runId

    private fun publishIfActive(runId: String, transform: (DiagnosticFlowUiState) -> DiagnosticFlowUiState) {
        if (isActiveRun(runId)) _uiState.value = transform(_uiState.value)
    }

    private suspend fun persistAndPublish(session: DiagnosticSession, runId: String) {
        val saved = withContext(Dispatchers.IO) { sessionStore.save(session) }
        val sessions = withContext(Dispatchers.IO) { sessionStore.loadAll() }
        publishIfActive(runId) {
            it.copy(
                running = false,
                progressMessage = "",
                sessions = sessions,
                currentSession = saved,
                error = null,
            )
        }
    }

    private fun buildHypotheses(
        inputLog: String,
        runtimeObservations: List<RuntimeProbeObservation>,
        snapshot: ProbeNetworkSnapshot? = null,
    ): List<DiagnosticHypothesis> {
        disconnectedNetworkHypothesis(snapshot)?.let { return listOf(it) }

        val evidenceText = buildString {
            if (inputLog.isNotBlank()) appendLine(inputLog)
            snapshot?.let { appendSnapshotEvidence(it) }
            runtimeObservations.forEach { appendRuntimeEvidence(it) }
        }
        val parsed = parser.parse(evidenceText)
        val matched = ruleEngine.evaluate(parsed)
        if (matched.isNotEmpty()) return matched.map(::mapHypothesis)

        val allProbesHealthy = runtimeObservations.isNotEmpty() &&
            runtimeObservations.all(::isHealthyProbe)
        return listOf(
            DiagnosticHypothesis(
                code = if (allProbesHealthy) "network.no_known_anomaly" else "input.insufficient_evidence",
                title = if (allProbesHealthy) "本次检测未发现已知高优先级异常" else "当前证据不足以归类",
                category = "GENERAL",
                priority = 20,
                evidenceStrength = EvidenceStrength.LOW,
                matchedEvidence = if (allProbesHealthy) {
                    listOf("DNS、TCP、TLS/HTTP 等已执行步骤未出现规则可识别的异常")
                } else {
                    listOf("未命中当前本地规则集")
                },
                rationale = "该结果仅表示当前规则和本次采样未发现明确故障，不代表网络始终正常。",
                recommendedActions = listOf("补充发生时间、错误码和原始日志", "在故障复现时重新执行体检"),
                ruleIds = emptyList(),
            )
        )
    }

    private fun StringBuilder.appendSnapshotEvidence(snapshot: ProbeNetworkSnapshot) {
        snapshotEvidenceLines(snapshot).forEach(::appendLine)
        snapshot.ipAddresses.forEach { appendLine(it) }
    }

    private fun StringBuilder.appendRuntimeEvidence(observation: RuntimeProbeObservation) {
        appendLine("${observation.kind.name} ${observation.status.name} ${observation.target}")
        observation.error?.let { appendLine("${observation.kind.name} ${it.code}: ${it.message}") }
        if (observation.kind == ProbeKind.HTTP) {
            observation.evidence["statusCode"]?.let { appendLine("HTTP status: $it") }
        }
        observation.evidence.forEach { (key, value) -> appendLine("${observation.kind.name} $key=$value") }
    }

    private fun mapHypothesis(source: RuleHypothesis): DiagnosticHypothesis {
        val strength = when {
            source.positiveEvidence.size >= 3 -> EvidenceStrength.HIGH
            source.positiveEvidence.size == 2 -> EvidenceStrength.MEDIUM
            else -> EvidenceStrength.LOW
        }
        return DiagnosticHypothesis(
            code = source.ruleId,
            title = source.title,
            category = source.ruleId.substringBefore('.').uppercase(),
            priority = source.priority.toScore(),
            evidenceStrength = strength,
            matchedEvidence = source.positiveEvidence.map {
                "第 ${it.lineNumber} 行：${it.sourceExcerpt}"
            },
            conflictingEvidence = source.conflictEvidence.map {
                "第 ${it.lineNumber} 行：${it.sourceExcerpt}"
            },
            rationale = source.explanation,
            recommendedActions = source.nextSteps,
            ruleIds = listOf(source.ruleId),
        )
    }

    private fun DiagnosticPriority.toScore(): Int = when (this) {
        DiagnosticPriority.P1 -> 100
        DiagnosticPriority.P2 -> 75
        DiagnosticPriority.P3 -> 50
        DiagnosticPriority.P4 -> 25
    }

    private fun mapSnapshot(source: ProbeNetworkSnapshot): NetworkSnapshot = NetworkSnapshot(
        capturedAtEpochMillis = source.capturedAtEpochMillis,
        connected = source.connected,
        transport = when {
            ProbeNetworkTransport.VPN in source.transports -> NetworkTransport.VPN
            ProbeNetworkTransport.WIFI in source.transports -> NetworkTransport.WIFI
            ProbeNetworkTransport.CELLULAR in source.transports -> NetworkTransport.CELLULAR
            ProbeNetworkTransport.ETHERNET in source.transports -> NetworkTransport.ETHERNET
            ProbeNetworkTransport.BLUETOOTH in source.transports -> NetworkTransport.BLUETOOTH
            source.transports.isEmpty() -> NetworkTransport.UNKNOWN
            else -> NetworkTransport.OTHER
        },
        localAddresses = source.ipAddresses,
        gatewayAddresses = source.gateways,
        dnsServers = source.dnsServers,
        hasIpv4 = source.ipAddresses.any { !it.substringBefore('/').contains(':') },
        hasIpv6 = source.ipAddresses.any { it.substringBefore('/').contains(':') },
        metered = source.metered,
        validated = source.validated,
        captivePortalDetected = source.captivePortal,
        attributes = buildMap {
            source.interfaceName?.let { put("interface", it) }
            source.mtu?.let { put("mtu", it.toString()) }
            put("internetCapable", source.internetCapable.toString())
            put("privateDnsActive", source.privateDnsActive.toString())
            source.privateDnsServerName?.let { put("privateDnsServer", it) }
            source.proxyHost?.let { put("proxy", "$it:${source.proxyPort ?: ""}") }
            source.error?.let { put("snapshotError", it) }
        },
    )

    private fun mapObservation(index: Int, source: RuntimeProbeObservation): ProbeObservation = ProbeObservation(
        sequence = index + 1,
        type = when (source.kind) {
            ProbeKind.DNS -> ProbeType.DNS
            ProbeKind.TCP -> ProbeType.TCP
            ProbeKind.TLS -> ProbeType.TLS
            ProbeKind.HTTP -> ProbeType.HTTP
        },
        status = when {
            source.kind == ProbeKind.HTTP && source.evidence["statusCode"]?.toIntOrNull() in 500..599 ->
                ProbeStatus.FAILED
            source.kind == ProbeKind.HTTP && source.evidence["statusCode"]?.toIntOrNull() in 400..499 ->
                ProbeStatus.WARNING
            else -> when (source.status) {
            RuntimeProbeStatus.SUCCESS -> ProbeStatus.SUCCESS
            RuntimeProbeStatus.FAILURE -> ProbeStatus.FAILED
            RuntimeProbeStatus.TIMEOUT -> ProbeStatus.TIMEOUT
            RuntimeProbeStatus.CANCELLED -> ProbeStatus.CANCELLED
            RuntimeProbeStatus.SKIPPED -> ProbeStatus.SKIPPED
            }
        },
        target = source.target,
        startedAtEpochMillis = source.startedAtEpochMillis,
        finishedAtEpochMillis = source.startedAtEpochMillis + source.durationMillis,
        durationMillis = source.durationMillis,
        summary = when (source.status) {
            RuntimeProbeStatus.SUCCESS -> "${source.kind.name} 检测完成"
            RuntimeProbeStatus.TIMEOUT -> "${source.kind.name} 检测超时"
            RuntimeProbeStatus.FAILURE -> "${source.kind.name} 检测失败"
            RuntimeProbeStatus.CANCELLED -> "${source.kind.name} 检测已取消"
            RuntimeProbeStatus.SKIPPED -> "${source.kind.name} 检测已跳过"
        },
        evidence = buildList {
            source.evidence.forEach { (key, value) -> add("$key: $value") }
            source.rawEvidence?.takeIf(String::isNotBlank)?.let { add(it.take(500)) }
        },
        attributes = source.evidence,
        errorCode = source.error?.code,
        errorMessage = source.error?.message,
    )

    private fun mapRunObservations(
        primary: List<RuntimeProbeObservation>,
        reference: List<RuntimeProbeObservation>,
    ): List<ProbeObservation> = primary.mapIndexed(::mapObservation) +
        reference.mapIndexed { index, observation ->
            mapObservation(primary.size + index, observation).copy(
                attributes = observation.evidence + (TARGET_ROLE_KEY to REFERENCE_ROLE),
            )
        }

    private fun isHealthyProbe(observation: RuntimeProbeObservation): Boolean =
        observation.status == RuntimeProbeStatus.SUCCESS &&
            !(observation.kind == ProbeKind.HTTP &&
                observation.evidence["statusCode"]?.toIntOrNull() in 400..599)
}

internal fun networkChanged(before: ProbeNetworkSnapshot, after: ProbeNetworkSnapshot): Boolean =
    before.connected != after.connected ||
        before.interfaceName != after.interfaceName ||
        before.transports != after.transports ||
        before.ipAddresses != after.ipAddresses ||
        before.gateways != after.gateways ||
        before.dnsServers != after.dnsServers ||
        before.validated != after.validated ||
        before.captivePortal != after.captivePortal ||
        before.privateDnsActive != after.privateDnsActive ||
        before.privateDnsServerName != after.privateDnsServerName ||
        before.proxyHost != after.proxyHost ||
        before.proxyPort != after.proxyPort

internal fun disconnectedNetworkHypothesis(
    snapshot: ProbeNetworkSnapshot?,
): DiagnosticHypothesis? {
    if (snapshot == null || snapshot.connected) return null
    return DiagnosticHypothesis(
        code = "network.device_offline",
        title = "当前设备未连接到可用网络",
        category = "CONNECTION",
        priority = 95,
        evidenceStrength = EvidenceStrength.HIGH,
        matchedEvidence = listOf(
            "系统网络快照显示 connected=false",
            "当前没有可用的本地 IP、网关或 DNS 路径",
        ),
        rationale = "设备没有活动网络连接时，后续 DNS、TCP、TLS 和 HTTP 检测会连锁失败。应先恢复基础连接，再判断目标服务是否异常。",
        recommendedActions = listOf(
            "关闭飞行模式，并启用 Wi-Fi 或移动数据",
            "确认已连接到可用热点或蜂窝网络",
            "恢复连接后使用“修复后复测”重新运行全部探测",
        ),
        ruleIds = listOf("network.device_offline"),
    )
}

internal fun snapshotEvidenceLines(snapshot: ProbeNetworkSnapshot): List<String> = buildList {
    // A disconnected snapshot is handled before parsing. Do not turn absent routes or
    // resolvers from an offline device into a separate configuration diagnosis.
    if (snapshot.connected && snapshot.gateways.isEmpty()) add("default gateway missing")
    if (snapshot.connected && snapshot.dnsServers.isEmpty()) add("DNS configuration missing")
    if (snapshot.captivePortal) add("captive portal detected")
    if (!snapshot.proxyHost.isNullOrBlank()) add("proxy detected: ${snapshot.proxyHost}:${snapshot.proxyPort}")
}
