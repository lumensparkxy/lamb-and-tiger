package com.maswadkar.lambandtiger.game

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class GameTest {
    private fun mask(s: String) = s.fold(0) { n, c -> n or Node.valueOf(c.toString()).bit }

    private fun fixture(
        t: String = "ADE",
        l: String = "",
        reserve: Int = 15,
        captured: Int = 0,
        turn: Side = Side.TIGER,
    ) =
        (GameEngine.fixture(Position(mask(t), mask(l), turn, reserve, captured))
                as Transition.Applied)
            .state

    private fun play(s: GameState, a: Action) = (GameEngine.apply(s, a) as Transition.Applied).state

    @Test
    fun boardExactlyMatchesApprovedLines() {
        assertEquals(23, Node.entries.size)
        assertEquals(38, Board.edges.size)
        assertEquals(56, Board.captures.size)
        assertEquals(38, Board.edges.toSet().size)
        assertFalse(Node.T in Board.neighbours(Node.N))
        assertFalse(Node.W in Board.neighbours(Node.S))
        assertTrue(Path(Node.A, Node.C, Node.I) in Board.captures)
        assertFalse(Path(Node.B, Node.C, Node.I) in Board.captures)
    }

    @Test
    fun openingAndPlacementE01E02() {
        val old = GameEngine.newGame()
        val s = play(old, Action.Place(Node.C))
        assertEquals(15, old.position.reserve)
        assertTrue(old.events.isEmpty())
        assertEquals(14, s.position.reserve)
        assertEquals(Side.TIGER, s.sideToMove)
        assertTrue(GameEngine.apply(old, Action.Place(Node.A)) is Transition.Rejected)
        val lamb = fixture(l = "C", reserve = 14, turn = Side.LAMB)
        assertTrue(GameEngine.apply(lamb, Action.Step(Node.C, Node.B)) is Transition.Rejected)
    }

    @Test
    fun stepsAndCapturesE03toE11() {
        val s = fixture(t = "AEW", l = "CO", reserve = 13)
        assertTrue(Action.Step(Node.A, Node.D) in GameEngine.available(s))
        assertTrue(GameEngine.apply(s, Action.Step(Node.A, Node.I)) is Transition.Rejected)
        val c = play(s, Action.Capture(Node.A, Node.C, Node.I))
        assertEquals(1, c.position.captured)
        assertEquals(13, c.position.reserve)
        assertEquals(Occupant.EMPTY, c.position.at(Node.C))
        assertEquals(Occupant.TIGER, c.position.at(Node.I))
        assertTrue(
            GameEngine.apply(c, Action.Capture(Node.I, Node.O, Node.T)) is Transition.Rejected
        )
        for (l in listOf("CI", "O")) assertTrue(
            GameEngine.apply(
                fixture(l = l, reserve = 15 - l.length),
                Action.Capture(Node.A, Node.C, Node.I),
            ) is Transition.Rejected
        )
        assertTrue(
            GameEngine.apply(fixture(t = "ACE"), Action.Capture(Node.A, Node.C, Node.I))
                is Transition.Rejected
        )
        val b = fixture(t = "BEW", l = "C", reserve = 14)
        assertTrue(
            GameEngine.apply(b, Action.Capture(Node.B, Node.C, Node.D)) is Transition.Applied
        )
        assertTrue(
            GameEngine.apply(b, Action.Capture(Node.B, Node.C, Node.I)) is Transition.Rejected
        )
        assertTrue(
            GameEngine.apply(fixture(t = "NEW"), Action.Step(Node.N, Node.T)) is Transition.Rejected
        )
        val lamb = fixture(t = "BEW", l = "ACDFGHIJKLMNOPS", reserve = 0, turn = Side.LAMB)
        assertTrue(
            GameEngine.apply(lamb, Action.Capture(Node.A, Node.C, Node.I)) is Transition.Rejected
        )
    }

    @Test
    fun phaseE12AndMovement() {
        val s = fixture(t = "ADE", l = "BCFGHILMNOP", reserve = 1, captured = 3, turn = Side.LAMB)
        val next = play(s, Action.Place(Node.S))
        assertEquals(Phase.MOVEMENT, next.position.phase)
        assertEquals(Side.TIGER, next.sideToMove)
        val later = play(next, Action.Step(Node.D, Node.J))
        assertTrue(GameEngine.available(later).all { it is Action.Step })
    }

    @Test
    fun endingsE13toE16() {
        assertEquals(Outcome.LAMBS_WIN, fixture(l = "BCFGHIJKLMNOPQR", reserve = 0).outcome)
        assertEquals(Outcome.ONGOING, fixture(l = "BCFGHJKLMNOPQRS", reserve = 0).outcome)
        val win =
            play(
                fixture(l = "C", reserve = 10, captured = 4),
                Action.Capture(Node.A, Node.C, Node.I),
            )
        assertEquals(Outcome.TIGERS_WIN, win.outcome)
        assertNull(win.sideToMove)
        assertEquals(1, win.repetitions.values.sum())
        assertEquals(1, win.eventCount)
        assertTrue(GameEngine.apply(win, Action.Place(Node.B)) is Transition.Rejected)
        assertTrue(GameEngine.completeAgreedDraw(win) is Transition.Rejected)
        assertEquals(
            Outcome.ONGOING,
            play(
                    fixture(l = "C", reserve = 11, captured = 3),
                    Action.Capture(Node.A, Node.C, Node.I),
                )
                .outcome,
        )
    }

    @Test
    fun threefoldE17AndEqualityE18() {
        var s = fixture(l = "BCFGHILMNOP", reserve = 0, captured = 4, turn = Side.LAMB)
        val cycle =
            listOf(
                Action.Step(Node.P, Node.Q),
                Action.Step(Node.D, Node.J),
                Action.Step(Node.Q, Node.P),
                Action.Step(Node.J, Node.D),
            )
        val initial = s.position
        repeat(2) { round ->
            cycle.forEach { s = play(s, it) }
            assertEquals(initial, s.position)
            assertEquals(round + 2, s.repetitions[initial])
        }
        assertEquals(Outcome.REPETITION_DRAW, s.outcome)
        assertTrue(GameEngine.apply(s, cycle.first()) is Transition.Rejected)
        assertNotEquals(initial, initial.copy(turn = Side.TIGER))
    }

    @Test
    fun agreementE19AndWinPriorityE20() {
        val s = GameEngine.newGame()
        val next = (GameEngine.completeAgreedDraw(s) as Transition.Applied).state
        assertEquals(s.position, next.position)
        assertEquals(s.repetitions, next.repetitions)
        assertEquals(Outcome.AGREED_DRAW, next.outcome)
        assertTrue(GameEngine.completeAgreedDraw(next) is Transition.Rejected)
        assertTrue(GameEngine.replay(next.events) is Transition.Invalid)
        assertTrue(GameEngine.replay(next.events, allowAgreement = true) is Transition.Applied)
    }

    @Test
    fun invalidE21AndImportFailure() {
        assertTrue(
            GameEngine.fixture(Position(mask("ADE"), 0, Side.LAMB, 0, 0)) is Transition.Invalid
        )
        assertTrue(
            GameEngine.fixture(Position(mask("ADE"), mask("A"), Side.LAMB, 14, 0))
                is Transition.Invalid
        )
        assertTrue(GameEngine.replay(emptyList(), "unknown") is Transition.Invalid)
        assertTrue(
            GameEngine.replay(listOf(Event.Played(Action.Place(Node.A)))) is Transition.Invalid
        )
    }

    @Test
    fun replayUndoAndImmutableViews() {
        val initial = GameEngine.newGame()
        var s = play(initial, Action.Place(Node.B))
        s = play(s, Action.Step(Node.A, Node.C))
        assertEquals(0, GameEngine.undoPrefix(s, Side.LAMB))
        assertNull(GameEngine.undoPrefix(play(initial, Action.Place(Node.B)), Side.TIGER))
        val replay = (GameEngine.replay(s.events) as Transition.Applied).state
        assertEquals(s.position, replay.position)
        assertEquals(s.repetitions, replay.repetitions)
        assertEquals(
            initial.position,
            (GameEngine.replay(s.events.take(0)) as Transition.Applied).state.position,
        )
        try {
            (s.events as MutableList).clear()
            fail()
        } catch (_: UnsupportedOperationException) {}
        try {
            (s.repetitions as MutableMap).clear()
            fail()
        } catch (_: UnsupportedOperationException) {}
    }

    @Test
    fun tenThousandSeededTransitionsAndReplay() {
        val random = Random(17017)
        var s = GameEngine.newGame()
        repeat(10000) {
            if (s.outcome != Outcome.ONGOING) s = GameEngine.newGame()
            val before = s
            val tigerState =
                (GameEngine.fixture(before.position.copy(turn = Side.TIGER)) as Transition.Applied)
                    .state
            val tigerActions = GameEngine.available(tigerState)
            val activity = GameEngine.tigerActivity(before.position)
            assertEquals(tigerActions.size, activity.mobility)
            assertEquals(GameEngine.tigerMobility(before.position), activity.mobility)
            assertEquals(tigerActions.count { it is Action.Capture }, activity.captures)
            assertEquals(tigerActions.map { it.origin }.distinct().size, activity.mobileTigers)
            val a = GameEngine.available(s).random(random)
            s = play(s, a)
            val fast = (GameEngine.advanceLegal(before, a) as Transition.Applied).state
            assertEquals(s.position, fast.position)
            assertEquals(s.outcome, fast.outcome)
            assertEquals(s.repetitions, fast.repetitions)
            assertNull(GameEngine.structuralError(s.position))
            assertEquals(before.eventCount + 1, s.eventCount)
            if (it % 101 == 0) {
                val r = (GameEngine.replay(s.events) as Transition.Applied).state
                assertEquals(s.position, r.position)
                assertEquals(s.outcome, r.outcome)
                assertEquals(s.repetitions, r.repetitions)
            }
        }
    }

    @Test
    fun computerLegalDeterministicAndCancellable() {
        for (d in Difficulty.entries) {
            val s = GameEngine.newGame()
            val a = Computer.choose(s, d, 42)!!
            assertTrue(a.action in GameEngine.available(s))
            assertEquals(a, Computer.choose(s, d, 42))
            assertNotNull(Computer.choose(s, d, 42) { true })
        }
    }
}
