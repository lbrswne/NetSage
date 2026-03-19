package com.netsage.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netsage.app.ui.screen.InputScreen
import com.netsage.app.ui.screen.ResultScreen
import com.netsage.app.viewmodel.DiagnoseViewModel
import com.netsage.app.viewmodel.ViewModelFactory

@Composable
fun NetSageApp() {
    val state = remember { AppState() }
    val vm: DiagnoseViewModel = viewModel(factory = ViewModelFactory())
    val ui by vm.uiState.collectAsState()

    when (state.page) {
        AppPage.INPUT -> InputScreen(onDiagnose = { text -> vm.diagnose(text) })
        AppPage.RESULT -> ResultScreen(causes = state.causes, onBack = { state.backToInput() })
    }

    if (ui.causes.isNotEmpty() && state.page != AppPage.RESULT) {
        state.showResult(ui.causes)
    }

    ui.error?.let {
        Text("错误: $it")
        vm.clearError()
    }
}
