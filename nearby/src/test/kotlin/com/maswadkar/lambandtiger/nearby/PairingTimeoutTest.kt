package com.maswadkar.lambandtiger.nearby

import org.junit.Assert.*
import org.junit.Test

class PairingTimeoutTest {
    private var now = 0L
    private val pending = mutableListOf<Pair<Long, () -> Unit>>()
    private val failures = mutableListOf<String>()
    private val timeout = PairingTimeout({ delay, task -> pending += (now + delay) to task }, failures::add)
    private fun advance(milliseconds: Long) {
        now += milliseconds
        val due = pending.filter { it.first <= now }
        pending.removeAll(due.toSet())
        due.forEach { it.second() }
    }
    @Test fun lateSelectionAndConfirmationGetFreshBudgets() {
        timeout.enter(PairingTimeout.Stage.SEARCH)
        advance(59_000)
        timeout.enter(PairingTimeout.Stage.CONNECTION)
        advance(59_000)
        assertTrue(failures.isEmpty())
        timeout.enter(PairingTimeout.Stage.CONFIRMATION)
        advance(59_000)
        assertTrue(failures.isEmpty())
        advance(1_000)
        assertEquals(listOf("ConfirmationTimeout"), failures)
    }
    @Test fun stopOrConnectedCancelsAllEarlierStages() {
        timeout.enter(PairingTimeout.Stage.SEARCH)
        timeout.enter(PairingTimeout.Stage.CONNECTION)
        timeout.enter(PairingTimeout.Stage.CONFIRMATION)
        timeout.cancel()
        advance(120_000)
        assertTrue(failures.isEmpty())
    }
    @Test fun retryIgnoresPreviousSearchDeadline() {
        timeout.enter(PairingTimeout.Stage.SEARCH)
        advance(30_000)
        timeout.cancel()
        timeout.enter(PairingTimeout.Stage.SEARCH)
        advance(30_000)
        assertTrue(failures.isEmpty())
        advance(30_000)
        assertEquals(listOf("DiscoveryTimeout"), failures)
    }
    @Test fun stalledConnectionIsNotReportedAsDiscovery() {
        timeout.enter(PairingTimeout.Stage.CONNECTION)
        advance(60_000)
        assertEquals(listOf("ConnectionTimeout"), failures)
    }
}
