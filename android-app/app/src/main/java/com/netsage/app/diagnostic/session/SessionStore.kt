package com.netsage.app.diagnostic.session

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.netsage.app.util.LocalDocumentIo
import java.util.UUID

/**
 * Internal SharedPreferences-backed store for diagnostic sessions.
 *
 * Writes are synchronous so callers know whether a mutation reached the preferences file.
 * Integrations should invoke mutating methods away from the main thread.
 */
class SessionStore(
    context: Context,
    private val gson: Gson = Gson(),
    private val maxSessions: Int = DEFAULT_MAX_SESSIONS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        require(maxSessions > 0) { "maxSessions must be greater than zero" }
    }

    @Synchronized
    fun loadAll(): List<DiagnosticSession> {
        val raw = runCatching { preferences.getString(KEY_SESSIONS, null) }
            .getOrNull()
            ?: return emptyList()
        return decodeSessions(raw)
            .distinctBy(DiagnosticSession::id)
            .sortedWith(sessionOrder)
            .take(maxSessions)
    }

    @Synchronized
    fun load(sessionId: String): DiagnosticSession? =
        loadAll().firstOrNull { it.id == sessionId }

    /** Inserts a new session or replaces the session with the same id. */
    @Synchronized
    fun save(session: DiagnosticSession): DiagnosticSession {
        val now = clock()
        val current = loadAll()
        val requestedId = session.id.trim().ifEmpty { UUID.randomUUID().toString() }
        val existing = current.firstOrNull { it.id == requestedId }
        val normalized = normalizeForStorage(session.copy(id = requestedId), existing, now)
        val merged = (listOf(normalized) + current.filterNot { it.id == normalized.id })
            .sortedWith(sessionOrder)
            .take(maxSessions)

        check(persist(merged)) { "Unable to persist diagnostic sessions" }
        return normalized
    }

    /** Explicit update alias for integrations that separate create and update actions. */
    @Synchronized
    fun update(session: DiagnosticSession): DiagnosticSession = save(session)

    @Synchronized
    fun delete(sessionId: String): Boolean {
        val current = loadAll()
        val remaining = current.filterNot { it.id == sessionId }
        if (remaining.size == current.size) return false
        return persist(remaining)
    }

    @Synchronized
    fun clear(): Boolean = preferences.edit().remove(KEY_SESSIONS).commit()

    private fun normalizeForStorage(
        session: DiagnosticSession,
        existing: DiagnosticSession?,
        now: Long,
    ): DiagnosticSession {
        val createdAt = when {
            existing != null -> existing.createdAtEpochMillis
            session.createdAtEpochMillis > 0L -> session.createdAtEpochMillis
            else -> now
        }
        return session.copy(
            schemaVersion = DIAGNOSTIC_SESSION_SCHEMA_VERSION,
            id = session.id,
            title = session.title.trim(),
            createdAtEpochMillis = createdAt,
            updatedAtEpochMillis = now,
            targetHost = session.targetHost.trim(),
            targetScheme = session.targetScheme.trim().lowercase(),
            inputLog = session.inputLog?.take(LocalDocumentIo.MAX_LOG_CHARS),
            observations = session.observations.sortedWith(
                compareBy<ProbeObservation> { it.sequence }
                    .thenBy { it.startedAtEpochMillis }
                    .thenBy { it.id }
            ),
            hypotheses = session.hypotheses.sortedWith(
                compareByDescending<DiagnosticHypothesis> { it.priority }
                    .thenBy { it.code }
                    .thenBy { it.id }
            ),
            tags = session.tags.map(String::trim).filter(String::isNotEmpty).distinct(),
        )
    }

    private fun persist(sessions: List<DiagnosticSession>): Boolean {
        val envelope = SessionEnvelope(
            schemaVersion = DIAGNOSTIC_SESSION_SCHEMA_VERSION,
            sessions = sessions,
        )
        return preferences.edit().putString(KEY_SESSIONS, gson.toJson(envelope)).commit()
    }

    /**
     * Accepts both the current envelope and a legacy bare array. Invalid roots return an
     * empty list; invalid individual entries are skipped while valid entries still load.
     */
    private fun decodeSessions(raw: String): List<DiagnosticSession> = runCatching {
        val root = JsonParser.parseString(raw)
        val entries = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> root.asJsonObject.get(KEY_ENVELOPE_SESSIONS)
                ?.takeIf(JsonElement::isJsonArray)
                ?.asJsonArray
                ?: JsonArray()
            else -> JsonArray()
        }
        entries.mapNotNull(::decodeSession)
    }.getOrDefault(emptyList())

    private fun decodeSession(element: JsonElement): DiagnosticSession? = runCatching {
        if (!element.isJsonObject) return@runCatching null
        val source = element.asJsonObject
        val id = source.stringOrNull("id")?.trim().orEmpty()
        if (id.isEmpty()) return@runCatching null

        val normalized = source.deepCopy()
        normalized.keepStringOrRemove("title")
        normalized.keepStringOrRemove("targetHost")
        normalized.keepStringOrRemove("targetScheme")
        normalized.keepKnownEnum("mode", DiagnosticSessionMode.entries.map { it.name })
        normalized.keepKnownEnum("status", DiagnosticSessionStatus.entries.map { it.name })
        normalized.keepArrayOrRemove("observations") { item ->
            item.keepStringOrRemove("id")
            item.keepStringOrRemove("target")
            item.keepStringOrRemove("summary")
            item.keepKnownEnum("type", ProbeType.entries.map { it.name })
            item.keepKnownEnum("status", ProbeStatus.entries.map { it.name })
            item.keepStringArrayOrRemove("evidence")
            item.keepStringMapOrRemove("attributes")
        }
        normalized.keepArrayOrRemove("hypotheses") { item ->
            item.keepStringOrRemove("id")
            item.keepStringOrRemove("code")
            item.keepStringOrRemove("title")
            item.keepStringOrRemove("category")
            item.keepStringOrRemove("rationale")
            item.keepKnownEnum("evidenceStrength", EvidenceStrength.entries.map { it.name })
            item.keepStringArrayOrRemove("matchedEvidence")
            item.keepStringArrayOrRemove("conflictingEvidence")
            item.keepStringArrayOrRemove("recommendedActions")
            item.keepStringArrayOrRemove("ruleIds")
        }
        normalized.keepStringArrayOrRemove("tags")
        normalized.keepStringMapOrRemove("metadata")
        normalized.objectOrNull("networkSnapshot")?.apply {
            keepKnownEnum("transport", NetworkTransport.entries.map { it.name })
            keepStringArrayOrRemove("localAddresses")
            keepStringArrayOrRemove("gatewayAddresses")
            keepStringArrayOrRemove("dnsServers")
            keepStringMapOrRemove("attributes")
        }
        normalized.objectOrNull("retestComparison")?.apply {
            keepStringOrRemove("baselineSessionId")
            keepStringOrRemove("retestSessionId")
            keepStringOrRemove("summary")
            keepKnownEnum("outcome", RetestOutcome.entries.map { it.name })
            keepArrayOrRemove("probeComparisons") { item ->
                item.keepStringOrRemove("comparisonKey")
                item.keepStringOrRemove("target")
                item.keepStringOrRemove("summary")
                item.keepKnownEnum("probeType", ProbeType.entries.map { it.name })
                item.keepKnownEnum("beforeStatus", ProbeStatus.entries.map { it.name }, allowNull = true)
                item.keepKnownEnum("afterStatus", ProbeStatus.entries.map { it.name }, allowNull = true)
                item.keepKnownEnum("change", ProbeChange.entries.map { it.name })
            }
            keepStringArrayOrRemove("resolvedHypothesisCodes")
            keepStringArrayOrRemove("remainingHypothesisCodes")
            keepStringArrayOrRemove("newHypothesisCodes")
        }

        gson.fromJson(normalized, DiagnosticSession::class.java)
    }.getOrNull()

    private fun JsonObject.keepKnownEnum(
        property: String,
        knownValues: Collection<String>,
        allowNull: Boolean = false,
    ) {
        val value = get(property) ?: return
        if (allowNull && value.isJsonNull) return
        val isKnown = runCatching { value.asString in knownValues }.getOrDefault(false)
        if (!isKnown) remove(property)
    }

    private fun JsonObject.keepArrayOrRemove(
        property: String,
        normalizeItem: ((JsonObject) -> Unit)? = null,
    ) {
        val value = get(property) ?: return
        if (!value.isJsonArray) {
            remove(property)
            return
        }
        if (normalizeItem == null) return

        val cleaned = JsonArray()
        value.asJsonArray.forEach { item ->
            if (item.isJsonObject) {
                val itemCopy = item.asJsonObject.deepCopy()
                normalizeItem(itemCopy)
                cleaned.add(itemCopy)
            }
        }
        add(property, cleaned)
    }

    private fun JsonObject.keepStringOrRemove(property: String) {
        val value = get(property) ?: return
        if (value.isJsonNull || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            remove(property)
        }
    }

    private fun JsonObject.keepStringArrayOrRemove(property: String) {
        val value = get(property) ?: return
        if (!value.isJsonArray) {
            remove(property)
            return
        }
        val cleaned = JsonArray()
        value.asJsonArray.forEach { item ->
            if (!item.isJsonNull && item.isJsonPrimitive && item.asJsonPrimitive.isString) {
                cleaned.add(item.asString)
            }
        }
        add(property, cleaned)
    }

    private fun JsonObject.keepStringMapOrRemove(property: String) {
        val value = get(property) ?: return
        if (!value.isJsonObject) {
            remove(property)
            return
        }
        val cleaned = JsonObject()
        value.asJsonObject.entrySet().forEach { (key, item) ->
            if (!item.isJsonNull && item.isJsonPrimitive) {
                cleaned.addProperty(key, item.asString)
            }
        }
        add(property, cleaned)
    }

    private fun JsonObject.objectOrNull(property: String): JsonObject? {
        val value = get(property) ?: return null
        if (!value.isJsonObject) {
            remove(property)
            return null
        }
        return value.asJsonObject
    }

    private fun JsonObject.stringOrNull(property: String): String? = runCatching {
        get(property)?.takeUnless(JsonElement::isJsonNull)?.asString
    }.getOrNull()

    private data class SessionEnvelope(
        val schemaVersion: Int,
        val sessions: List<DiagnosticSession>,
    )

    companion object {
        const val DEFAULT_MAX_SESSIONS = 20
        private const val PREFERENCES_NAME = "netsage_diagnostic_sessions"
        private const val KEY_SESSIONS = "session_envelope"
        private const val KEY_ENVELOPE_SESSIONS = "sessions"

        private val sessionOrder = compareByDescending<DiagnosticSession> { it.updatedAtEpochMillis }
            .thenByDescending { it.createdAtEpochMillis }
            .thenBy { it.id }
    }
}
