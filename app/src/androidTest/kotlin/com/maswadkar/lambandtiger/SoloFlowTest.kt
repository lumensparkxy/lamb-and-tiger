package com.maswadkar.lambandtiger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SoloFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val vm
        get() = rule.activity.model

    private fun ready() {
        rule.waitUntil(10000) { !vm.ui.value.busy }
    }

    private fun start(side: Side = Side.LAMB) {
        ready()
        // Compose can be idle before a newly launched/rotated Activity owns the window.
        rule.waitUntil(10000) {
            rule.activity.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.RESUMED &&
                rule.activity.hasWindowFocus()
        }
        // The previous match may schedule a computer write while Compose settles.
        // Pause/cancel it first, then let any durable write finish before setup.
        rule.runOnIdle { vm.home() }
        ready()
        rule.runOnIdle {
            vm.openSetup()
            vm.setup(side, Difficulty.VERY_EASY)
            vm.start()
        }
        try {
            rule.waitUntil(10000) {
                !vm.ui.value.busy && vm.ui.value.match?.state?.sideToMove == side && !vm.ui.value.paused
            }
        } catch (timeout: ComposeTimeoutException) {
            val s = vm.ui.value
            throw AssertionError(
                "Start $side: screen=${s.screen}, busy=${s.busy}, paused=${s.paused}, " +
                    "error=${s.error}, human=${s.match?.human}, turn=${s.match?.state?.sideToMove}, " +
                    "events=${s.match?.state?.eventCount}", timeout,
            )
        }
    }

    @Test
    fun landscapeControlsRetainSpokenFeedback() {
        rule.runOnIdle {
            rule.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        rule.waitUntil(10000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        start()
        val polite = SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.LiveRegion,
            androidx.compose.ui.semantics.LiveRegionMode.Polite,
        )
        rule.onNode(hasContentDescription("Lambs captured: 0 / 5", substring = true))
            .assert(polite)
        rule.runOnIdle { vm.tap(Node.A) }
        rule.onNodeWithText(rule.activity.getString(R.string.illegal_action)).assert(polite)
        rule.runOnIdle { vm.hint() }
        ready()
        assertNotNull(vm.ui.value.hint)
        rule.onNodeWithText("Try", substring = true).assert(polite)
    }

    @Test
    fun tutorialCaptureExposesSelectionAndAnnouncesSuccess() {
        ready()
        val saved = vm.ui.value.match
        rule.runOnIdle { vm.lesson(3) }
        rule.onNodeWithContentDescription("Point A, Tiger, not selected").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Point A, Tiger, selected").assertIsSelected()
        rule.onNode(hasContentDescription("Point I, empty, capture", substring = true))
            .performScrollTo().performClick()
        rule.runOnIdle {
            assertTrue(vm.ui.value.lessonSuccess)
            assertEquals(saved, vm.ui.value.match)
        }
        rule.onNodeWithText(rule.activity.getString(vm.ui.value.message!!)).assert(
            SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.LiveRegion,
                androidx.compose.ui.semantics.LiveRegionMode.Polite,
            )
        )
    }

    @Test
    fun resizedBoardTapUsesRenderedCoordinates() {
        start()
        val density = rule.activity.resources.displayMetrics.density
        rule.onNodeWithTag("play-board").performTouchInput {
            val scale = minOf((width - 36f * density) / 10, (height - 60f * density) / 8)
            // B = (-3, 2) in the approved board. Verify Canvas hit testing,
            // rather than bypassing it through the coordinator or point list.
            click(androidx.compose.ui.geometry.Offset(width / 2f - 3 * scale, height / 2f - 2 * scale))
        }
        rule.waitUntil(10000) { vm.ui.value.match?.state?.eventCount == 2 && !vm.ui.value.busy }
        assertEquals(Occupant.LAMB, vm.ui.value.match!!.state.position.at(Node.B))
        rule.onNodeWithTag("controls-panel").assertDoesNotExist()
    }

    @Test
    fun directBoardKeepsActionsReachableWithoutPointList() {
        start()
        val before = vm.ui.value.match!!
        rule.onNodeWithTag("play-board").assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.choose_point)).assertDoesNotExist()
        rule.onNodeWithTag("play-board").performTouchInput { swipeUp() }
        rule.onNodeWithTag("controls-panel").assertDoesNotExist()
        for (label in listOf(R.string.hint, R.string.undo)) {
            val action = rule.onNodeWithText(rule.activity.getString(label))
            if (!action.isDisplayed()) action.performScrollTo()
            action.assertIsDisplayed()
        }
        assertEquals(before.state.events, vm.ui.value.match!!.state.events)
        assertFalse(vm.ui.value.paused)
    }

    private fun selectPoint(node: Node) {
        val density = rule.activity.resources.displayMetrics.density
        rule.onNodeWithTag("play-board").performTouchInput {
            val scale = minOf((width - 36f * density) / 10, (height - 60f * density) / 8)
            val point = Board.points.getValue(node)
            click(androidx.compose.ui.geometry.Offset(width / 2f + point.x * scale, height / 2f + (point.y - 4) * scale))
        }
    }

    @Test
    fun backDuringInitialSaveDoesNotReopenGame() {
        ready()
        for (reenterSetup in listOf(false, true)) {
            rule.runOnIdle {
                vm.home()
                vm.openSetup()
                vm.setup(Side.LAMB, Difficulty.VERY_EASY)
                vm.start()
                vm.back()
                if (reenterSetup) vm.openSetup()
            }
            ready()
            assertEquals(if (reenterSetup) Screen.SETUP else Screen.HOME, vm.ui.value.screen)
            assertTrue(vm.ui.value.paused)
            assertTrue(vm.ui.value.hasSave)
            assertEquals(0, vm.ui.value.match!!.state.eventCount)
        }
    }

    @Test
    fun corruptMatchDoesNotHideSavedPreferences() {
        start()
        rule.runOnIdle { vm.pause() }
        val application = rule.activity.application
        val file = java.io.File(application.noBackupFilesDir, "matches.sqlite")
        val database = android.database.sqlite.SQLiteDatabase.openDatabase(file.path, null, 0)
        val id = vm.ui.value.match!!.id
        val payload =
            database.rawQuery("SELECT payload FROM matches WHERE id=?", arrayOf(id)).use {
                assertTrue(it.moveToFirst())
                it.getBlob(0)
            }
        val store = androidx.lifecycle.ViewModelStore()
        try {
            database.execSQL("INSERT OR REPLACE INTO preferences(name,value) VALUES('dark','true')")
            database.execSQL("UPDATE matches SET payload=? WHERE id=?", arrayOf(byteArrayOf(0), id))
            lateinit var restored: GameViewModel
            rule.runOnIdle {
                restored =
                    androidx.lifecycle
                        .ViewModelProvider(
                            store,
                            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(
                                application
                            ),
                        )[GameViewModel::class.java]
            }
            rule.waitUntil(10000) { !restored.ui.value.busy }
            assertEquals(Screen.RECOVERY, restored.ui.value.screen)
            assertTrue(restored.ui.value.preferences.dark)
            assertTrue(restored.ui.value.paused)
        } finally {
            rule.runOnIdle { store.clear() }
            database.execSQL("UPDATE matches SET payload=? WHERE id=?", arrayOf(payload, id))
            database.execSQL(
                "INSERT OR REPLACE INTO preferences(name,value) VALUES('dark',?)",
                arrayOf(vm.ui.value.preferences.dark.toString()),
            )
            database.close()
        }
    }

    @Test
    fun placementComputerReplyHintUndoAndPause() {
        start()
        selectPoint(Node.B)
        rule.waitUntil(10000) { vm.ui.value.match?.state?.eventCount == 2 && !vm.ui.value.busy }
        rule.runOnIdle { vm.hint() }
        ready()
        assertNotNull(vm.ui.value.hint)
        rule.runOnIdle { vm.undo() }
        ready()
        assertEquals(0, vm.ui.value.match!!.state.eventCount)
        rule.runOnIdle { vm.navigate(Screen.SETTINGS) }
        assertTrue(vm.ui.value.paused)
        rule.runOnIdle { vm.back() }
        assertEquals(Screen.GAME, vm.ui.value.screen)
        assertTrue(vm.ui.value.paused)
        rule.runOnIdle {
            vm.resume()
            vm.home()
            vm.continueGame()
        }
        assertTrue(vm.ui.value.paused)
    }

    @Test
    fun recreationPreservesMatchAndPause() {
        start(Side.TIGER)
        val id = vm.ui.value.match!!.id
        val events = vm.ui.value.match!!.state.events
        rule.runOnIdle { vm.pause() }
        rule.activityRule.scenario.recreate()
        ready()
        assertEquals(id, vm.ui.value.match!!.id)
        assertEquals(events, vm.ui.value.match!!.state.events)
        assertTrue(vm.ui.value.paused)
    }

    @Test
    fun completeSoloGamesAsBothSides() {
        for (side in Side.entries) {
            start(side)
            var turns = 0
            while (vm.ui.value.match!!.state.outcome == Outcome.ONGOING && turns < 400) {
                rule.waitUntil(10000) {
                    val s = vm.ui.value
                    s.match!!.state.outcome != Outcome.ONGOING ||
                        (!s.busy && s.match.state.sideToMove == side)
                }
                val state = vm.ui.value.match!!.state
                if (state.outcome != Outcome.ONGOING) break
                val a = Computer.choose(state, Difficulty.EASY, turns.toLong())!!.action
                a.origin?.let(::selectPoint)
                selectPoint(a.destination)
                rule.waitUntil(10000) {
                    vm.ui.value.match!!.state.eventCount > state.eventCount && !vm.ui.value.busy
                }
                turns++
            }
            assertNotEquals(
                "Game did not finish in test budget",
                Outcome.ONGOING,
                vm.ui.value.match!!.state.outcome,
            )
            assertFalse(vm.ui.value.hasSave)
            val finished = vm.ui.value.match!!
            rule.waitUntil(10000) {
                rule.onAllNodesWithTag("game-result-dialog").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText(rule.activity.getString(R.string.switch_sides)).performScrollTo().performClick()
            rule.waitUntil(10000) { !vm.ui.value.busy && vm.ui.value.match?.id != finished.id }
            assertEquals(finished.human.other(), vm.ui.value.match!!.human)
            assertEquals(finished.difficulty, vm.ui.value.match!!.difficulty)
            assertEquals(0, vm.ui.value.match!!.state.position.captured)
            assertTrue(vm.ui.value.match!!.state.eventCount <= 1)
        }
    }

    @Test
    fun selectionSurvivesRotationAndBackgroundDiscardsComputerWork() {
        start(Side.TIGER)
        val state = vm.ui.value.match!!.state
        val action = GameEngine.available(state).first()
        rule.runOnIdle { vm.tap(action.origin!!) }
        rule.activityRule.scenario.recreate()
        ready()
        assertEquals(action.origin, vm.ui.value.selected)
        rule.runOnIdle { vm.tap(action.destination) }
        ready()
        rule.runOnIdle { vm.foreground(false) }
        val count = vm.ui.value.match!!.state.eventCount
        Thread.sleep(1300)
        assertEquals(count, vm.ui.value.match!!.state.eventCount)
        assertTrue(vm.ui.value.paused)
        rule.runOnIdle { vm.foreground(true) }
        assertTrue(vm.ui.value.paused)
    }

    @Test
    fun allSettingsPreserveGame() {
        start()
        val match = vm.ui.value.match!!
        rule.runOnIdle {
            vm.navigate(Screen.SETTINGS)
            vm.preference("dark", true)
            vm.preference("sound", true)
            vm.preference("vibration", true)
            vm.preference("reduced", true)
        }
        assertEquals(match.id, vm.ui.value.match!!.id)
        assertEquals(match.state.events, vm.ui.value.match!!.state.events)
        assertTrue(vm.ui.value.preferences.reduced)
        assertTrue(vm.ui.value.paused)
        rule.runOnIdle {
            vm.preference("sound", false)
            vm.preference("vibration", false)
            vm.preference("dark", false)
            vm.preference("reduced", false)
        }
    }

    @Test
    fun tutorialNeverChangesSaveAndNearbyEntryOffersHostAndJoin() {
        start()
        val id = vm.ui.value.match!!.id
        val before = vm.ui.value.match!!.state.events
        rule.runOnIdle {
            vm.home()
            vm.lesson(0)
            vm.tap(Node.B)
        }
        assertTrue(vm.ui.value.lessonSuccess)
        assertEquals(id, vm.ui.value.match!!.id)
        assertEquals(before, vm.ui.value.match!!.state.events)
        rule.runOnIdle {
            vm.lesson(5)
            repeat(8) { vm.repetitionLesson() }
        }
        assertEquals(Outcome.REPETITION_DRAW, vm.ui.value.lessonState!!.outcome)
        rule.runOnIdle {
            vm.home()
            vm.navigate(Screen.NEARBY)
        }
        rule.onNodeWithText(rule.activity.getString(R.string.friend_host)).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.friend_join)).assertIsDisplayed()
        assertEquals(id, vm.ui.value.match!!.id)
        assertEquals(before, vm.ui.value.match!!.state.events)
    }
}
