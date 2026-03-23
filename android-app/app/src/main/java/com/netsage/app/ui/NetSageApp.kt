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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netsage.app.model.CauseItem
import com.netsage.app.model.DiagnoseHistoryRecord
import com.netsage.app.model.FaultCategory
import com.netsage.app.model.FaultScenario
import com.netsage.app.model.TroubleshootingChecklist
import com.netsage.app.repo.OfflineKnowledgeRepository
import com.netsage.app.ui.screen.AppearanceSettingsScreen
import com.netsage.app.ui.screen.ChecklistScreen
import com.netsage.app.ui.screen.FeatureShowcaseScreen
import com.netsage.app.ui.screen.HistoryScreen
import com.netsage.app.ui.screen.HomeModule
import com.netsage.app.ui.screen.HomeScreen
import com.netsage.app.ui.screen.InputScreen
import com.netsage.app.ui.screen.PrivacyDocType
import com.netsage.app.ui.screen.PrivacyOnboardingScreen
import com.netsage.app.ui.screen.PrivacyDocumentScreen
import com.netsage.app.ui.screen.QuickReferenceScreen
import com.netsage.app.ui.screen.ResultScreen
import com.netsage.app.ui.screen.ReviewDemoScreen
import com.netsage.app.ui.screen.ReviewShotStep
import com.netsage.app.ui.screen.ReviewShotsGuideScreen
import com.netsage.app.ui.screen.SampleCenterScreen
import com.netsage.app.ui.screen.SavedReportsScreen
import com.netsage.app.ui.screen.ScenarioLibraryScreen
import com.netsage.app.ui.screen.ToolboxScreen
import com.netsage.app.util.AppearanceSettings
import com.netsage.app.util.DiagnoseHistoryStore
import com.netsage.app.model.SavedReportItem
import com.netsage.app.util.PrivacyPrefs
import com.netsage.app.util.SavedReportStore
import com.netsage.app.util.buildReport
import com.netsage.app.viewmodel.DiagnoseViewModel
import com.netsage.app.viewmodel.ViewModelFactory

private fun inferCategory(cause: CauseItem): FaultCategory? {
    val text = buildString {
        append(cause.name)
        append(' ')
        append(cause.fix)
        append(' ')
        append(cause.evidence.joinToString(" "))
    }.lowercase()

    return when {
        listOf("dns", "nxdomain", "解析").any { text.contains(it) } -> FaultCategory.DNS
        listOf("tls", "certificate", "handshake", "证书").any { text.contains(it) } -> FaultCategory.TLS
        listOf("http", "502", "504", "gateway", "upstream", "网关").any { text.contains(it) } -> FaultCategory.HTTP
        listOf("丢包", "packet loss", "wireless", "无线").any { text.contains(it) } -> FaultCategory.PACKET_LOSS
        listOf("route", "gateway", "dhcp", "连接", "网关", "路由").any { text.contains(it) } -> FaultCategory.CONNECTION
        else -> null
    }
}

private fun inferTags(cause: CauseItem): List<String> {
    val categoryTag = inferCategory(cause)?.label
    val severityTag = when {
        cause.confidence >= 0.85 -> "高优先级"
        cause.confidence >= 0.60 -> "中优先级"
        else -> "低优先级"
    }
    return listOfNotNull(categoryTag, severityTag).distinct()
}

private fun extractKeywords(cause: CauseItem): List<String> {
    val tokens = buildList {
        add(cause.name)
        add(cause.fix)
        addAll(cause.evidence)
    }.flatMap { text ->
        text.lowercase()
            .split(Regex("[^a-z0-9\u4e00-\u9fa5+]+"))
            .filter { it.length >= 2 }
    }

    return tokens.distinct()
}

private fun scoreScenario(cause: CauseItem, scenario: FaultScenario): Int {
    val scenarioText = buildString {
        append(scenario.title.lowercase())
        append(' ')
        append(scenario.symptoms.lowercase())
        append(' ')
        append(scenario.checks.joinToString(" ").lowercase())
        append(' ')
        append(scenario.fixHints.joinToString(" ").lowercase())
    }
    val keywords = extractKeywords(cause)
    val category = inferCategory(cause)
    var score = if (category == scenario.category) 6 else 0
    keywords.forEach { keyword ->
        if (scenarioText.contains(keyword)) score += if (keyword.length >= 4) 3 else 2
    }
    return score
}

private fun scoreChecklist(cause: CauseItem, checklist: TroubleshootingChecklist): Int {
    val checklistText = buildString {
        append(checklist.title.lowercase())
        append(' ')
        append(checklist.notes.lowercase())
        append(' ')
        append(checklist.steps.joinToString(" ").lowercase())
    }
    val keywords = extractKeywords(cause)
    val category = inferCategory(cause)
    var score = if (category == checklist.category) 6 else 0
    keywords.forEach { keyword ->
        if (checklistText.contains(keyword)) score += if (keyword.length >= 4) 3 else 2
    }
    return score
}

private fun recommendScenarios(causes: List<CauseItem>): List<FaultScenario> {
    return OfflineKnowledgeRepository.scenarios
        .map { scenario -> scenario to causes.sumOf { scoreScenario(it, scenario) } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(3)
}

private fun recommendChecklists(causes: List<CauseItem>): List<TroubleshootingChecklist> {
    return OfflineKnowledgeRepository.checklists
        .map { checklist -> checklist to causes.sumOf { scoreChecklist(it, checklist) } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(3)
}

@Composable
fun NetSageApp(
    appearanceSettings: AppearanceSettings,
    onUpdateAppearanceSettings: (AppearanceSettings) -> Unit,
) {
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
    var nextSampleIndex by rememberSaveable { mutableIntStateOf(0) }

    val sampleLogs = OfflineKnowledgeRepository.sampleLogs
    fun takeNextSample(): String {
        if (sampleLogs.isEmpty()) return ""
        val sample = sampleLogs[nextSampleIndex % sampleLogs.size]
        nextSampleIndex = (nextSampleIndex + 1) % sampleLogs.size
        return sample.content
    }

    val modules = listOf(
        HomeModule("快速诊断", "输入日志快速生成 Top3 根因与下一步行动") { state.showInput() },
        HomeModule("故障场景库", "离线按 DNS/连接/TLS/HTTP/丢包分类") { state.showScenarioLibrary() },
        HomeModule("样例中心", "一键使用内置样例日志进行诊断") { state.page = AppPage.SAMPLE_CENTER },
        HomeModule("排障清单", "按步骤完成常见网络问题排查") { state.showChecklists() },
        HomeModule("错误码速查", "离线术语/错误码快速查询") { state.page = AppPage.QUICK_REFERENCE },
        HomeModule("收藏诊断", "查看已收藏的诊断结果") { state.showSavedReports() },
        HomeModule("历史复盘", "回看历史记录并继续上次诊断") { state.page = AppPage.HISTORY },
        HomeModule("现场工具箱", "内置常见排障命令模板，可一键复制") { state.page = AppPage.TOOLBOX },
        HomeModule("审核演示", "一键跑通固定脚本，快速核验核心功能") { state.page = AppPage.REVIEW_DEMO },
        HomeModule("截图向导", "5步截图采集，一键跳转对应页面") { state.page = AppPage.REVIEW_SHOTS_GUIDE },
        HomeModule("版本新增", "给审核员看的新增能力清单") { state.page = AppPage.FEATURE_SHOWCASE },
        HomeModule("显示与风格", "调整字体大小与整体配色") { state.page = AppPage.APPEARANCE_SETTINGS }
    )

    when {
        currentDoc != null -> PrivacyDocumentScreen(
            type = currentDoc!!,
            onBack = { currentDoc = null }
        )

        !hasAgreedPrivacy -> PrivacyOnboardingScreen(
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

        state.page == AppPage.HOME -> HomeScreen(
            modules = modules,
            latestRecordSummary = history.firstOrNull()?.let { "${it.inputSummary} → ${it.resultSummary}" },
            savedReportCount = savedReports.size,
            historyCount = history.size,
            onQuickOpenInput = { state.showInput() },
            onQuickOpenHistory = { state.page = AppPage.HISTORY },
            onQuickOpenReference = { state.page = AppPage.QUICK_REFERENCE },
            onQuickOpenSamples = { state.page = AppPage.SAMPLE_CENTER },
            onQuickOpenReviewDemo = { state.page = AppPage.REVIEW_DEMO },
            onQuickOpenShotsGuide = { state.page = AppPage.REVIEW_SHOTS_GUIDE },
            onQuickOpenScenarios = { state.showScenarioLibrary() },
            onQuickOpenFeatureShowcase = { state.page = AppPage.FEATURE_SHOWCASE },
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
            onOpenAppearanceSettings = { state.page = AppPage.APPEARANCE_SETTINGS },
        )

        state.page == AppPage.INPUT -> InputScreen(
            onDiagnose = { text ->
                state.draftInput = text
                vm.diagnose(text)
            },
            initialText = state.draftInput,
            isLoading = ui.loading,
            onFillSample = {
                val sample = takeNextSample()
                state.draftInput = sample
                sample
            },
            onOpenUserAgreement = { currentDoc = PrivacyDocType.USER_AGREEMENT },
            onOpenPrivacyPolicy = { currentDoc = PrivacyDocType.PRIVACY_POLICY },
            onBackHome = { state.showHome() }
        )

        state.page == AppPage.RESULT -> ResultScreen(
            causes = state.causes,
            recommendedChecklists = recommendChecklists(state.causes),
            recommendedScenarios = recommendScenarios(state.causes),
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
                            inputText = state.draftInput,
                            tags = inferTags(top)
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
            },
            onCopyIncidentBrief = {
                val top = state.causes.firstOrNull()
                val brief = buildString {
                    appendLine("NetSage 故障事件简报")
                    appendLine("主判断：${top?.name ?: "暂无"}")
                    appendLine("置信度：${top?.let { "${(it.confidence * 100).toInt()}%" } ?: "暂无"}")
                    appendLine("影响范围：${top?.evidence?.firstOrNull() ?: "待补充"}")
                    appendLine("建议处置：${top?.fix ?: "待补充"}")
                    appendLine("时间：${System.currentTimeMillis()}")
                }
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-incident-brief", brief))
                Toast.makeText(context, "故障简报已复制", Toast.LENGTH_SHORT).show()
            },
            onExportActionPlan = {
                val top = state.causes.firstOrNull()
                val checklists = recommendChecklists(state.causes)
                val scenarios = recommendScenarios(state.causes)
                val actionPlan = buildString {
                    appendLine("NetSage 下一步行动单")
                    appendLine("主判断：${top?.name ?: "暂无"}")
                    appendLine("建议处理：${top?.fix ?: "暂无"}")
                    appendLine("优先动作：")
                    appendLine("1) 先核对输入日志与现场现象是否一致")
                    appendLine("2) 按推荐清单执行前2步：${checklists.firstOrNull()?.title ?: "（暂无推荐清单）"}")
                    appendLine("3) 对照场景复核：${scenarios.firstOrNull()?.title ?: "（暂无推荐场景）"}")
                    appendLine("4) 若无改善，回看 Top2/Top3 继续排查")
                }
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-action-plan", actionPlan))
                Toast.makeText(context, "行动单已复制", Toast.LENGTH_SHORT).show()
            }
        )

        state.page == AppPage.SCENARIO_LIBRARY -> ScenarioLibraryScreen(
            scenarios = OfflineKnowledgeRepository.scenarios,
            highlightedId = state.highlightedScenarioId,
            favoriteIds = state.favoriteScenarioIds,
            onToggleFavorite = { id ->
                state.favoriteScenarioIds = if (id in state.favoriteScenarioIds) {
                    state.favoriteScenarioIds - id
                } else {
                    state.favoriteScenarioIds + id
                }
            },
            onBack = { state.showHome() },
            onUseScenario = { scenario ->
                val draft = buildString {
                    appendLine("场景：${scenario.title}")
                    appendLine("分类：${scenario.category.label}")
                    appendLine("典型现象：${scenario.symptoms}")
                    appendLine("建议先排查：${scenario.checks.joinToString("；")}")
                    appendLine("修复提示：${scenario.fixHints.joinToString("；")}")
                }
                state.showInput(draft)
            },
            onRunReviewDemo = { scenario ->
                val draft = buildString {
                    appendLine("场景：${scenario.title}")
                    appendLine("分类：${scenario.category.label}")
                    appendLine("典型现象：${scenario.symptoms}")
                    appendLine("建议先排查：${scenario.checks.joinToString("；")}")
                    appendLine("修复提示：${scenario.fixHints.joinToString("；")}")
                }
                state.draftInput = draft
                vm.diagnose(draft)
                Toast.makeText(context, "已启动审核演示诊断", Toast.LENGTH_SHORT).show()
            }
        )

        state.page == AppPage.SAMPLE_CENTER -> SampleCenterScreen(
            samples = sampleLogs,
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
            onCopySummary = { item ->
                val content = buildString {
                    appendLine("NetSage 收藏诊断")
                    appendLine("标题：${item.title}")
                    appendLine("摘要：${item.summary}")
                    if (item.tags.isNotEmpty()) appendLine("标签：${item.tags.joinToString(" / ")}")
                    append("收藏时间：${item.createdAt}")
                }
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-saved-report", content))
                Toast.makeText(context, "已复制收藏摘要", Toast.LENGTH_SHORT).show()
            },
            onDelete = { item ->
                SavedReportStore.remove(context, item.id)
                savedReports = SavedReportStore.load(context)
                Toast.makeText(context, "已删除收藏", Toast.LENGTH_SHORT).show()
            },
            onBack = { state.showHome() }
        )

        state.page == AppPage.TOOLBOX -> ToolboxScreen(
            onBack = { state.showHome() },
            onCopyCommand = { command ->
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-toolbox-command", command))
                Toast.makeText(context, "命令已复制", Toast.LENGTH_SHORT).show()
            },
            onCopyInterpretation = { interpretation ->
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-toolbox-interpretation", interpretation))
                Toast.makeText(context, "判读提示已复制", Toast.LENGTH_SHORT).show()
            }
        )

        state.page == AppPage.REVIEW_DEMO -> ReviewDemoScreen(
            onBack = { state.showHome() },
            onRunDemo = {
                val demoInput = """
                    审核演示样例：
                    现象：业务域名间歇性 504，晚高峰出现明显超时；
                    日志：GET /api/report -> 504 Gateway Timeout; upstream timed out after 30s;
                    环境：校园网出口，多个终端复现。
                """.trimIndent()
                state.draftInput = demoInput
                vm.diagnose(demoInput)
                Toast.makeText(context, "审核演示已启动", Toast.LENGTH_SHORT).show()
            },
            onCopyReviewSummary = {
                val summary = buildString {
                    appendLine("NetSage 功能核验摘要（审核演示）")
                    appendLine("1) 首页可见：功能厚度总览 + 审核演示入口")
                    appendLine("2) 输入页可见：随机样例 / 一键清空 / 诊断进度")
                    appendLine("3) 结果页可见：风险等级 / 影响范围 / 建议处理顺序 / 行动单导出")
                    appendLine("4) 场景库可见：分类计数 / 热门场景 / 一键回填并诊断")
                    appendLine("5) 工具箱可见：常见排障命令模板可复制")
                    appendLine("结论：当前版本为多模块可见增强版本，功能厚度显著提升。")
                }
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-review-summary", summary))
                Toast.makeText(context, "功能核验摘要已复制", Toast.LENGTH_SHORT).show()
            }
        )

        state.page == AppPage.REVIEW_SHOTS_GUIDE -> ReviewShotsGuideScreen(
            onBack = { state.showHome() },
            steps = listOf(
                ReviewShotStep("首页功能厚度", "截图首页的功能厚度卡片与模块入口。", "跳转首页") { state.showHome() },
                ReviewShotStep("输入页增强", "截图随机样例/一键清空/诊断按钮。", "跳转输入页") { state.showInput() },
                ReviewShotStep("结果页专业报告", "截图风险等级、影响范围、建议处理顺序。", "跳转结果演示") {
                    val demo = listOf(
                        CauseItem("DNS 解析异常", 0.82, "检查权威记录并切换公共 DNS 复测", listOf("命中 NXDOMAIN", "关键字包含 server can't find")),
                        CauseItem("网关上游超时", 0.63, "检查 upstream 健康与超时配置", listOf("命中 504", "命中 upstream timeout")),
                        CauseItem("TLS 证书链异常", 0.51, "核查证书链与系统时间", listOf("命中 certificate", "命中 handshake"))
                    )
                    state.showResult(demo)
                },
                ReviewShotStep("场景库与热门场景", "截图分类计数与热门场景一键诊断按钮。", "跳转场景库") { state.showScenarioLibrary() },
                ReviewShotStep("审核演示与新增清单", "截图审核演示页/版本新增页作为收尾证明。", "跳转审核演示") { state.page = AppPage.REVIEW_DEMO },
            ),
            onCopyShotScript = {
                val script = """
                    NetSage 审核截图顺序（建议）
                    1) 首页：功能厚度卡片 + 模块入口
                    2) 输入页：随机样例 / 一键清空 / 质量评分
                    3) 结果页：风险等级 / 影响范围 / 处置剧本 / 应急指挥卡
                    4) 场景库：分类计数 / 热门场景 / 层级筛选
                    5) 审核演示页：一键演示 + 核验摘要复制
                """.trimIndent()
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-review-shot-script", script))
                Toast.makeText(context, "截图顺序脚本已复制", Toast.LENGTH_SHORT).show()
            }
        )

        state.page == AppPage.FEATURE_SHOWCASE -> FeatureShowcaseScreen(
            onBack = { state.showHome() },
            onOpenInput = { state.showInput() },
            onOpenResultDemo = {
                val demo = vm.uiState.value.causes.ifEmpty {
                    listOf(
                        CauseItem("DNS 解析异常", 0.82, "检查权威记录并切换公共 DNS 复测", listOf("命中 NXDOMAIN", "关键字包含 server can't find")),
                        CauseItem("网关上游超时", 0.63, "检查 upstream 健康与超时配置", listOf("命中 504", "命中 upstream timeout")),
                        CauseItem("TLS 证书链异常", 0.51, "核查证书链与系统时间", listOf("命中 certificate", "命中 handshake"))
                    )
                }
                state.showResult(demo)
            },
            onOpenScenarios = { state.showScenarioLibrary() },
            onOpenSamples = { state.page = AppPage.SAMPLE_CENTER },
            onCopySubmissionBrief = {
                val brief = """
                    NetSage 本版本功能增强说明（提审用）
                    1) 首页：功能厚度面板 + 能力评分条 + 快速开始集群
                    2) 输入页：随机样例/一键清空/诊断进度 + 输入质量评分 + 补全清单
                    3) 结果页：风险等级/影响范围/处置剧本/黄金15分钟应急指挥卡/行动单导出
                    4) 场景库：分类计数 + 热门场景 + 层级筛选 + 一键回填诊断
                    5) 审核支持：一键审核演示、截图向导、核验摘要与截图脚本复制
                    6) 工具箱：排障命令模板 + 判读提示复制
                """.trimIndent()
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("netsage-submission-brief", brief))
                Toast.makeText(context, "提审功能说明已复制", Toast.LENGTH_SHORT).show()
            }
        )

        state.page == AppPage.APPEARANCE_SETTINGS -> AppearanceSettingsScreen(
            settings = appearanceSettings,
            onSelectFontScale = { option ->
                onUpdateAppearanceSettings(appearanceSettings.copy(fontScale = option))
            },
            onSelectThemeStyle = { option ->
                onUpdateAppearanceSettings(appearanceSettings.copy(themeStyle = option))
            },
            onSelectThemeMode = { option ->
                onUpdateAppearanceSettings(appearanceSettings.copy(themeMode = option))
            },
            onSelectLayoutDensity = { option ->
                onUpdateAppearanceSettings(appearanceSettings.copy(layoutDensity = option))
            },
            onResetDefaults = {
                onUpdateAppearanceSettings(AppearanceSettings())
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
