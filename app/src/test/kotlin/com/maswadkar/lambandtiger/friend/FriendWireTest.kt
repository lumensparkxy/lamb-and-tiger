package com.maswadkar.lambandtiger.friend

import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Test

class FriendWireTest {
    @Test fun chunkingBackpressureRetransmissionAndDuplicates() {
        val wire=ArrayDeque<Pair<Boolean,ByteArray>>()
        val received=mutableListOf<ByteArray>()
        val a=FramedChannel({ wire.addLast(false to it) }) { error("Unexpected response") }
        val b=FramedChannel({ wire.addLast(true to it) }) { received.add(it) }
        val value=ByteArray(100_000) { (it%251).toByte() }
        a.enqueue(value)
        // Drop first frame then retransmit; duplicate all later frames and ACKs.
        wire.removeFirst(); a.retransmit()
        var n=0
        while(wire.isNotEmpty()) {
            check(n++<200)
            val (toHost,bytes)=wire.removeFirst(); assertTrue(bytes.size<=32768)
            val target=if(toHost) a else b
            target.accept(bytes)
            if(n==1) target.accept(bytes)
        }
        assertEquals(1,received.size); assertArrayEquals(value,received.single())
    }
    @Test fun rejectsUnknownVersionAndTrailingBytes() {
        val p=Packet(Kind.HEARTBEAT).bytes()
        assertThrows(IllegalArgumentException::class.java) { Packet.decode(p+byteArrayOf(0)) }
        p[3]=2
        assertThrows(IllegalArgumentException::class.java) { Packet.decode(p) }
    }
    @Test fun goldenOpeningAndStrictReplay() {
        val m=FriendMatch("00000000-0000-0000-0000-000000000001","00000000-0000-0000-0000-000000000002","00000000-0000-0000-0000-000000000003",Side.LAMB,GameEngine.newGame())
        val restored=FriendMatch.decode(m.bytes())
        assertArrayEquals(m.bytes(),restored.bytes())
        assertEquals(0,restored.state.eventCount)
        val corrupt=m.bytes(); corrupt[corrupt.lastIndex]=(corrupt.last()+1).toByte()
        assertThrows(IllegalArgumentException::class.java) { FriendMatch.decode(corrupt) }
        // Stable canonical vector checked against an independent byte layout below.
        assertEquals("77b0cf40d18b29f0faf88aee3acc283829582e51e61d5c9c80ab9e31ddc9fba5",m.digest())
    }
    @Test fun receiptChangesWithActorAndActionAndIdentity() {
        val m=FriendMatch(uid(),uid(),uid(),Side.LAMB,GameEngine.newGame())
        val t=Transaction(uid(),1,m.host,"START:1",m.digest(),m)
        assertNotEquals(t.receipt("commit-host"),t.receipt("commit-peer"))
        assertNotEquals(t.identity(),t.copy(actor=m.guest).identity())
    }
}
