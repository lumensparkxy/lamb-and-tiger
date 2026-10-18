package com.maswadkar.lambandtiger.friend

import com.maswadkar.lambandtiger.game.*
import org.junit.Assert.*
import org.junit.Test

class FriendSessionTest {
    private class Memory(override val player: String=uid()) : FriendPersistence {
        var active: ByteArray?=null
        var recovery: ByteArray?=null
        var fail=false
        override fun load()=(recovery ?: active)?.let(FriendRecord::decode)
        override fun save(record: FriendRecord,promote: Boolean) {
            if(fail) error("Disk full")
            val bytes=record.bytes()
            if((record.tx!=null && record.tx.stage!=Stage.ACKED) || (!record.started && record.requests.keys.any { it.startsWith("ready:") })) recovery=bytes
            else recovery=null
            if(promote || active?.let { FriendRecord.decode(it).match.id==record.match.id }==true) active=bytes
        }
    }
    private class Pairing {
        val aStore=Memory(); val bStore=Memory()
        val queue=ArrayDeque<Pair<Boolean,Packet>>()
        var a=FriendSession(aStore,true,false) { queue.addLast(false to it) }
        var b=FriendSession(bStore,false,false) { queue.addLast(true to it) }
        fun pump(until: Kind?=null): Packet? {
            var n=0
            while(queue.isNotEmpty()) {
                check(n++ < 500)
                val (toHost,p)=queue.removeFirst()
                if(p.kind==until) { queue.addFirst(toHost to p); return p }
                (if(toHost) a else b).receive(Packet.decode(p.bytes()))
            }
            return null
        }
        fun begin() { a.hello(); b.hello(); pump(); a.select(Side.LAMB); pump(); b.select(Side.TIGER); pump(); a.ready(); pump(); b.ready(); pump() }
        fun restart() {
            queue.clear(); a.disconnect(); b.disconnect()
            a=FriendSession(aStore,true,true) { queue.addLast(false to it) }
            b=FriendSession(bStore,false,true) { queue.addLast(true to it) }
            a.hello(); b.hello(); pump()
        }
        fun resume() { a.ready(); pump(); b.ready(); pump() }
    }
    @Test fun bothReadyStartAndBothActorsMove() {
        val p=Pairing(); p.begin()
        assertTrue(p.a.playable); assertTrue(p.b.playable)
        p.a.action(Action.Place(Node.B)); p.pump()
        p.b.action(GameEngine.available(p.b.match!!.state).first()); p.pump()
        assertEquals(2,p.a.match!!.state.eventCount)
        assertArrayEquals(p.a.match!!.bytes(),p.b.match!!.bytes())
    }
    @Test fun completeGamesKeepEveryBoardAndOutcomeIdentical() {
        repeat(4) { seed ->
            val p=Pairing(); p.begin(); var moves=0
            while(p.a.match!!.state.outcome==Outcome.ONGOING && moves<800) {
                val m=p.a.match!!; val actor=if(m.state.sideToMove==m.side(p.a.player)) p.a else p.b
                val action=Computer.choose(m.state,Difficulty.VERY_EASY,seed.toLong()+moves)!!.action
                actor.action(action); p.pump(); moves++
                assertArrayEquals(p.a.match!!.bytes(),p.b.match!!.bytes())
            }
            assertNotEquals("seed $seed",Outcome.ONGOING,p.a.match!!.state.outcome)
        }
    }

    @Test fun sideConflictAndChangesClearReadiness() {
        val p=Pairing(); p.a.hello(); p.b.hello(); p.pump()
        p.a.select(Side.TIGER); p.pump(); p.b.select(Side.TIGER); p.pump()
        assertThrows(IllegalStateException::class.java) { p.a.ready() }
        p.b.select(Side.LAMB); p.pump(); p.a.ready(); p.pump()
        p.a.select(Side.LAMB); p.pump()
        assertFalse(p.a.localReady); assertFalse(p.b.peerReady); assertNull(p.a.match)
    }
    @Test fun restartAtEveryDurableBoundaryReconcilesWithoutDoubleMove() {
        for(boundary in listOf(Kind.PREPARED,Kind.COMMIT,Kind.COMMITTED,Kind.SETTLED,Kind.SETTLED_ACK,Kind.OPEN)) {
            val p=Pairing(); p.begin(); p.a.action(Action.Place(Node.B)); assertNotNull(p.pump(boundary))
            p.restart()
            assertFalse(p.a.playable); assertFalse(p.b.playable)
            p.resume()
            assertTrue("$boundary host",p.a.playable); assertTrue("$boundary guest",p.b.playable)
            assertEquals(1,p.a.match!!.state.eventCount)
            assertArrayEquals(p.a.match!!.bytes(),p.b.match!!.bytes())
        }
    }
    @Test fun startAndDrawRecoverAtEachReceiptBoundary() {
        for(boundary in listOf(Kind.PREPARE,Kind.PREPARED,Kind.COMMIT,Kind.COMMITTED,Kind.SETTLED,Kind.SETTLED_ACK,Kind.OPEN)) {
            val p=Pairing()
            p.a.hello(); p.b.hello(); p.pump(); p.a.select(Side.TIGER); p.pump(); p.b.select(Side.LAMB); p.pump()
            p.a.ready(); p.pump(); p.b.ready(); assertNotNull(p.pump(boundary)); p.restart()
            assertEquals(0,p.a.match!!.state.eventCount); assertArrayEquals(p.a.match!!.bytes(),p.b.match!!.bytes())
            p.resume(); p.b.offerDraw(); p.pump(); p.a.answerDraw(true); assertNotNull(p.pump(boundary)); p.restart()
            assertEquals(Outcome.AGREED_DRAW,p.a.match!!.state.outcome)
            assertArrayEquals(p.a.match!!.bytes(),p.b.match!!.bytes())
        }
    }
    @Test fun reverseActorMoveRecoversBeforePeerPrepare() {
        val p=Pairing(); p.begin(); p.a.action(Action.Place(Node.B)); p.pump()
        p.b.action(GameEngine.available(p.b.match!!.state).first()); p.pump(Kind.PREPARE)
        p.restart(); p.resume(); assertEquals(2,p.a.match!!.state.eventCount)
    }

    @Test fun duplicateEveryTransactionMessageIsIdempotent() {
        val p=Pairing(); p.begin(); p.a.action(Action.Place(Node.B))
        var steps=0
        while(p.queue.isNotEmpty()) {
            check(steps++<500)
            val (toHost,packet)=p.queue.removeFirst(); val receiver=if(toHost) p.a else p.b
            receiver.receive(packet)
            if(packet.kind in listOf(Kind.PREPARE,Kind.PREPARED,Kind.COMMIT,Kind.COMMITTED,Kind.SETTLED,Kind.SETTLED_ACK)) receiver.receive(packet)
        }
        assertEquals(1,p.a.match!!.state.eventCount); assertEquals(1,p.b.match!!.state.eventCount)
    }
    @Test fun staleSessionWrongSecretAndChangedCandidateFailClosed() {
        val p=Pairing(); p.begin(); p.a.action(Action.Place(Node.B)); val old=p.pump(Kind.PREPARE)!!; p.pump(); p.restart()
        assertThrows(IllegalArgumentException::class.java) { p.b.receive(old) }
        val record=p.bStore.load()!!; p.bStore.save(record.copy(secret="a".repeat(64)),true)
        assertThrows(IllegalArgumentException::class.java) { p.restart() }
    }
    @Test fun failedPrepareWriteDoesNotPublishOrAcknowledge() {
        val p=Pairing(); p.begin(); p.bStore.fail=true; p.a.action(Action.Place(Node.B))
        assertThrows(IllegalStateException::class.java) { p.pump() }
        assertEquals(0,p.b.match!!.state.eventCount)
        assertEquals(Stage.PREPARED,p.a.record!!.tx!!.stage)
    }
    @Test fun pauseInvalidatesDelayedResume() {
        val p=Pairing(); p.begin(); p.a.pause(); p.pump(); p.a.ready(); p.pump(); p.b.ready()
        val resume=p.pump(Kind.RESUME)!!
        p.b.pause()
        assertThrows(IllegalArgumentException::class.java) { p.b.receive(resume) }
        assertFalse(p.b.playable)
    }
    @Test fun drawRequiresExplicitOtherPlayerAcceptanceAndSettlesOnBoth() {
        val p=Pairing(); p.begin(); p.a.offerDraw(); p.pump()
        assertFalse(p.a.playable); assertFalse(p.b.playable)
        assertThrows(IllegalStateException::class.java) { p.a.answerDraw(true) }
        p.b.answerDraw(true); p.pump()
        assertEquals(Outcome.AGREED_DRAW,p.a.match!!.state.outcome)
        assertArrayEquals(p.a.match!!.bytes(),p.b.match!!.bytes())
    }
    @Test fun rematchRequiresFreshMutualReadinessAndPreservesPreviousResult() {
        val p=Pairing(); p.begin(); p.a.offerDraw(); p.pump(); p.b.answerDraw(true); p.pump()
        val old=p.a.match!!.id
        p.b.rematch(true); p.pump()
        assertNull(p.a.match); assertEquals(old,p.aStore.load()!!.match.id)
        p.a.ready(); p.pump(); assertNull(p.b.match)
        p.b.ready(); p.pump()
        assertNotEquals(old,p.a.match!!.id); assertEquals(Side.TIGER,p.a.match!!.hostSide)
        assertArrayEquals(p.a.match!!.bytes(),p.b.match!!.bytes())
    }

    @Test fun drawWithdrawalKeepsPositionAndRequiresBothResume() {
        val p=Pairing(); p.begin(); p.a.offerDraw(); p.pump(); p.a.answerDraw(false); p.pump()
        assertEquals(0,p.a.match!!.state.eventCount); assertFalse(p.a.playable)
        p.a.ready(); p.pump(); assertFalse(p.b.playable)
        p.b.ready(); p.pump(); assertTrue(p.a.playable); assertTrue(p.b.playable)
    }
}
