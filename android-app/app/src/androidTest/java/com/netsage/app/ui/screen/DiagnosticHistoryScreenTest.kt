package com.netsage.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.netsage.app.diagnostic.session.DiagnosticSessionMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticHistoryScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private var clearCalls = 0

    private fun showHistory(count: Int = 2, onShare: (String) -> Unit = {}) {
        composeRule.setContent {
            DiagnosticHistoryScreen(
                items = (1..count).map { index ->
                    DiagnosticHistoryItemUi(
                        id = "$index",
                        createdAt = 0L,
                        mode = "诊断",
                        sessionMode = DiagnosticSessionMode.QUICK_CHECKUP,
                        target = "目标 $index",
                        summary = "摘要 $index",
                        probeSummary = "探测 $index",
                    )
                },
                onBack = {},
                onOpen = {},
                onShare = onShare,
                onDelete = {},
                onClear = { clearCalls++ },
            )
        }
    }

    private fun openConfirmation() {
        composeRule.onNodeWithText("清空全部本地会话").performScrollTo().performClick()
    }

    @Test
    fun clearOpensConfirmationWithSessionCountAndIrreversibleWarning() {
        showHistory()

        openConfirmation()

        composeRule.onNodeWithText("将删除全部 2 条本地会话，删除后无法恢复。").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, clearCalls) }
    }

    @Test
    fun cancelDismissesWithoutClearing() {
        showHistory()
        openConfirmation()

        composeRule.onNodeWithText("取消").performClick()

        composeRule.onNodeWithText("确认清空").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, clearCalls) }
    }

    @Test
    fun confirmClearsExactlyOnceAndDismisses() {
        showHistory()
        openConfirmation()

        composeRule.onNodeWithText("确认清空").performClick()

        composeRule.onNodeWithText("确认清空").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, clearCalls) }
    }

    @Test
    fun searchAndModeFilterCanBeCombinedAndReset() {
        composeRule.setContent {
            DiagnosticHistoryScreen(
                items = listOf(
                    DiagnosticHistoryItemUi("1", 0L, "快速体检", DiagnosticSessionMode.QUICK_CHECKUP, "example.com", "DNS 故障", "探测"),
                    DiagnosticHistoryItemUi("2", 0L, "日志诊断", DiagnosticSessionMode.LOG_ANALYSIS, "本地日志", "连接超时", "探测"),
                    DiagnosticHistoryItemUi("3", 0L, "修复后复测", DiagnosticSessionMode.RETEST, "example.net", "已恢复", "探测"),
                ),
                onBack = {}, onOpen = {}, onShare = {}, onDelete = {}, onClear = {},
            )
        }

        composeRule.onNodeWithText("搜索目标地址或摘要").performTextInput("example")
        composeRule.onNodeWithText("筛选结果 2 条").assertIsDisplayed()
        composeRule.onNodeWithText("日志").performClick()
        composeRule.onNodeWithText("没有符合条件的会话。请调整搜索词或筛选模式。").assertIsDisplayed()
        composeRule.onNodeWithText("全部").performClick()
        composeRule.onNodeWithText("搜索目标地址或摘要").performTextClearance()
        composeRule.onNodeWithText("筛选结果 3 条").assertIsDisplayed()
    }

    @Test
    fun deletingOneSessionRequiresConfirmationAndTargetsSelectedId() {
        val deletedIds = mutableListOf<String>()
        composeRule.setContent {
            DiagnosticHistoryScreen(
                items = listOf(
                    DiagnosticHistoryItemUi("first", 0L, "快速体检", DiagnosticSessionMode.QUICK_CHECKUP, "first.example", "摘要", "探测"),
                ),
                onBack = {}, onOpen = {}, onShare = {}, onDelete = deletedIds::add, onClear = {},
            )
        }

        composeRule.onNodeWithText("删除").performScrollTo().performClick()
        composeRule.onNodeWithText("将删除会话「first.example」，删除后无法恢复。").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList<String>(), deletedIds) }

        composeRule.onNodeWithText("取消").performClick()
        composeRule.runOnIdle { assertEquals(emptyList<String>(), deletedIds) }

        composeRule.onNodeWithText("删除").performScrollTo().performClick()
        composeRule.onNodeWithText("确认删除").performClick()
        composeRule.runOnIdle { assertEquals(listOf("first"), deletedIds) }
    }

    @Test
    fun historyCardOffersDirectReportSharing() {
        val sharedIds = mutableListOf<String>()
        showHistory(count = 2, onShare = sharedIds::add)

        composeRule.onNodeWithText("搜索目标地址或摘要").performTextInput("目标 2")
        composeRule.onNodeWithText("分享报告").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(listOf("2"), sharedIds) }
    }
}
