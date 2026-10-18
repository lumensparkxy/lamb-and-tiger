package com.maswadkar.lambandtiger.data

import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Test

class MatchCodecTest {
    private fun match() =
        SoloMatch(
            "00000000-0000-0000-0000-000000000001",
            Side.LAMB,
            Difficulty.EASY,
            0,
            GameEngine.newGame(),
        )

    @Test
    fun openingGoldenDigest() {
        assertEquals(
            "868a653d86479173c1bfdfd07a6798dc11f710bde963e93764669f73db9cdf48",
            MatchCodec.digest(GameEngine.newGame()),
        )
        assertArrayEquals(byteArrayOf(1, 22), MatchCodec.encodeAction(Action.Place(Node.W)))
        assertArrayEquals(
            byteArrayOf(3, 0, 2, 8),
            MatchCodec.encodeAction(Action.Capture(Node.A, Node.C, Node.I)),
        )
    }

    @Test
    fun roundTripAndCanonicalReplay() {
        var m = match()
        repeat(30) {
            if (m.state.outcome == Outcome.ONGOING)
                m =
                    m.copy(
                        revision = m.revision + 1,
                        state =
                            (GameEngine.apply(m.state, GameEngine.available(m.state).first())
                                    as Transition.Applied)
                                .state,
                    )
        }
        val bytes = MatchCodec.encode(m)
        val restored = MatchCodec.decode(bytes)
        assertEquals(m.id, restored.id)
        assertEquals(m.human, restored.human)
        assertEquals(m.difficulty, restored.difficulty)
        assertEquals(m.state.events, restored.state.events)
        assertEquals(m.state.repetitions, restored.state.repetitions)
        assertArrayEquals(bytes, MatchCodec.encode(restored))
        assertEquals(MatchCodec.digest(m.state), MatchCodec.digest(restored.state))
    }

    @Test
    fun rejectsTruncationVersionTrailingAndCorruption() {
        val bytes = MatchCodec.encode(match())
        for (n in 0 until bytes.size) {
            try {
                MatchCodec.decode(bytes.copyOf(n))
                fail("accepted truncation $n")
            } catch (_: Exception) {}
        }
        for (b in
            listOf(
                bytes + 0,
                bytes.clone().also { it[3] = 2 },
                bytes.clone().also { it[it.lastIndex] = 9 },
            )) {
            try {
                MatchCodec.decode(b)
                fail("accepted corruption")
            } catch (_: Exception) {}
        }
    }

    @Test
    fun allActionsRoundTripAndInvalidNodeRejected() {
        for (a in
            listOf(
                Action.Place(Node.W),
                Action.Step(Node.A, Node.C),
                Action.Capture(Node.A, Node.C, Node.I),
            )) assertEquals(a, MatchCodec.decodeAction(MatchCodec.encodeAction(a)))
        try {
            MatchCodec.decodeAction(byteArrayOf(1, 99))
            fail()
        } catch (_: Exception) {}
    }

    @Test
    fun fixtureCannotEscapeIntoSave() {
        val fixture = (GameEngine.fixture(GameEngine.opening) as Transition.Applied).state
        try {
            MatchCodec.encode(match().copy(state = fixture))
            fail()
        } catch (_: IllegalArgumentException) {}
    }
}
