package com.maswadkar.lambandtiger

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FeedbackLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @org.junit.Before fun selectTestAudience() {
        rule.runOnIdle { rule.activity.audience.choose(com.maswadkar.lambandtiger.privacy.AgeGroup.UNDER_13) }
    }
    private val vm get() = rule.activity.model

    @Test fun portraitFeedbackDoesNotMoveBoardOrActions() = checkFeedback(false)

    @Test fun landscapeFeedbackDoesNotMoveBoardOrActions() = checkFeedback(true)

    private fun checkFeedback(landscape: Boolean) {
        rule.runOnIdle {
            rule.activity.requestedOrientation = if (landscape)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        rule.waitUntil(10000) {
            rule.activity.resources.configuration.orientation == if (landscape)
                Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        }
        rule.waitUntil(10000) { !vm.ui.value.busy }
        rule.runOnIdle {
            vm.home()
            vm.openSetup()
            vm.setup(Side.LAMB, Difficulty.VERY_EASY)
            vm.start()
        }
        rule.waitUntil(10000) { !vm.ui.value.busy && vm.playable() }
        val board = rule.onNodeWithTag("play-board")
        val hint = rule.onNodeWithText(rule.activity.getString(R.string.hint))
        val undo = rule.onNodeWithText(rule.activity.getString(R.string.undo))
        val before = listOf(board, hint, undo).map { it.fetchSemanticsNode().boundsInRoot }
        fun assertStable() {
            assertEquals(before, listOf(board, hint, undo).map { it.fetchSemanticsNode().boundsInRoot })
        }

        // An occupied point produces the multiline error in the reported bug.
        rule.runOnIdle { vm.tap(Node.A) }
        rule.onNodeWithTag("game-error-dialog").assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.illegal_action)).assertIsDisplayed()
        assertStable()
        rule.waitForIdle()
        android.os.SystemClock.sleep(350)
        val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(rule.activity.getExternalFilesDir(null),
            if (landscape) "landscape-error-popup.png" else "portrait-error-popup.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        val matchBeforeDismiss = vm.ui.value.match
        rule.onNodeWithText(rule.activity.getString(android.R.string.ok)).performClick()
        rule.onNodeWithTag("game-error-dialog").assertDoesNotExist()
        assertEquals(matchBeforeDismiss, vm.ui.value.match)
        assertStable()

        // Dismissing must allow the same error to appear again on the next invalid tap.
        rule.runOnIdle { vm.tap(Node.A) }
        rule.onNodeWithTag("game-error-dialog").assertExists()
        pressDialogBack()
        rule.waitUntil(10000) { vm.ui.value.message == null }
        assertNull(vm.ui.value.message)

        rule.runOnIdle { vm.hint() }
        rule.waitUntil(10000) { !vm.ui.value.busy && vm.ui.value.hint != null }
        rule.onNodeWithText("Try", substring = true).assertIsDisplayed()
        assertStable()

        // The hint can remain when an invalid tap adds an error.
        rule.runOnIdle { vm.tap(Node.A) }
        rule.onNodeWithTag("game-error-dialog").assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.illegal_action)).assertIsDisplayed()
        assertStable()
        pressDialogBack()
        rule.waitUntil(10000) { vm.ui.value.message == null }
        rule.onNodeWithTag("game-error-dialog").assertDoesNotExist()
        assertNotNull(vm.ui.value.hint)
        assertNull(vm.ui.value.message)
        assertStable()

        rule.runOnIdle { vm.tap(Node.B) }
        rule.waitUntil(10000) { !vm.ui.value.busy && vm.ui.value.match!!.state.eventCount == 2 }
        assertNull(vm.ui.value.message)
        assertNull(vm.ui.value.hint)
        assertStable()
    }
}
