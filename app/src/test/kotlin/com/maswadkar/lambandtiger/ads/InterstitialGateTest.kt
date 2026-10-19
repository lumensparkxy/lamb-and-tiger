package com.maswadkar.lambandtiger.ads

import org.junit.Assert.*
import org.junit.Test

class InterstitialGateTest {
    private var now = 1_000_000L
    private var last = 0L
    private var calls = 0
    private val attempted = mutableSetOf<String>()
    private fun gate() = InterstitialGate({ now }, { last }, { last = it }, attempted)

    @Test fun unavailableAdContinuesImmediatelyAndCannotAppearLate() {
        val gate = gate()
        assertFalse(gate.begin("a", "a", false) { calls++ })
        assertEquals(1, calls)
        gate.finish("a", "a")
        assertEquals(1, calls)
    }
    @Test fun duplicateTapsAndSdkCallbacksContinueExactlyOnce() {
        val gate = gate()
        assertTrue(gate.begin("a", "a", true) { calls++ })
        assertFalse(gate.begin("a", "a", true) { calls++ })
        gate.shown(); gate.finish("a", "a"); gate.finish("a", "a")
        assertEquals(1, calls)
        assertEquals(now, last)
    }
    @Test fun navigationOrNewMatchDropsOldContinuation() {
        val gate = gate()
        assertTrue(gate.begin("a", "a", true) { calls++ })
        gate.finish("a", "b")
        assertEquals(0, calls)
        assertFalse(gate.begin("a", null, true) { calls++ })
        assertEquals(0, calls)
    }
    @Test fun cooldownAndClockRollbackSkipWithoutBlocking() {
        last = now
        assertFalse(gate().begin("a", "a", true) { calls++ })
        now -= 20_000L
        assertFalse(gate().begin("b", "b", true) { calls++ })
        now = last + 120_000L
        assertTrue(gate().begin("c", "c", true) { calls++ })
        assertEquals(2, calls)
    }
    @Test fun recreationCancelsOldActivityAndDoesNotShowSameMatchAgain() {
        val gate = gate()
        assertTrue(gate.begin("a", "a", true) { calls++ })
        gate.cancel(); gate.finish("a", "a")
        assertEquals(0, calls)
        assertFalse(gate().begin("a", "a", true) { calls++ })
        assertEquals(1, calls)
    }
    @Test fun lateOldCallbackCannotCompleteANewAd() {
        val gate = gate()
        assertTrue(gate.begin("a", "a", true) { calls++ })
        gate.finish("a", "a")
        assertTrue(gate.begin("b", "b", true) { calls++ })
        gate.finish("a", "b")
        assertEquals(1, calls)
        gate.finish("b", "b")
        assertEquals(2, calls)
    }
    @Test fun failedShowContinuesWithoutRecordingAnImpression() {
        val gate = gate()
        assertTrue(gate.begin("a", "a", true) { calls++ })
        gate.finish("a", "a")
        assertEquals(1, calls)
        assertEquals(0L, last)
    }
}
