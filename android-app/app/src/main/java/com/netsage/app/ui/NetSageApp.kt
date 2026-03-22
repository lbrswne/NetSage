package com.netsage.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netsage.app.model.DiagnoseHistoryRecord
import com.netsage.app.repo.OfflineKnowledgeRepository
import com.netsage.app.ui.component.PrivacyConsentDialog
import com.netsage.app.ui.screen.ChecklistScreen
import com.netsage.app.ui.screen.HistoryScreen
import com.netsage.app.ui.screen.HomeModule
import com.netsage.app.ui.screen.HomeScreen
import com.netsage.app.ui.screen.InputScreen
import com.netsage.app.ui.screen.PrivacyDocType
import com.netsage.app.ui.screen.PrivacyDocumentScreen
import com.netsage.app.ui.screen.QuickReferenceScreen
import com.netsage.app.ui.screen.ResultScreen
import com.netsage.app.ui.screen.SampleCenterScreen
import com.netsage.app.ui.screen.SavedReportsScreen
import com.netsage.app.ui.screen.ScenarioLibraryScreen
import com.netsage.app.util.DiagnoseHistoryStore
import com.netsage.app.model.SavedReportItem
import com.netsage.app.util.PrivacyPrefs
import com.netsage.app.util.SavedReportStore
import com.netsage.app.util.buildReport
import com.netsage.app.viewmodel.DiagnoseViewModel
import com.netsage.app.viewmodel.ViewModelFactory

@Composable
fun NetSageApp() {
    val state = remember { AppState() }
    val context = LocalContext.current
    val activity = context as? Activity
    val vm: DiagnoseViewModel = viewModel(factory = ViewModelFactory())
    val ui by vm.uiState.collectAsState()
    val privacyAccepted = PrivacyPrefs.hasAgreed(context)
    var hasAgreedPrivacy by remember(privacyAccepted) { mutableStateOf(privacyAccepted) }
    var currentDoc by remember { mutableStateOf<PrivacyDocType?>(null) }
    var history by remember { mutableStateOf(DiagnoseHistoryStore.load(context)) }
    var savedReports by remember { mutableStateOf(SavedReportStore.load(context)) }

    if (!hasAgreedPrivacy) {
        PrivacyConsentDialog(
            onOpenUserAgreement = { currentDoc = PrivacyDocType.USER_AGREEMENT },
            onOpenPrivacyPolicy = { currentDoc = PrivacyDocType.PRIVACY_POLICY },
            onAgree = {
                PrivacyPrefs.setAgreed(context, true)
                hasAgreedPrivacy = true
            },
            onReject = {
                PrivacyPrefs.reset(context)
                hasAgreedPrivacy = false
                currentDoc = null
                activity?.finishAffinity()
            }
        )
    }

    val modules = listOf(
        HomeModule("日志诊断", "保留原有核心：粘贴日志得出根因") { state.showInput() },
        HomeModule("故障场景库", "离线按 DNS/连接/TLS/HTTP/丢包分类") { state.showScenarioLibrary() },
        HomeModule("样例中心", "一键使用内置样例日志进行诊断") { state.page = AppPage.SAMPLE_CENTER },
        HomeModule("排障清单", "按步骤完成常见网络问题排查") { state.showChecklists() },
        HomeModule("诊断历史", "本地持久化保存输入和结果摘要") { state.page = AppPage.HISTORY },
        HomeModule("错误码速查", "离线术语/错误码快速查询") { state.page = AppPage.QUICK_REFERENCE },
        HomeModule("收藏诊断", "查看已收藏的诊断结果") { state.showSavedReports() }
    )

    when {
        currentDoc != null -> PrivacyDocumentScreen(
            type = currentDoc!!,
            onBack = { currentDoc = null }
        )

        state.page == AppPage.HOME -> HomeScreen(
            modules = modules,
            latestRecordSummary = history.firstOrNull()?.let { "${it.inputSummary} → ${it.resultSummary}" },
            savedReportCount = savedReports.size,
            onQuickOpenInput = { state.showInput() },
            onQuickOpenHistory = { state.page = AppPage.HISTORY },
            onQuickOpenReference = { state.page = AppPage.QUICK_REFERENCE },
            onReuseLatestRecord = {
                history.firstOrNull()?.let { record ->
                    state.showInput(record.inputText.ifBlank { record.inputSummary })
                } ?: run {
                    state.showInput()
                }
            },
            onOpenHistory = { state.page = AppPage.HISTORY },
            onOpenUserAgreement = { currentDoc = PrivacyDocType.USER_AGREEMENT },
            onOpenPrivacyPolicy = { currentDoc = PrivacyDocType.PRIVACY_POLICY },
        )

        state.page == AppPage.INPUT -> InputScreen(
            onDiagnose = { text ->
                state.draftInput = text
                vm.diagnose(text)
            },
            initialText = state.draftInput,
            onOpenUserAgreement = { currentDoc = PrivacyDocType.USER_AGREEMENT },
            onOpenPrivacyPolicy = { currentDoc = PrivacyDocType.PRIVACY_POLICY },
            onBackHome = { state.showHome() }
        )

        state.page == AppPage.RESULT -> ResultScreen(
            causes = state.causes,
            recommendedChecklists = OfflineKnowledgeRepository.checklists.filter { checklist ->
                state.causes.any { cause ->
                    checklist.title.contains(cause.name.take(6), ignoreCase = true) ||
                        checklist.notes.contains(cause.name.take(6), ignoreCase = true)
                }
            },
            recommendedScenarios = OfflineKnowledgeRepository.scenarios.filter { scenario ->
                state.causes.any { cause ->
                    scenario.title.contains(cause.name.take(6), ignoreCase = true) ||
                        scenario.symptoms.contains(cause.name.take(6), ignoreCase = true)
                }
            },
            onOpenChecklists = { highlightedId -> state.showChecklists(highlightedId) },
            onOpenScenarios = { highlightedId -> state.showScenarioLibrary(highlightedId) },
            onSaveReport = {
                val top = state.causes.firstOrNull()
                if (top != null) {
                    SavedReportStore.add(
                        context,
                        SavedReportItem(
                            id = System.currentTimeMillis(),
                            title = top.name,
                            summary = top.fix,
                            createdAt = System.currentTimeMillis(),
                            inputText = state.draftInput
                        )
                    )
                    savedReports = SavedReportStore.load(context)
                    Toast.makeText(context, "已收藏本次诊断", Toast.LENGTH_SHORT).show()
                }
            },
            onBack = {
                state.showHome()
                vm.consumeCauses()
            },
            onCopyReport = {
                val report = buildReport(state.causes)
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-report", report))
                Toast.makeText(context, "报告已复制", Toast.LENGTH_SHORT).show()
            }
        )

        state.page == AppPage.SCENARIO_LIBRARY -> ScenarioLibraryScreen(
            scenarios = OfflineKnowledgeRepository.scenarios,
            highlightedId = state.highlightedScenarioId,
            onBack = { state.showHome() }
        )

        state.page == AppPage.SAMPLE_CENTER -> SampleCenterScreen(
            samples = OfflineKnowledgeRepository.sampleLogs,
            onUseSample = { sample ->
                state.showInput(sample)
            },
            onBack = { state.showHome() }
        )

        state.page == AppPage.CHECKLISTS -> ChecklistScreen(
            items = OfflineKnowledgeRepository.checklists,
            highlightedId = state.highlightedChecklistId,
            onBack = { state.showHome() }
        )

        state.page == AppPage.HISTORY -> HistoryScreen(
            records = history,
            onReuse = { record ->
                state.showInput(record.inputText.ifBlank { record.inputSummary })
            },
            onClear = {
                DiagnoseHistoryStore.clear(context)
                history = emptyList()
            },
            onBack = { state.showHome() }
        )

        state.page == AppPage.QUICK_REFERENCE -> QuickReferenceScreen(
            items = OfflineKnowledgeRepository.quickRefs,
            onBack = { state.showHome() }
        )

        state.page == AppPage.SAVED_REPORTS -> SavedReportsScreen(
            items = savedReports,
            selectedId = state.selectedSavedReportId,
            onReuse = { item ->
                state.showInput(item.inputText.ifBlank { item.summary })
            },
            onDelete = { item ->
                SavedReportStore.remove(context, item.id)
                savedReports = SavedReportStore.load(context)
                Toast.makeText(context, "已删除收藏", Toast.LENGTH_SHORT).show()
            },
            onBack = { state.showHome() }
        )
    }

    LaunchedEffect(ui.causes) {
        if (ui.causes.isNotEmpty()) {
            state.showResult(ui.causes)

            val resultSummary = ui.causes.joinToString(" | ") { "${it.name}(${(it.confidence * 100).toInt()}%)" }
            val record = DiagnoseHistoryRecord(
                timestamp = System.currentTimeMillis(),
                inputSummary = ui.inputSummary.ifBlank { "(空)" },
                resultSummary = resultSummary,
                inputText = state.draftInput
            )
            DiagnoseHistoryStore.add(context, record)
            history = DiagnoseHistoryStore.load(context)
        }
    }

    ui.error?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        vm.clearError()
    }
}
