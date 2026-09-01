package com.netsage.app.diagnostic.rules

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.netsage.app.diagnostic.parser.ObservationKind
import java.io.InputStream

object RuleDefinitionLoader {
    const val DEFAULT_ASSET_NAME = "diagnostic_rules_v1.json"

    fun fromAsset(
        assetOpener: (String) -> InputStream,
        assetName: String = DEFAULT_ASSET_NAME,
    ): DiagnosticRuleSet = assetOpener(assetName).use(::fromInputStream)

    fun fromInputStream(input: InputStream): DiagnosticRuleSet =
        input.bufferedReader(Charsets.UTF_8).use { fromJson(it.readText()) }

    fun fromJson(json: String): DiagnosticRuleSet {
        require(json.isNotBlank()) { "Rule JSON must not be blank" }

        val dto = try {
            Gson().fromJson(json, RuleSetDto::class.java)
        } catch (error: JsonParseException) {
            throw IllegalArgumentException("Invalid diagnostic rule JSON", error)
        } ?: throw IllegalArgumentException("Diagnostic rule JSON must contain an object")

        val schemaVersion = dto.schemaVersion
            ?: throw IllegalArgumentException("schemaVersion is required")
        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported diagnostic rule schema version: $schemaVersion"
        }

        val ruleSetId = dto.ruleSetId.required("ruleSetId")
        val ruleDtos = dto.rules ?: throw IllegalArgumentException("rules is required")
        require(ruleDtos.isNotEmpty()) { "Rule set must contain at least one rule" }

        val rules = ruleDtos.mapIndexed { index, rule -> rule.toDefinition(index) }
        val duplicateIds = rules.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) { "Duplicate rule ids: ${duplicateIds.sorted().joinToString()}" }

        return DiagnosticRuleSet(
            schemaVersion = schemaVersion,
            ruleSetId = ruleSetId,
            rules = rules,
        )
    }

    private fun RuleDto.toDefinition(index: Int): RuleDefinition {
        val id = id.required("rules[$index].id")
        val title = title.required("rules[$index].title")
        val priorityValue = priority.required("rules[$index].priority")
        val parsedPriority = runCatching { DiagnosticPriority.valueOf(priorityValue.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Unknown priority '$priorityValue' in rule '$id'") }
        val positiveKinds = positiveKinds.toObservationKinds(id, "positiveKinds")
        val positiveRanges = positiveHttpStatusRanges.toRanges(id, "positiveHttpStatusRanges")
        require(positiveKinds.isNotEmpty() || positiveRanges.isNotEmpty()) {
            "Rule '$id' needs at least one positive condition"
        }

        return RuleDefinition(
            id = id,
            title = title,
            priority = parsedPriority,
            order = order ?: index,
            positiveKinds = positiveKinds,
            positiveHttpStatusRanges = positiveRanges,
            conflictKinds = conflictKinds.toObservationKinds(id, "conflictKinds"),
            conflictHttpStatusRanges = conflictHttpStatusRanges.toRanges(id, "conflictHttpStatusRanges"),
            conflictPenaltyLevels = (conflictPenaltyLevels ?: 1).coerceAtLeast(0),
            explanation = explanation.required("rules[$index].explanation"),
            nextSteps = nextSteps.orEmpty().map(String::trim).filter(String::isNotEmpty),
        )
    }

    private fun List<String>?.toObservationKinds(ruleId: String, field: String): Set<ObservationKind> =
        orEmpty().map { rawKind ->
            runCatching { ObservationKind.valueOf(rawKind.uppercase()) }
                .getOrElse { throw IllegalArgumentException("Unknown observation kind '$rawKind' in $ruleId.$field") }
        }.toSet()

    private fun List<StatusRangeDto>?.toRanges(ruleId: String, field: String): List<HttpStatusRange> =
        orEmpty().mapIndexed { index, range ->
            val min = range.min ?: throw IllegalArgumentException("$ruleId.$field[$index].min is required")
            val max = range.max ?: throw IllegalArgumentException("$ruleId.$field[$index].max is required")
            HttpStatusRange(min = min, max = max)
        }

    private fun String?.required(field: String): String {
        val value = this?.trim().orEmpty()
        require(value.isNotEmpty()) { "$field is required" }
        return value
    }

    private const val SUPPORTED_SCHEMA_VERSION = 1

    private class RuleSetDto {
        var schemaVersion: Int? = null
        var ruleSetId: String? = null
        var rules: List<RuleDto>? = null
    }

    private class RuleDto {
        var id: String? = null
        var title: String? = null
        var priority: String? = null
        var order: Int? = null
        var positiveKinds: List<String>? = null
        var positiveHttpStatusRanges: List<StatusRangeDto>? = null
        var conflictKinds: List<String>? = null
        var conflictHttpStatusRanges: List<StatusRangeDto>? = null
        var conflictPenaltyLevels: Int? = null
        var explanation: String? = null
        var nextSteps: List<String>? = null
    }

    private class StatusRangeDto {
        var min: Int? = null
        var max: Int? = null
    }
}
