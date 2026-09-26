package com.netsage.app.diagnostic.session

/** Creates a deterministic before/after comparison without performing any network I/O. */
object RetestComparator {
    fun compare(
        before: DiagnosticSession,
        after: DiagnosticSession,
        comparedAtEpochMillis: Long = System.currentTimeMillis(),
    ): RetestComparison {
        val beforeProbes = comparisonMap(before.observations)
        val afterProbes = comparisonMap(after.observations)
        val keys = (beforeProbes.keys + afterProbes.keys).sorted()
        val probeComparisons = keys.map { key ->
            compareProbe(key, beforeProbes[key], afterProbes[key])
        }

        val improved = probeComparisons.count { it.change == ProbeChange.IMPROVED }
        val regressed = probeComparisons.count { it.change == ProbeChange.REGRESSED }
        val unchanged = probeComparisons.count { it.change == ProbeChange.UNCHANGED }
        val comparable = improved + regressed + unchanged
        val outcome = when {
            comparable == 0 -> RetestOutcome.INCONCLUSIVE
            improved > 0 && regressed > 0 -> RetestOutcome.MIXED
            improved > 0 -> RetestOutcome.IMPROVED
            regressed > 0 -> RetestOutcome.REGRESSED
            else -> RetestOutcome.UNCHANGED
        }

        val beforeHypotheses = before.hypotheses.map(::hypothesisKey).filter(String::isNotBlank).toSet()
        val afterHypotheses = after.hypotheses.map(::hypothesisKey).filter(String::isNotBlank).toSet()
        val networkChanged = compareNetworkSnapshots(before.networkSnapshot, after.networkSnapshot)

        return RetestComparison(
            baselineSessionId = before.id,
            retestSessionId = after.id,
            comparedAtEpochMillis = comparedAtEpochMillis,
            outcome = outcome,
            networkChanged = networkChanged,
            probeComparisons = probeComparisons,
            resolvedHypothesisCodes = (beforeHypotheses - afterHypotheses).sorted(),
            remainingHypothesisCodes = (beforeHypotheses intersect afterHypotheses).sorted(),
            newHypothesisCodes = (afterHypotheses - beforeHypotheses).sorted(),
            summary = buildSummary(outcome, improved, unchanged, regressed, probeComparisons.size),
        )
    }

    private fun comparisonMap(observations: List<ProbeObservation>): Map<String, ProbeObservation> {
        val counts = mutableMapOf<String, Int>()
        val result = linkedMapOf<String, ProbeObservation>()
        observations
            .sortedWith(compareBy<ProbeObservation> { it.sequence }.thenBy { it.startedAtEpochMillis }.thenBy { it.id })
            .forEach { observation ->
                val base = "${observation.type.name}|${observation.target.trim().lowercase()}"
                val occurrence = counts.getOrDefault(base, 0)
                counts[base] = occurrence + 1
                result["$base#$occurrence"] = observation
            }
        return result
    }

    private fun compareProbe(
        key: String,
        before: ProbeObservation?,
        after: ProbeObservation?,
    ): ProbeRetestComparison {
        val change = when {
            before == null -> ProbeChange.ADDED
            after == null -> ProbeChange.REMOVED
            compareHttpStatus(before, after) != null -> compareHttpStatus(before, after)!!
            statusRank(before.status) == null || statusRank(after.status) == null -> ProbeChange.INCONCLUSIVE
            statusRank(after.status)!! < statusRank(before.status)!! -> ProbeChange.IMPROVED
            statusRank(after.status)!! > statusRank(before.status)!! -> ProbeChange.REGRESSED
            else -> ProbeChange.UNCHANGED
        }
        val durationDelta = if (before?.durationMillis != null && after?.durationMillis != null) {
            after.durationMillis - before.durationMillis
        } else {
            null
        }
        val representative = after ?: before
        return ProbeRetestComparison(
            comparisonKey = key,
            probeType = representative?.type ?: ProbeType.OTHER,
            target = representative?.target.orEmpty(),
            beforeStatus = before?.status,
            afterStatus = after?.status,
            beforeDurationMillis = before?.durationMillis,
            afterDurationMillis = after?.durationMillis,
            durationDeltaMillis = durationDelta,
            change = change,
            summary = tcpCountSummary(before, after) ?: when (change) {
                ProbeChange.IMPROVED -> "Probe status improved"
                ProbeChange.UNCHANGED -> "Probe status is unchanged"
                ProbeChange.REGRESSED -> "Probe status regressed"
                ProbeChange.ADDED -> "Probe exists only in the retest"
                ProbeChange.REMOVED -> "Probe was not repeated"
                ProbeChange.INCONCLUSIVE -> "Probe states are not comparable"
            },
        )
    }

    private fun tcpCountSummary(before: ProbeObservation?, after: ProbeObservation?): String? {
        if ((before ?: after)?.type != ProbeType.TCP) return null
        val lines = listOf("ipv4" to "IPv4", "ipv6" to "IPv6").mapNotNull { (key, label) ->
            val beforeCount = before?.attributes?.get("${key}Connections")?.takeIf(::isTcpCount)
            val afterCount = after?.attributes?.get("${key}Connections")?.takeIf(::isTcpCount)
            if (beforeCount == null && afterCount == null) null
            else "$label TCP 建连成功：前 ${beforeCount ?: "未记录"} → 后 ${afterCount ?: "未记录"}"
        }
        return lines.takeIf { it.isNotEmpty() }?.joinToString("；")
    }

    private fun isTcpCount(value: String): Boolean = value.matches(Regex("[0-9]+/[1-9][0-9]*"))

    private fun compareHttpStatus(before: ProbeObservation, after: ProbeObservation): ProbeChange? {
        if (before.type != ProbeType.HTTP || after.type != ProbeType.HTTP) return null
        val beforeCode = before.attributes["statusCode"]?.toIntOrNull()
        val afterCode = after.attributes["statusCode"]?.toIntOrNull()
        if (beforeCode == afterCode) return null
        if (beforeCode == null || afterCode == null) return ProbeChange.INCONCLUSIVE

        val beforeRank = httpStatusRank(beforeCode)
        val afterRank = httpStatusRank(afterCode)
        return when {
            afterRank < beforeRank -> ProbeChange.IMPROVED
            afterRank > beforeRank -> ProbeChange.REGRESSED
            else -> ProbeChange.INCONCLUSIVE
        }
    }

    private fun httpStatusRank(statusCode: Int): Int = when (statusCode) {
        in 500..599 -> 2
        in 400..499 -> 1
        else -> 0
    }

    /** Null means one or both sessions did not capture a snapshot. */
    private fun compareNetworkSnapshots(before: NetworkSnapshot?, after: NetworkSnapshot?): Boolean? {
        if (before == null || after == null) return null
        return networkFingerprint(before) != networkFingerprint(after)
    }

    private fun networkFingerprint(snapshot: NetworkSnapshot): List<Any?> = listOf(
        snapshot.connected,
        snapshot.transport,
        snapshot.localAddresses.sorted(),
        snapshot.gatewayAddresses.sorted(),
        snapshot.dnsServers.sorted(),
        snapshot.hasIpv4,
        snapshot.hasIpv6,
        snapshot.metered,
    )

    private fun hypothesisKey(hypothesis: DiagnosticHypothesis): String = when {
        hypothesis.code.isNotBlank() -> hypothesis.code.trim()
        hypothesis.ruleIds.isNotEmpty() -> hypothesis.ruleIds.sorted().joinToString("+")
        else -> hypothesis.title.trim()
    }

    private fun statusRank(status: ProbeStatus): Int? = when (status) {
        ProbeStatus.SUCCESS -> 0
        ProbeStatus.WARNING -> 1
        ProbeStatus.TIMEOUT,
        ProbeStatus.FAILED,
        -> 2
        ProbeStatus.PENDING,
        ProbeStatus.RUNNING,
        ProbeStatus.SKIPPED,
        ProbeStatus.CANCELLED,
        -> null
    }

    private fun buildSummary(
        outcome: RetestOutcome,
        improved: Int,
        unchanged: Int,
        regressed: Int,
        total: Int,
    ): String = "Outcome ${outcome.name.lowercase()}: " +
        "$improved improved, $unchanged unchanged, $regressed regressed, $total total probe comparisons."
}
