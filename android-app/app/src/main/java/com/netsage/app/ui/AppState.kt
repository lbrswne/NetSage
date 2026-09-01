package com.netsage.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netsage.app.model.CauseItem

enum class AppPage {
    HOME,
    INPUT,
    RESULT,
    LOCAL_CHECKUP,
    COMBINED_CHECKUP,
    DIAGNOSTIC_RESULT,
    DIAGNOSTIC_HISTORY,
    SCENARIO_LIBRARY,
    HISTORY,
    QUICK_REFERENCE,
    SAMPLE_CENTER,
    CHECKLISTS,
    SAVED_REPORTS,
    APPEARANCE_SETTINGS,
    FEATURE_SHOWCASE,
    TOOLBOX,
    REVIEW_DEMO,
    REVIEW_SHOTS_GUIDE,
    ONE_TAP_CHECKUP,
}

class AppState {
    var page by mutableStateOf(AppPage.HOME)
    var causes by mutableStateOf(emptyList<CauseItem>())
    var draftInput by mutableStateOf("")
    var highlightedScenarioId by mutableStateOf<String?>(null)
    var highlightedChecklistId by mutableStateOf<String?>(null)
    var selectedSavedReportId by mutableStateOf<Long?>(null)
    var favoriteScenarioIds by mutableStateOf(setOf<String>())
    val resultTaskStatuses = mutableStateMapOf<Int, String>()

    fun showInput(prefill: String = draftInput) {
        draftInput = prefill
        page = AppPage.INPUT
    }

    fun showResult(newCauses: List<CauseItem>) {
        if (causes != newCauses) resultTaskStatuses.clear()
        causes = newCauses
        page = AppPage.RESULT
    }

    fun showScenarioLibrary(highlightedId: String? = null) {
        highlightedScenarioId = highlightedId
        page = AppPage.SCENARIO_LIBRARY
    }

    fun showChecklists(highlightedId: String? = null) {
        highlightedChecklistId = highlightedId
        page = AppPage.CHECKLISTS
    }

    fun showSavedReports(selectedId: Long? = null) {
        selectedSavedReportId = selectedId
        page = AppPage.SAVED_REPORTS
    }

    fun showHome() {
        page = AppPage.HOME
    }
}
