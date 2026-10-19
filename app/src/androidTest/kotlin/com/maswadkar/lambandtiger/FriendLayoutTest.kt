package com.maswadkar.lambandtiger

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.maswadkar.lambandtiger.friend.FriendMatch
import com.maswadkar.lambandtiger.friend.FriendUi
import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Render real UI with isolated state/callbacks: never pair radios or modify either save. */
class FriendLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @org.junit.Before fun selectTestAudience() {
        rule.runOnIdle { rule.activity.audience.choose(com.maswadkar.lambandtiger.privacy.AgeGroup.UNDER_13) }
    }
    private var state by mutableStateOf(FriendUi())
    private var midnight by mutableStateOf(false)

    private fun show(initial: FriendUi, actions: FriendActions = FriendActions(), landscape: Boolean = false) {
        rule.runOnIdle {
            rule.activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        rule.waitUntil(10000) {
            rule.activity.resources.configuration.orientation == if (landscape) Configuration.ORIENTATION_LANDSCAPE
                else Configuration.ORIENTATION_PORTRAIT
        }
        rule.runOnIdle {
            state = initial
            rule.activity.setContent {
                MaterialTheme(colorScheme = if (midnight) dark else light) {
                    Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                        FriendContent(state, Preferences(reduced = true, dark = midnight), "Nearby 4821", actions)
                    }
                }
            }
        }
    }

    private fun session(game: GameState = (GameEngine.fixture(Position(
        Node.A.bit or Node.D.bit or Node.E.bit, Node.H.bit or Node.J.bit or Node.R.bit,
        Side.LAMB, 12, 0)) as Transition.Applied).state) = FriendUi(
        phase = "session", match = FriendMatch("layout", "local", "peer", Side.LAMB, game),
        player = "local", playable = true, paused = false, saved = true, savedHost = true,
    )

    private fun clickText(id: Int) {
        val node = rule.onNodeWithText(rule.activity.getString(id))
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }

    private fun clickTag(tag: String) {
        val node = rule.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }

    @Test fun pairingKeepsSaveConfirmationAndCodeConfirmationExplicit() {
        val starts = mutableListOf<Pair<Boolean, Boolean>>()
        var confirmations = 0
        var stops = 0
        show(FriendUi(saved = true, savedHost = true), FriendActions(
            start = { h, r -> starts += h to r }, confirm = { confirmations++ }, stop = { stops++ }))
        screenshot("entry")
        clickTag("friend-host")
        assertTrue(starts.isEmpty())
        clickText(R.string.cancel)
        clickTag("friend-join")
        clickText(R.string.replace)
        assertEquals(listOf(false to false), starts)
        clickText(R.string.friend_continue)
        assertEquals(true to true, starts.last())
        rule.runOnIdle { state = FriendUi(phase = "confirm", code = "4821", peerName = "Nearby 4821") }
        rule.onNodeWithTag("friend-confirmation-code").assertTextEquals("4821")
        screenshot("code")
        assertEquals(0, confirmations)
        clickText(R.string.friend_confirm)
        assertEquals(1, confirmations)
        clickText(R.string.friend_reject)
        assertEquals(1, stops)
    }

    @Test fun oppositeSidesAndReadinessAreRequired() {
        var readiness = 0
        var selection: Side? = null
        show(FriendUi(phase = "session", localSide = Side.LAMB, peerSide = Side.LAMB),
            FriendActions(ready = { readiness++ }, select = { selection = it }))
        rule.onNodeWithTag("friend-ready").assertIsNotEnabled()
        clickTag("friend-side-TIGER")
        assertEquals(Side.TIGER, selection)
        rule.runOnIdle { state = state.copy(localSide = Side.TIGER, peerReady = true) }
        screenshot("sides")
        clickTag("friend-ready")
        assertEquals(1, readiness)
        rule.runOnIdle { state = state.copy(ready = true) }
        clickText(R.string.friend_cancel_ready)
        assertEquals(2, readiness)
        rule.runOnIdle { state = state.copy(busy = true) }
        rule.onNodeWithTag("friend-ready").assertIsNotEnabled()
        rule.onNodeWithTag("friend-side-LAMB").assertIsNotEnabled()
    }

    @Test fun portraitBoardAndResultStayStable() = result(false)
    @Test fun landscapeBoardAndResultStayStable() = result(true)

    private fun result(landscape: Boolean) {
        val initial = (GameEngine.fixture(Position(Node.A.bit or Node.D.bit or Node.E.bit,
            Node.C.bit, Side.TIGER, 10, 4)) as Transition.Applied).state
        val rematches = mutableListOf<Boolean>()
        show(session(initial), FriendActions(rematch = { rematches += it }), landscape)
        val before = rule.onNodeWithTag("friend-board").fetchSemanticsNode().boundsInRoot
        val finished = (GameEngine.apply(initial, Action.Capture(Node.A, Node.C, Node.I)) as Transition.Applied).state
        rule.runOnIdle { state = state.copy(match = state.match!!.copy(state = finished), playable = false) }
        rule.onNodeWithTag("friend-result-dialog").assertExists()
        screenshot(if (landscape) "landscape-result" else "portrait-result")
        clickText(R.string.view_board)
        assertEquals(before, rule.onNodeWithTag("friend-board").fetchSemanticsNode().boundsInRoot)
        clickTag("friend-match-action")
        clickText(R.string.switch_sides)
        assertEquals(listOf(true), rematches)
        clickText(R.string.play_again)
        assertEquals(listOf(true, false), rematches)
        pressDialogBack()
        rule.onNodeWithTag("friend-result-dialog").assertDoesNotExist()
        assertEquals(finished, state.match!!.state)
    }

    @Test fun blockedStatesPreventBoardInputAndReconnectKeepsGeometry() {
        val taps = mutableListOf<Node>()
        val starts = mutableListOf<Pair<Boolean, Boolean>>()
        val active = session()
        show(active, FriendActions(tap = { taps += it }, start = { h, r -> starts += h to r }))
        screenshot("board-light")
        tapPoint(Node.C)
        assertEquals(listOf(Node.C), taps)
        val before = rule.onNodeWithTag("friend-board").fetchSemanticsNode().boundsInRoot
        val blocked = listOf(
            active.copy(busy = true), active.copy(busy = true, match = null),
            active.copy(paused = true), active.copy(playable = false),
            active.copy(player = "peer"), active.copy(phase = "disconnected", paused = true, playable = false),
            active.copy(match = null, phase = "disconnected", paused = true, playable = false),
        )
        blocked.forEach { value ->
            rule.runOnIdle { state = value }
            tapPoint(Node.C)
            assertEquals(1, taps.size)
            assertEquals(before, rule.onNodeWithTag("friend-board").fetchSemanticsNode().boundsInRoot)
        }
        rule.runOnIdle { state = state.copy(error = true) }
        rule.onNodeWithTag("friend-error-dialog").assertExists()
        clickText(android.R.string.ok)
        assertEquals(before, rule.onNodeWithTag("friend-board").fetchSemanticsNode().boundsInRoot)
        screenshot("disconnected")
        clickTag("friend-match-action")
        assertEquals(listOf(true to true), starts)
    }

    @Test fun drawOfferRequiresConfirmationAndRemainsReviewable() {
        var offers = 0
        var pauses = 0
        val answers = mutableListOf<Boolean>()
        val active = session()
        show(active, FriendActions(offer = { offers++ }, answer = { answers += it }, pause = { pauses++ }))
        clickTag("friend-menu")
        assertEquals(0, pauses)
        clickText(R.string.friend_offer_draw)
        assertEquals(0, offers)
        clickText(R.string.cancel)
        clickTag("friend-menu")
        clickText(R.string.friend_offer_draw)
        clickText(R.string.friend_send_offer)
        assertEquals(1, offers)
        rule.runOnIdle { state = active.copy(offer = true, offerer = false, paused = true, playable = false) }
        rule.onNodeWithTag("friend-offer-dialog").assertExists()
        screenshot("draw-offer")
        pressDialogBack()
        clickTag("friend-match-action")
        clickText(R.string.friend_decline)
        clickText(R.string.friend_accept_draw)
        assertEquals(listOf(false, true), answers)
        rule.runOnIdle { state = state.copy(offerer = true) }
        rule.onNodeWithText(rule.activity.getString(R.string.friend_accept_draw)).assertDoesNotExist()
        clickText(R.string.friend_withdraw)
        assertEquals(false, answers.last())
    }

    @Test fun midnightPauseAndReadinessUseExistingCommands() {
        var pauses = 0
        var ready = 0
        show(session(), FriendActions(pause = { pauses++ }, ready = { ready++ }))
        rule.runOnIdle { midnight = true }
        screenshot("board-dark")
        clickTag("friend-match-action")
        assertEquals(1, pauses)
        rule.runOnIdle { state = state.copy(paused = true, playable = false, peerReady = true) }
        clickText(R.string.resume)
        assertEquals(1, ready)
        rule.runOnIdle { state = state.copy(ready = true) }
        screenshot("resume-ready")
        clickText(R.string.friend_cancel_ready)
        assertEquals(2, ready)
    }

    private fun tapPoint(node: Node) {
        val density = rule.activity.resources.displayMetrics.density
        val point = Board.points.getValue(node)
        rule.onNodeWithTag("friend-board").performTouchInput {
            val scale = minOf((width - 36 * density) / 10, (height - 60 * density) / 8)
            click(Offset(width / 2f + point.x * scale, height / 2f + (point.y - 4) * scale))
        }
    }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        android.os.SystemClock.sleep(350)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val file = java.io.File(rule.activity.getExternalFilesDir(null), "friend-$name.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
