package com.maswadkar.lambandtiger

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.maswadkar.lambandtiger.game.Difficulty
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SetupLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun portraitStartRemainsVisibleWhileChoosingDifficulty() = checkStart(false)

    @Test fun landscapeStartRemainsVisibleWhileChoosingDifficulty() = checkStart(true)

    private fun checkStart(landscape: Boolean) {
        rule.runOnIdle {
            rule.activity.requestedOrientation = if (landscape)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        rule.waitUntil(10000) {
            rule.activity.resources.configuration.orientation == if (landscape)
                Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        }
        val vm = rule.activity.model
        rule.waitUntil(10000) { !vm.ui.value.busy && rule.activity.hasWindowFocus() }
        rule.runOnIdle { vm.home(); vm.openSetup() }
        val start = rule.onNodeWithTag("setup-start")
        rule.onNodeWithText(rule.activity.getString(R.string.window_small)).assertDoesNotExist()
        rule.waitUntil(10000) { start.isDisplayed() }
        start.assertIsDisplayed().assertIsEnabled()
        val bounds = start.fetchSemanticsNode().boundsInRoot
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Entire Start button must be inside the window", root.contains(bounds.topLeft) &&
            bounds.right <= root.right && bounds.bottom <= root.bottom)
        capture(if (landscape) "setup-landscape" else "setup-portrait")

        val last = Difficulty.entries.last()
        rule.onNodeWithTag("setup-difficulty-${last.name}").performScrollTo().performClick()
            .assertIsSelected()
        assertEquals(last, vm.ui.value.setupDifficulty)
        start.assertIsDisplayed()
        assertEquals("Scrolling choices must not move Start", bounds, start.fetchSemanticsNode().boundsInRoot)
        // Start remains clickable without scrolling back to it; existing saves still require confirmation.
        start.performClick()
        rule.waitUntil(10000) { vm.ui.value.replaceDialog || vm.ui.value.screen == Screen.GAME }
        rule.runOnIdle { vm.home() }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(rule.activity.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
