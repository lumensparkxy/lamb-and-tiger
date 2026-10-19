package com.maswadkar.lambandtiger

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.maswadkar.lambandtiger.data.SoloMatch
import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ResultLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun portraitResultOverlaysStationaryBoard() = checkResult(false)
    @Test fun landscapeResultOverlaysStationaryBoard() = checkResult(true)

    private fun checkResult(landscape: Boolean) {
        rule.runOnIdle {
            rule.activity.requestedOrientation = if (landscape)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        rule.waitUntil(10000) {
            rule.activity.resources.configuration.orientation == if (landscape)
                Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        }
        // Approved fifth-capture fixture; render only, never write it to a real save.
        val initial = (GameEngine.fixture(Position(Node.A.bit or Node.D.bit or Node.E.bit,
            Node.C.bit, Side.TIGER, 10, 4)) as Transition.Applied).state
        var state by mutableStateOf(UiState(screen = Screen.GAME, busy = false, paused = false,
            match = SoloMatch("result-layout", Side.TIGER, Difficulty.EASY, 0, initial),
            preferences = Preferences(reduced = true)))
        rule.runOnIdle {
            rule.activity.setContent {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                        GameScreen(state, rule.activity.model)
                    }
                }
            }
        }
        val before = rule.onNodeWithTag("play-board").fetchSemanticsNode().boundsInRoot
        val capture = Action.Capture(Node.A, Node.C, Node.I)
        val finished = (GameEngine.apply(initial, capture) as Transition.Applied).state
        rule.runOnIdle { state = state.copy(match = state.match!!.copy(state = finished), lastAction = capture) }
        rule.onNodeWithTag("game-result-dialog").assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.reason_tigers)).assertIsDisplayed()
        screenshot(if (landscape) "landscape-popup" else "portrait-popup")
        rule.onNodeWithText(rule.activity.getString(R.string.view_board)).performScrollTo().performClick()
        rule.onNodeWithTag("game-result-dialog").assertDoesNotExist()
        assertEquals(before, rule.onNodeWithTag("play-board").fetchSemanticsNode().boundsInRoot)
        screenshot(if (landscape) "landscape-board" else "portrait-board")
        val reopen = rule.onNodeWithTag("view-result")
        if (!reopen.isDisplayed()) reopen.performScrollTo()
        reopen.performClick()
        rule.onNodeWithTag("game-result-dialog").assertExists()
        // Android Back dismisses the popup too, leaving the final board inspectable.
        pressDialogBack()
        rule.waitUntil(10000) {
            rule.onAllNodesWithTag("game-result-dialog").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithTag("game-result-dialog").assertDoesNotExist()
        assertEquals(finished, state.match!!.state)
    }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        // Window animations run outside Compose's test clock.
        android.os.SystemClock.sleep(350)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = java.io.File(rule.activity.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
