package com.netsage.app.diagnostic.rules

import com.netsage.app.diagnostic.parser.Observation
import com.netsage.app.diagnostic.parser.ObservationKind
import com.netsage.app.diagnostic.parser.ObservationParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRuleEngineTest {
    private val parser = ObservationParser()

    @Test
    fun `loads the versioned asset and returns top three with evidence`() {
        val engine = assetEngine()
        val observations = parser.parse(
            """
            certificate has expired
            query returned NXDOMAIN
            HTTP/1.1 503 Service Unavailable
            packet loss: 20%
            """.trimIndent(),
        )

        val hypotheses = engine.evaluate(observations)

        assertEquals(3, hypotheses.size)
        assertEquals("tls.certificate_expired", hypotheses[0].ruleId)
        assertEquals(DiagnosticPriority.P1, hypotheses[0].priority)
        assertTrue(hypotheses.all { it.positiveEvidence.isNotEmpty() })
        assertTrue(hypotheses.none { it.positiveEvidence === it.conflictEvidence })
    }

    @Test
    fun `reports competing evidence and lowers effective priority`() {
        val engine = assetEngine()
        val observations = listOf(
            observation(ObservationKind.TCP_REFUSED, 1, "connection refused"),
            observation(ObservationKind.TCP_TIMEOUT, 2, "connection timed out"),
        )

        val hypotheses = engine.evaluate(observations)
        val refused = hypotheses.single { it.ruleId == "tcp.connection_refused" }
        val timeout = hypotheses.single { it.ruleId == "tcp.connection_timeout" }

        assertEquals(listOf(ObservationKind.TCP_TIMEOUT), refused.conflictEvidence.map { it.kind })
        assertEquals(DiagnosticPriority.P2, refused.priority)
        assertEquals(listOf(ObservationKind.TCP_REFUSED), timeout.conflictEvidence.map { it.kind })
        assertEquals(DiagnosticPriority.P3, timeout.priority)
    }

    @Test
    fun `returns no hypothesis when observations do not satisfy a rule`() {
        val engine = assetEngine()
        val cluesOnly = listOf(
            observation(ObservationKind.IPV4_CLUE, 1, "192.168.1.10", value = "192.168.1.10"),
            observation(ObservationKind.IPV6_CLUE, 2, "2001:db8::10", value = "2001:db8::10"),
        )

        assertTrue(engine.evaluate(cluesOnly).isEmpty())
    }

    @Test
    fun `uses rule id as deterministic final tie breaker`() {
        val engine = LocalRuleEngine.fromJson(
            """
            {
              "schemaVersion": 1,
              "ruleSetId": "stable-order-test",
              "rules": [
                {"id":"rule.c","title":"C","priority":"P2","order":10,"positiveKinds":["DNS_TIMEOUT"],"explanation":"C"},
                {"id":"rule.a","title":"A","priority":"P2","order":10,"positiveKinds":["DNS_TIMEOUT"],"explanation":"A"},
                {"id":"rule.b","title":"B","priority":"P2","order":10,"positiveKinds":["DNS_TIMEOUT"],"explanation":"B"}
              ]
            }
            """.trimIndent(),
        )
        val observations = listOf(observation(ObservationKind.DNS_TIMEOUT, 1, "DNS timeout"))

        val first = engine.evaluate(observations).map { it.ruleId }
        val second = engine.evaluate(observations).map { it.ruleId }

        assertEquals(listOf("rule.a", "rule.b", "rule.c"), first)
        assertEquals(first, second)
    }

    @Test
    fun `returns no hypotheses for empty input`() {
        assertTrue(assetEngine().evaluate(emptyList()).isEmpty())
    }

    @Test
    fun `maps new local network observations to explicit rules`() {
        val hypotheses = assetEngine().evaluate(
            listOf(
                observation(ObservationKind.DEFAULT_GATEWAY_MISSING, 1, "default gateway missing"),
                observation(ObservationKind.DNS_CONFIGURATION_MISSING, 2, "DNS configuration missing"),
                observation(ObservationKind.INTERMITTENT_CONNECTIVITY, 3, "网络时好时坏"),
            ),
        )

        assertEquals(
            listOf(
                "network.default_gateway_missing",
                "network.dns_configuration_missing",
                "network.intermittent_connectivity",
            ),
            hypotheses.map { it.ruleId },
        )
    }

    private fun assetEngine(): LocalRuleEngine {
        val asset = listOf(
            File("src/main/assets/${RuleDefinitionLoader.DEFAULT_ASSET_NAME}"),
            File("app/src/main/assets/${RuleDefinitionLoader.DEFAULT_ASSET_NAME}"),
        ).firstOrNull(File::isFile)
            ?: error("Missing ${RuleDefinitionLoader.DEFAULT_ASSET_NAME} below ${File(".").absolutePath}")
        return asset.inputStream().use(LocalRuleEngine::fromInputStream)
    }

    private fun observation(
        kind: ObservationKind,
        lineNumber: Int,
        excerpt: String,
        value: String? = null,
    ) = Observation(
        kind = kind,
        sourceExcerpt = excerpt,
        lineNumber = lineNumber,
        value = value,
    )
}
