package com.netsage.app.diagnostic.rules

import com.netsage.app.diagnostic.parser.Observation
import com.netsage.app.diagnostic.parser.ObservationKind

/** P1 is the highest diagnostic priority. This is an ordering, not a probability. */
enum class DiagnosticPriority(val sortRank: Int) {
    P1(4),
    P2(3),
    P3(2),
    P4(1);

    internal fun loweredBy(levels: Int): DiagnosticPriority {
        val all = values()
        return all[(ordinal + levels.coerceAtLeast(0)).coerceAtMost(all.lastIndex)]
    }
}

data class HttpStatusRange(
    val min: Int,
    val max: Int,
) {
    init {
        require(min in 100..599 && max in 100..599 && min <= max) {
            "HTTP status range must stay within 100..599"
        }
    }

    operator fun contains(statusCode: Int): Boolean = statusCode in min..max
}

data class RuleDefinition(
    val id: String,
    val title: String,
    val priority: DiagnosticPriority,
    val order: Int,
    val positiveKinds: Set<ObservationKind>,
    val positiveHttpStatusRanges: List<HttpStatusRange>,
    val conflictKinds: Set<ObservationKind>,
    val conflictHttpStatusRanges: List<HttpStatusRange>,
    val conflictPenaltyLevels: Int,
    val explanation: String,
    val nextSteps: List<String>,
)

data class DiagnosticRuleSet(
    val schemaVersion: Int,
    val ruleSetId: String,
    val rules: List<RuleDefinition>,
)

data class DiagnosticHypothesis(
    val ruleId: String,
    val title: String,
    val priority: DiagnosticPriority,
    val positiveEvidence: List<Observation>,
    val conflictEvidence: List<Observation>,
    val explanation: String,
    val nextSteps: List<String>,
)
