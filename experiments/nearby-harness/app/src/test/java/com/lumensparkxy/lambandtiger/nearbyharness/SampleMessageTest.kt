package com.lumensparkxy.lambandtiger.nearbyharness

import org.junit.Assert.*
import org.junit.Test

class SampleMessageTest {
    private val id = "12345678-1234-1234-1234-123456789abc"
    @Test fun samplesAndReceiptsRoundTrip() {
        for (kind in listOf("SAMPLE", "ACK")) {
            val message = SampleMessage(kind, id)
            assertEquals(message, SampleMessage.decode(message.encode()))
        }
    }
    @Test fun malformedOrUnknownWireDataIsRejected() {
        for (wire in listOf("", "LT-HARNESS-2|SAMPLE|$id", "LT-HARNESS-1|MOVE|$id",
                "LT-HARNESS-1|SAMPLE|bad", "LT-HARNESS-1|ACK|$id|extra", "x".repeat(129)))
            assertNull(SampleMessage.decode(wire.toByteArray()))
    }
    @Test fun onlyKnownPendingSamplesCanBeAcknowledged() {
        val ledger = SampleLedger()
        assertFalse(ledger.acknowledge(id))
        ledger.pending.add(id)
        assertTrue(ledger.acknowledge(id))
        assertFalse(ledger.acknowledge(id))
        assertTrue(ledger.receive(id))
        assertFalse(ledger.receive(id))
    }
    @Test fun reconnectNeverReplaysUncertainSamples() {
        val ledger = SampleLedger()
        ledger.pending.add(id)
        ledger.clear()
        assertTrue(ledger.pending.isEmpty())
        assertFalse(ledger.acknowledge(id))
    }
}
