package com.netsage.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netsage.app.ui.screen.InputScreen
import com.netsage.app.ui.screen.ResultScreen
import com.netsage.app.util.buildReport
import com.netsage.app.viewmodel.DiagnoseViewModel
import com.netsage.app.viewmodel.ViewModelFactory

@Composable
fun NetSageApp() {
    val state = remember { AppState() }
    val context = LocalContext.current
    val vm: DiagnoseViewModel = viewModel(factory = ViewModelFactory())
    val ui by vm.uiState.collectAsState()

    when (state.page) {
        AppPage.INPUT -> InputScreen(onDiagnose = { text -> vm.diagnose(text) })
        AppPage.RESULT -> ResultScreen(
            causes = state.causes,
            onBack = { state.backToInput() },
            onCopyReport = {
                val report = buildReport(state.causes)
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-report", report))
                Toast.makeText(context, "报告已复制", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (ui.causes.isNotEmpty() && state.page != AppPage.RESULT) {
        state.showResult(ui.causes)
    }

    ui.error?.let {
        Text("错误: $it")
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        vm.clearError()
    }
}
