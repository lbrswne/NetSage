package com.netsage.app.model

enum class FaultCategory(val label: String) {
    DNS("DNS"),
    CONNECTION("连接"),
    TLS("TLS"),
    HTTP("HTTP"),
    PACKET_LOSS("丢包")
}

data class FaultScenario(
    val id: String,
    val category: FaultCategory,
    val title: String,
    val symptoms: String,
    val checks: List<String>,
    val fixHints: List<String>
)

enum class QuickRefCategory(val label: String) {
    DNS("DNS"),
    HTTP("HTTP"),
    TLS("TLS"),
    CONNECTION("连接")
}

data class QuickRefItem(
    val term: String,
    val explanation: String,
    val tips: String,
    val category: QuickRefCategory
)

data class DiagnoseHistoryRecord(
    val timestamp: Long,
    val inputSummary: String,
    val resultSummary: String,
    val inputText: String = ""
)

data class SampleLogItem(
    val id: String,
    val title: String,
    val category: FaultCategory,
    val content: String,
    val hint: String
)

data class TroubleshootingChecklist(
    val id: String,
    val title: String,
    val category: FaultCategory,
    val steps: List<String>,
    val notes: String
)

data class SavedReportItem(
    val id: Long,
    val title: String,
    val summary: String,
    val createdAt: Long
)
