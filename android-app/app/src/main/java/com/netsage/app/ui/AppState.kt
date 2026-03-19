package com.netsage.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netsage.app.model.CauseItem

enum class AppPage { INPUT, RESULT }

class AppState {
    var page by mutableStateOf(AppPage.INPUT)
    var causes by mutableStateOf(emptyList<CauseItem>())

    fun showResult(newCauses: List<CauseItem>) {
        causes = newCauses
        page = AppPage.RESULT
    }

    fun backToInput() {
        page = AppPage.INPUT
    }
}
