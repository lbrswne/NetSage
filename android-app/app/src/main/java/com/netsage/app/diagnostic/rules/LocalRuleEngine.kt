package com.netsage.app.diagnostic.rules

import com.netsage.app.diagnostic.parser.Observation
import com.netsage.app.diagnostic.parser.ObservationKind
import java.io.InputStream

class LocalRuleEngine(private val ruleSet: DiagnosticRuleSet) {
    fun evaluate(observations: List<Observation>, limit: Int = DEFAULT_LIMIT): List<DiagnosticHypothesis> {
        require(limit >= 0) { "limit must not be negative" }
        if (observations.isEmpty() || limit == 0) return emptyList()

        return ruleSet.rules.mapNotNull { rule -> match(rule, observations) }
            .sortedWith(
                compareByDescending<RankedHypothesis> { it.hypothesis.priority.sortRank }
                    .thenByDescending { it.hypothesis.positiveEvidence.size }
                    .thenBy { it.order }
                    .thenBy { it.hypothesis.ruleId },
            )
            .take(limit)
            .map(RankedHypothesis::hypothesis)
    }

    private fun match(rule: RuleDefinition, observations: List<Observation>): RankedHypothesis? {
        val positiveEvidence = observations.filter { observation ->
            observation.kind in rule.positiveKinds ||
                (observation.kind == ObservationKind.HTTP_STATUS &&
                    observation.httpStatusCode()?.let { code ->
                        rule.positiveHttpStatusRanges.any { code in it }
                    } == true)
        }.distinctEvidence()

        if (positiveEvidence.isEmpty()) return null

        val conflictEvidence = observations.filter { observation ->
            observation.kind in rule.conflictKinds ||
                (observation.kind == ObservationKind.HTTP_STATUS &&
                    observation.httpStatusCode()?.let { code ->
                        rule.conflictHttpStatusRanges.any { code in it }
                    } == true)
        }.distinctEvidence()

        val effectivePriority = rule.priority.loweredBy(
            if (conflictEvidence.isEmpty()) 0 else rule.conflictPenaltyLevels,
        )

        return RankedHypothesis(
            order = rule.order,
            hypothesis = DiagnosticHypothesis(
                ruleId = rule.id,
                title = rule.title,
                priority = effectivePriority,
                positiveEvidence = positiveEvidence,
                conflictEvidence = conflictEvidence,
                explanation = rule.explanation,
                nextSteps = rule.nextSteps,
            ),
        )
    }

    private fun Observation.httpStatusCode(): Int? =
        numericValue?.toInt() ?: value?.toIntOrNull()

    private fun List<Observation>.distinctEvidence(): List<Observation> =
        distinctBy { observation ->
            listOf(
                observation.kind.name,
                observation.lineNumber,
                observation.value.orEmpty(),
                observation.numericValue ?: "",
            ).joinToString("|")
        }

    private data class RankedHypothesis(
        val order: Int,
        val hypothesis: DiagnosticHypothesis,
    )

    companion object {
        const val DEFAULT_LIMIT = 3

        fun fromAsset(
            assetOpener: (String) -> InputStream,
            assetName: String = RuleDefinitionLoader.DEFAULT_ASSET_NAME,
        ): LocalRuleEngine = LocalRuleEngine(
            RuleDefinitionLoader.fromAsset(assetOpener, assetName),
        )

        fun fromInputStream(input: InputStream): LocalRuleEngine =
            LocalRuleEngine(RuleDefinitionLoader.fromInputStream(input))

        fun fromJson(json: String): LocalRuleEngine =
            LocalRuleEngine(RuleDefinitionLoader.fromJson(json))
    }
}
