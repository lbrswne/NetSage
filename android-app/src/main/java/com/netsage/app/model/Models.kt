package com.netsage.app.model

data class DiagnoseRequest(val log_text: String)

data class CauseItem(
    val name: String,
    val confidence: Double,
    val fix: String,
    val evidence: List<String> = emptyList()
)

data class DiagnoseResponse(val top_causes: List<CauseItem>)
