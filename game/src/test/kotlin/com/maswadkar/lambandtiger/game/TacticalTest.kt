package com.maswadkar.lambandtiger.game

import org.junit.Assert.*
import org.junit.Test

class TacticalTest {
    @Test
    fun twentyDefensiveFixturesAcrossBothSidesAndPhases() {
        val rows =
            javaClass.getResource("/defensive-fixtures.csv")!!.readText().lines().drop(1).filter {
                it.isNotBlank()
            }
        assertEquals(20, rows.size)
        fun mask(text: String) = text.fold(0) { bits, c -> bits or Node.valueOf(c.toString()).bit }
        fun decode(text: String): Action {
            fun node(i: Int) = Node.valueOf(text[i].toString())
            return when (text[0]) {
                'P' -> Action.Place(node(1))
                'S' -> Action.Step(node(1), node(2))
                else -> Action.Capture(node(1), node(2), node(3))
            }
        }
        for (row in rows) {
            val c = row.split(',')
            val side = Side.valueOf(c[0])
            val state =
                (GameEngine.fixture(
                        Position(mask(c[2]), mask(c[3]), side, c[4].toInt(), c[5].toInt())
                    ) as Transition.Applied)
                    .state
            assertEquals(c[1], state.position.phase.name)
            val safe = c[6].split('|').map(::decode).toSet()
            val opponentWin = if (side == Side.LAMB) Outcome.TIGERS_WIN else Outcome.LAMBS_WIN
            // Check the independently enumerated fixture labels against all immediate replies.
            val verified =
                GameEngine.available(state)
                    .filter { action ->
                        val next = (GameEngine.apply(state, action) as Transition.Applied).state
                        next.outcome != opponentWin &&
                            GameEngine.available(next).none { reply ->
                                (GameEngine.apply(next, reply) as Transition.Applied)
                                    .state
                                    .outcome == opponentWin
                            }
                    }
                    .toSet()
            assertEquals(row, safe, verified)
            val choice = Computer.choose(state, Difficulty.EXPERT, 17017)!!
            assertTrue("$row: $choice", choice.action in safe)
        }
    }

    @Test
    fun twentyImmediateFifthCaptureFixtures() {
        for (path in Board.captures.take(20)) {
            val others =
                Node.entries.filter { it !in listOf(path.from, path.over, path.to) }.takeLast(2)
            val p =
                Position(
                    path.from.bit or others[0].bit or others[1].bit,
                    path.over.bit,
                    Side.TIGER,
                    10,
                    4,
                )
            val s = (GameEngine.fixture(p) as Transition.Applied).state
            val expected = Action.Capture(path.from, path.over, path.to)
            assertTrue(expected in GameEngine.available(s))
            val a = Computer.choose(s, Difficulty.EXPERT, 7)!!.action
            val next = (GameEngine.apply(s, a) as Transition.Applied).state
            assertEquals("${path.from}-${path.over}-${path.to}", Outcome.TIGERS_WIN, next.outcome)
        }
    }

    @Test
    fun lambdaCanCompleteTrapDuringPlacement() {
        val target = "BCFGHIJKLMNOPQR".fold(0) { m, c -> m or Node.valueOf(c.toString()).bit }
        for (node in listOf(Node.C, Node.F, Node.J, Node.K)) {
            val p = Position(GameEngine.opening.tigers, target xor node.bit, Side.LAMB, 1, 0)
            val s = (GameEngine.fixture(p) as Transition.Applied).state
            if (s.outcome == Outcome.ONGOING) {
                val result = Computer.choose(s, Difficulty.EXPERT, 9)!!
                assertEquals(
                    Outcome.LAMBS_WIN,
                    (GameEngine.apply(s, result.action) as Transition.Applied).state.outcome,
                )
            }
        }
    }
}
