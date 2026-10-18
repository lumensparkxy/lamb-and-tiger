package com.maswadkar.lambandtiger

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BoardAnimationTest {
    @get:Rule val rule=createComposeRule()
    private fun apply(state: GameState,action: Action)=(GameEngine.apply(state,action) as Transition.Applied).state

    @Test fun firstCaptureFrameKeepsTigerAtOriginBeforeTravelling() {
        var state by mutableStateOf(apply(GameEngine.newGame(),Action.Place(Node.C)))
        var density=1f
        rule.setContent {
            density=LocalDensity.current.density
            MaterialTheme {
                BoardView(state,null,emptyList(),null,{},Modifier.fillMaxWidth().height(360.dp).testTag("capture-board"),reducedMotion=false)
            }
        }
        rule.mainClock.autoAdvance=false
        rule.runOnIdle {state=apply(state,Action.Capture(Node.A,Node.C,Node.I))}
        rule.mainClock.advanceTimeByFrame()
        fun tigerVisible(node: Node): Boolean {
            val pixels=rule.onNodeWithTag("capture-board").captureToImage().toPixelMap()
            val scale=minOf((pixels.width-36*density)/10,(pixels.height-60*density)/8)
            val point=Board.points.getValue(node)
            val x=(pixels.width/2+point.x*scale).toInt()
            val y=(pixels.height/2+(point.y-4)*scale).toInt()
            val radius=(8*density).toInt()
            return (x-radius..x+radius).any { px -> (y-radius..y+radius).any { py ->
                val color=pixels[px.coerceIn(0,pixels.width-1),py.coerceIn(0,pixels.height-1)]
                color.red>.55f && color.green>.25f && color.green<.8f && color.blue<.3f
            } }
        }
        assertTrue("First frame must not flash the destination",tigerVisible(Node.A))
        assertFalse(tigerVisible(Node.I))
        rule.mainClock.advanceTimeBy(420)
        assertFalse(tigerVisible(Node.A))
        assertTrue(tigerVisible(Node.I))
    }

    @Test fun committedActionsAnimateAndReducedMotionCancelsWithoutReplaying() {
        var state by mutableStateOf(GameEngine.newGame())
        var reduced by mutableStateOf(false)
        var running=false
        var starts=0
        rule.setContent {
            MaterialTheme {
                BoardView(state,null,emptyList(),null,{},Modifier.fillMaxWidth().height(360.dp),
                    reducedMotion=reduced,onMotionRunning={running=it;if(it) starts++})
            }
        }
        rule.mainClock.autoAdvance=false
        rule.runOnIdle { assertEquals(0,starts);state=apply(state,Action.Place(Node.C)) }
        rule.mainClock.advanceTimeBy(32)
        rule.runOnIdle { assertTrue(running) }
        rule.mainClock.advanceTimeBy(300)
        rule.runOnIdle { assertFalse(running);state=apply(state,Action.Capture(Node.A,Node.C,Node.I)) }
        rule.mainClock.advanceTimeBy(64)
        rule.runOnIdle { assertTrue(running);reduced=true }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertFalse(running);assertEquals(1,state.position.captured);reduced=false }
        rule.mainClock.advanceTimeBy(400)
        rule.runOnIdle { assertEquals(2,starts);assertFalse(running);state=apply(state,Action.Place(Node.B)) }
        rule.mainClock.advanceTimeBy(320)
        rule.runOnIdle { state=apply(state,Action.Step(Node.D,Node.J)) }
        rule.mainClock.advanceTimeBy(64)
        rule.runOnIdle { assertTrue(running) }
        rule.mainClock.advanceTimeBy(300)
        rule.runOnIdle { assertFalse(running);assertEquals(4,starts);assertEquals(Occupant.TIGER,state.position.at(Node.J)) }
    }

    @Test fun restoredPositionsAndUndoDoNotReplayAndPauseSettlesInFlightMotion() {
        val saved=apply(GameEngine.newGame(),Action.Place(Node.C))
        var state by mutableStateOf(saved)
        var enabled by mutableStateOf(true)
        var running=false
        var starts=0
        rule.setContent {
            MaterialTheme {
                BoardView(state,null,emptyList(),null,{},Modifier.fillMaxWidth().height(360.dp),
                    reducedMotion=false,motionEnabled=enabled,onMotionRunning={running=it;if(it) starts++})
            }
        }
        rule.mainClock.autoAdvance=false
        rule.runOnIdle { assertEquals(0,starts);state=apply(state,Action.Capture(Node.A,Node.C,Node.I)) }
        rule.mainClock.advanceTimeBy(64)
        rule.runOnIdle { assertTrue(running);enabled=false }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertFalse(running);enabled=true }
        rule.mainClock.advanceTimeBy(400)
        rule.runOnIdle { assertEquals(1,starts);state=GameEngine.newGame() }
        rule.mainClock.advanceTimeBy(400)
        rule.runOnIdle { assertEquals(1,starts);assertFalse(running);assertEquals(15,state.position.reserve) }
    }
}
