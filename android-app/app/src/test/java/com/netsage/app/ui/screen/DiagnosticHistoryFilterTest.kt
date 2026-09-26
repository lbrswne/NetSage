package com.netsage.app.ui.screen

import com.netsage.app.diagnostic.session.DiagnosticSessionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticHistoryFilterTest {
    private val items = listOf(
        item("1", DiagnosticSessionMode.QUICK_CHECKUP, "example.com", "DNS failed"),
        item("2", DiagnosticSessionMode.LOG_ANALYSIS, "本地日志", "Timeout on mobile"),
        item("3", DiagnosticSessionMode.COMBINED, "api.example.com", "TLS issue"),
        item("4", DiagnosticSessionMode.RETEST, "example.net", "DNS restored"),
    )

    @Test fun searchMatchesTargetOrSummaryIgnoringCaseAndWhitespace() {
        assertEquals(listOf("1", "3"), filterDiagnosticHistory(items, "  EXAMPLE.COM  ", HistoryModeFilter.ALL).map { it.id })
        assertEquals(listOf("1", "4"), filterDiagnosticHistory(items, "dns", HistoryModeFilter.ALL).map { it.id })
    }

    @Test fun modesKeepCombinedWithCheckupAndWorkWithSearch() {
        assertEquals(listOf("1", "3"), filterDiagnosticHistory(items, "", HistoryModeFilter.CHECKUP).map { it.id })
        assertEquals(listOf("2"), filterDiagnosticHistory(items, "", HistoryModeFilter.LOG).map { it.id })
        assertEquals(listOf("4"), filterDiagnosticHistory(items, "", HistoryModeFilter.RETEST).map { it.id })
        assertEquals(listOf("3"), filterDiagnosticHistory(items, "api", HistoryModeFilter.CHECKUP).map { it.id })
        assertEquals(emptyList<String>(), filterDiagnosticHistory(items, "api", HistoryModeFilter.LOG).map { it.id })
    }

    private fun item(id: String, mode: DiagnosticSessionMode, target: String, summary: String) = DiagnosticHistoryItemUi(
        id = id,
        createdAt = 0L,
        mode = "诊断",
        sessionMode = mode,
        target = target,
        summary = summary,
        probeSummary = "探测",
    )
}
