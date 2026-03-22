package com.netsage.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netsage.app.model.CauseItem

enum class AppPage { HOME, INPUT, RESULT, SCENARIO_LIBRARY, HISTORY, QUICK_REFERENCE, SAMPLE_CENTER, CHECKLISTS, SAVED_REPORTS }

class AppState {
    var page by mutableStateOf(AppPage.HOME)
    var causes by mutableStateOf(emptyList<CauseItem>())
    var draftInput by mutableStateOf("")

    fun showInput(prefill: String = draftInput) {
        draftInput = prefill
        page = AppPage.INPUT
    }

    fun showResult(newCauses: List<CauseItem>) {
        causes = newCauses
        page = AppPage.RESULT
    }

    fun showHome() {
        page = AppPage.HOME
    }
}
