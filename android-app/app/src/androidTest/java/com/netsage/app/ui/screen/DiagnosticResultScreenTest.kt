package com.netsage.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticResultScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun deletingResultRequiresConfirmationAndCancelKeepsSession() {
        var deleteCalls = 0
        composeRule.setContent {
            DiagnosticResultScreen(
                session = DiagnosticSessionUi(
                    id = "session-1",
                    createdAt = 0L,
                    mode = "快速体检",
                    target = "result.example",
                    networkLines = emptyList(),
                    probes = emptyList(),
                    hypotheses = emptyList(),
                ),
                isRetesting = false,
                retestProgress = "",
                onBack = {}, onRetest = {}, onExportMarkdown = {}, onExportJson = {}, onShare = {},
                onDelete = { deleteCalls++ },
            )
        }

        composeRule.onNodeWithText("删除本地会话").performScrollTo().performClick()
        composeRule.onNodeWithText("将删除会话「result.example」，删除后无法恢复。").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, deleteCalls) }

        composeRule.onNodeWithText("取消").performClick()
        composeRule.runOnIdle { assertEquals(0, deleteCalls) }

        composeRule.onNodeWithText("删除本地会话").performScrollTo().performClick()
        composeRule.onNodeWithText("确认删除").performClick()
        composeRule.runOnIdle { assertEquals(1, deleteCalls) }
    }

    @Test
    fun resultSharingRequiresPrivacyConfirmation() {
        var shareCalls = 0
        composeRule.setContent {
            DiagnosticResultScreen(
                session = DiagnosticSessionUi(
                    id = "session-2",
                    createdAt = 0L,
                    mode = "日志诊断",
                    target = "本地日志",
                    networkLines = emptyList(),
                    probes = emptyList(),
                    hypotheses = emptyList(),
                ),
                isRetesting = false,
                retestProgress = "",
                onBack = {}, onRetest = {}, onExportMarkdown = {}, onExportJson = {},
                onShare = { shareCalls++ }, onDelete = {},
            )
        }

        composeRule.onNodeWithText("通过系统分享").performScrollTo().performClick()
        composeRule.onNodeWithText("分享完整诊断报告？").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, shareCalls) }

        composeRule.onNodeWithText("取消").performClick()
        composeRule.runOnIdle { assertEquals(0, shareCalls) }

        composeRule.onNodeWithText("通过系统分享").performScrollTo().performClick()
        composeRule.onNodeWithText("继续分享").performClick()
        composeRule.runOnIdle { assertEquals(1, shareCalls) }
    }
}
