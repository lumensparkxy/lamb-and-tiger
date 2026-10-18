package com.maswadkar.lambandtiger

import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Test

class BoardMotionTest {
    private fun apply(state: GameState, action: Action) = (GameEngine.apply(state,action) as Transition.Applied).state

    @Test fun followsOnlyTheNextCommittedAction() {
        val opening=GameEngine.newGame()
        val placed=apply(opening,Action.Place(Node.B))
        val moved=apply(placed,Action.Step(Node.D,Node.J))
        assertEquals(Action.Place(Node.B),boardMotion(opening,placed)?.action)
        assertEquals(Action.Step(Node.D,Node.J),boardMotion(placed,moved)?.action)
        assertNull(boardMotion(opening,opening))
        assertNull(boardMotion(opening,moved)) // missed transitions on reconnect/restoration
        assertNull(boardMotion(moved,opening)) // undo
        val otherHistory=apply(opening,Action.Place(Node.G))
        assertNull(boardMotion(otherHistory,moved)) // same count, different history
        val drawn=(GameEngine.completeAgreedDraw(placed) as Transition.Applied).state
        assertNull(boardMotion(placed,drawn))
    }

    @Test fun captureKeepsTheRemovedLambOnlyInThePresentationSnapshot() {
        val before=apply(GameEngine.newGame(),Action.Place(Node.C))
        val action=Action.Capture(Node.A,Node.C,Node.I)
        val after=apply(before,action)
        val motion=boardMotion(before,after)!!
        assertEquals(Occupant.LAMB,motion.before.position.at(Node.C))
        assertEquals(Occupant.EMPTY,motion.after.position.at(Node.C))
        assertEquals(Occupant.TIGER,motion.after.position.at(Node.I))
        assertEquals(1,motion.after.position.captured)
        assertEquals(2,motion.after.eventCount)
        assertEquals(360,motion.durationMillis)
    }

    @Test fun restartingALessonDoesNotAnimateAcrossFixtureOrigins() {
        val first=(GameEngine.fixture(GameEngine.opening.copy(turn=Side.TIGER)) as Transition.Applied).state
        val other=(GameEngine.fixture(Position(Node.A.bit or Node.E.bit or Node.W.bit,Node.C.bit or Node.O.bit,Side.TIGER,13,0)) as Transition.Applied).state
        val captured=apply(other,Action.Capture(Node.A,Node.C,Node.I))
        assertNull(boardMotion(first,captured))
        assertNotNull(boardMotion(other,captured))
    }
}
