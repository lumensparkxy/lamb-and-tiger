package com.maswadkar.lambandtiger.friend

import com.maswadkar.lambandtiger.data.MatchCodec
import com.maswadkar.lambandtiger.game.*

/** Serialized app-owned coordinator. Persistence completes before any corresponding outbound receipt. */
internal class FriendSession(private val store: FriendPersistence, val host: Boolean, resume: Boolean,
    private val send: (Packet) -> Unit) {
    val player = store.player
    var record: FriendRecord? = if (resume) store.load() ?: error("Missing Nearby save") else null
        private set
    private val resumeId = record?.match?.id.orEmpty()
    private val nonce = randomSecret()
    private var peerNonce = ""
    private var peerId = ""
    private var session = ""
    var authenticated = false; private set
    var synced = false; private set
    var paused = true; private set
    var localReady = false; private set
    var peerReady = false; private set
    var localSide: Side? = null; private set
    var peerSide: Side? = null; private set
    var control = 0L; private set
    private var pausePending = false
    var offer: String? = null; private set
    var offerer: String? = null; private set
    var failure: String? = null; private set
    private var remoteSummary: FriendRecord? = null
    private var proofSent = false
    private var previousMatch = ""
    val match get() = record?.takeIf { it.started && (it.tx == null || it.tx!!.stage >= Stage.SETTLED) }?.match
    val busy get() = record?.tx?.stage?.let { it != Stage.ACKED } == true
    val playable get() = authenticated && synced && !paused && !busy && failure == null && offer == null && match?.state?.outcome == Outcome.ONGOING
    private val hostId get() = if (host) player else peerId
    private val guestId get() = if (host) peerId else player
    private fun persist(value: FriendRecord, promote: Boolean = false) { store.save(value,promote); record = value }
    fun hello() = send(Packet(Kind.HELLO, sender=player, fields=listOf(nonce, if(host) "HOST" else "GUEST", resumeId, GameEngine.RULESET)))
    fun disconnect() { authenticated=false; synced=false; paused=true; localReady=false; peerReady=false; offer=null; offerer=null }
    fun block(reason: String) { failure=reason; disconnect() }
    private fun packet(kind: Kind, fields: List<String> = emptyList(), data: ByteArray = byteArrayOf(), request: String = uid()): Packet {
        val r = record
        return Packet(kind,session,r?.match?.id.orEmpty(),player,request,r?.sequence ?: 0,r?.match?.state?.eventCount ?: 0,
            r?.match?.digest().orEmpty(),fields,data)
    }
    private fun emit(kind: Kind, fields: List<String> = emptyList(), data: ByteArray = byteArrayOf(), request: String = uid()) = send(packet(kind,fields,data,request))
    fun receive(p: Packet) {
        check(failure == null)
        if (p.kind == Kind.HELLO) { onHello(p); return }
        require(peerId.isNotEmpty() && p.sender == peerId && p.session == session) { "Wrong session" }
        if (p.kind == Kind.PAIR) {
            require(!host && !authenticated && resumeId.isEmpty() && record == null && p.fields.size == 1)
            val m = FriendMatch.decode(p.data)
            require(m.host == peerId && m.guest == player && m.state.eventCount == 0)
            require(p.fields[0].matches(Regex("[0-9a-f]{64}")))
            persist(FriendRecord(m,p.fields[0])); sendProof(); return
        }
        val r = record ?: error("Missing pairing")
        if(p.match==previousMatch && p.kind in listOf(Kind.HEARTBEAT,Kind.REMATCH)) return
        require(p.match == r.match.id) { "Wrong match" }
        if (p.kind == Kind.PROOF) {
            require(p.fields.size == 1 && !authenticated)
            require(equalSecret(p.fields[0], proof(r.secret, proofContext(peerId)))) { "Wrong saved opponent" }
            authenticated=true; emit(Kind.AUTH_OK); return
        }
        require(authenticated) { "Unauthenticated message" }
        when(p.kind) {
            Kind.AUTH_OK -> emit(Kind.SYNC, data=r.copy(secret="0".repeat(64)).bytes())
            Kind.SYNC -> sync(FriendRecord.decode(p.data))
            Kind.SIDE -> { require(host && synced && !r.started && !busy); selectPeer(Side.valueOf(p.fields.single())) }
            Kind.SETUP -> {
                require(!host && !r.started && !busy)
                val gen=p.fields[0].toLong(); require(gen >= control)
                control=gen; peerSide=p.fields[1].takeIf { it.isNotEmpty() }?.let(Side::valueOf)
                localSide=p.fields[2].takeIf { it.isNotEmpty() }?.let(Side::valueOf)
                localReady=false; peerReady=false; paused=true; synced=true
            }
            Kind.READY -> onReady(p)
            Kind.CANCEL_READY -> { if(host) pauseHost() else { localReady=false; peerReady=false; paused=true } }
            Kind.RESUME -> {
                require(!host && synced && p.fields.single().toLong()==control && localReady && !pausePending && sameBase(p) && !busy)
                peerReady=true; paused=false
            }
            Kind.PAUSE -> { require(host); pauseHost() }
            Kind.CONTROL -> {
                require(!host)
                val gen=p.fields.single().toLong(); require(gen >= control)
                control=gen; paused=true; localReady=false; peerReady=false; pausePending=false; offer=null; offerer=null; synced=true
            }
            Kind.ACTION -> { require(host); actionRequest(p) }
            Kind.PREPARE -> prepared(p)
            Kind.PREPARED -> {
                require(host); val tx=matchingTx(p)
                require(p.fields.single()==tx.receipt("prepared-peer"))
                if(tx.stage==Stage.PREPARED) persist(r.copy(match=tx.candidate,sequence=tx.sequence,started=true,tx=tx.copy(stage=Stage.COMMITTED)),true)
                emit(Kind.COMMIT,listOf(record!!.tx!!.receipt("commit-host")),request=tx.request)
            }
            Kind.COMMIT -> {
                require(!host); val tx=matchingTx(p)
                require(p.fields.single()==tx.receipt("commit-host"))
                if(tx.stage==Stage.PREPARED) persist(r.copy(match=tx.candidate,sequence=tx.sequence,started=true,tx=tx.copy(stage=Stage.COMMITTED)),true)
                emit(Kind.COMMITTED,listOf(tx.receipt("commit-peer")),request=tx.request)
            }
            Kind.COMMITTED -> {
                require(host); val tx=matchingTx(p); require(tx.stage>=Stage.COMMITTED && p.fields.single()==tx.receipt("commit-peer"))
                if(tx.stage<Stage.SETTLED) persist(r.copy(tx=tx.copy(stage=Stage.SETTLED)))
                emit(Kind.SETTLED,listOf(tx.receipt("commit-host"),tx.receipt("commit-peer")),request=tx.request)
            }
            Kind.SETTLED -> {
                require(!host); val tx=matchingTx(p); require(tx.stage>=Stage.COMMITTED)
                require(p.fields==listOf(tx.receipt("commit-host"),tx.receipt("commit-peer")))
                if(tx.stage<Stage.SETTLED) persist(r.copy(tx=tx.copy(stage=Stage.SETTLED)))
                emit(Kind.SETTLED_ACK,listOf(tx.identity()),request=tx.request)
            }
            Kind.SETTLED_ACK -> {
                require(host); val tx=matchingTx(p); require(tx.stage>=Stage.SETTLED && p.fields.single()==tx.identity())
                persist(r.copy(tx=tx.copy(stage=Stage.ACKED)))
                emit(Kind.OPEN,listOf(tx.identity()),request=tx.request)
            }
            Kind.OPEN -> {
                require(!host); val tx=matchingTx(p); require(tx.stage>=Stage.SETTLED && p.fields.single()==tx.identity())
                persist(r.copy(tx=tx.copy(stage=Stage.ACKED)))
            }
            Kind.DRAW_OFFER -> onOffer(p)
            Kind.DRAW_ACCEPT -> {
                require(host)
                if(offer==null) return // an earlier ordered cancellation/acceptance already won
                require(offer != null && p.fields.single()==offer && p.sender != offerer && sameBase(p) && !busy)
                val offerEvidence=offer!! + ":" + offerer!! + ":" + p.sender
                transact(p.request,p.sender,"DRAW:$offerEvidence",r.match.copy(state=(GameEngine.completeAgreedDraw(r.match.state) as Transition.Applied).state))
                offer=null; offerer=null
            }
            Kind.DRAW_CANCEL -> {
                if(offer==null) return
                require(offer != null && p.fields.single()==offer)
                if(host) pauseHost() else { paused=true; offer=null; offerer=null; localReady=false; peerReady=false }
            }
            Kind.REMATCH -> {
                require(synced && !busy && r.started && r.match.state.outcome!=Outcome.ONGOING && sameBase(p))
                if(host) rematch(p.fields.single()=="SWITCH")
                else {
                    val next=FriendMatch.decode(p.data)
                    require(next.id!=r.match.id && next.host==r.match.host && next.guest==r.match.guest && next.state.eventCount==0)
                    previousMatch=r.match.id
                    persist(FriendRecord(next,r.secret))
                    localSide=next.side(player); peerSide=next.side(peerId)
                    localReady=false; peerReady=false; paused=true; offer=null; offerer=null
                }
            }
            Kind.HEARTBEAT -> Unit
            else -> error("Unexpected message")
        }
    }
    private fun onHello(p: Packet) {
        require(peerId.isEmpty() && p.sender != player && p.fields.size==4)
        require(p.fields[0].matches(Regex("[0-9a-f]{64}")) && p.fields[1] == if(host) "GUEST" else "HOST")
        require(p.fields[2]==resumeId && p.fields[3]==GameEngine.RULESET)
        peerId=p.sender; peerNonce=p.fields[0]
        session=if(host) "$nonce:$peerNonce" else "$peerNonce:$nonce"
        if(resumeId.isNotEmpty()) {
            val m=record!!.match
            require(m.host==hostId && m.guest==guestId) { "Wrong original host or opponent" }
            localSide=m.side(player); peerSide=m.side(peerId); sendProof()
        } else if(host) {
            val r=FriendRecord(FriendMatch(uid(),player,peerId,Side.LAMB,GameEngine.newGame()),randomSecret())
            persist(r); emit(Kind.PAIR,listOf(r.secret),r.match.bytes()); sendProof()
        }
    }
    private fun proofContext(sender: String) = "1|${record!!.match.id}|$hostId|$guestId|$session|${if(sender==hostId) "HOST" else "GUEST"}"
    private fun sendProof() { check(!proofSent); proofSent=true; emit(Kind.PROOF,listOf(proof(record!!.secret,proofContext(player)))) }
    private fun sameBase(p: Packet) = p.sequence==record!!.sequence && p.count==record!!.match.state.eventCount && p.digest==record!!.match.digest()
    private fun sync(other: FriendRecord) {
        val r=record!!
        require(other.match.id==r.match.id && other.match.host==r.match.host && other.match.guest==r.match.guest)
        remoteSummary=other
        if(!host) return // only original coordinator chooses the recovery transaction, never a newer timestamp
        val tx=r.tx
        if(tx != null && (tx.stage!=Stage.ACKED || other.tx?.stage!=Stage.ACKED)) {
            require(other.sequence <= r.sequence && other.sequence >= tx.sequence-1)
            require(other.tx == null || other.tx.sequence < tx.sequence || other.tx.identity()==tx.identity()) { "Divergent transaction" }
            synced=true; pauseHost(); sendPrepare(tx)
        } else {
            require(r.sequence==other.sequence && r.started==other.started && r.match.bytes().contentEquals(other.match.bytes())) { "Saved history disagreement" }
            synced=true
            if(r.started) pauseHost() else publishSetup()
        }
    }
    fun select(side: Side) {
        check(authenticated && synced && record?.started==false && !busy)
        if(host) { localSide=side; publishSetup() } else emit(Kind.SIDE,listOf(side.name))
    }
    private fun selectPeer(side: Side) { peerSide=side; publishSetup() }
    private fun publishSetup() {
        control++; localReady=false; peerReady=false; paused=true
        emit(Kind.SETUP,listOf(control.toString(),localSide?.name.orEmpty(),peerSide?.name.orEmpty()))
    }
    fun ready() {
        check(authenticated && synced && !busy && failure==null)
        check(record!!.started || (localSide != null && peerSide != null && localSide != peerSide))
        if(localReady) { localReady=false; if(host) pauseHost() else emit(Kind.CANCEL_READY); return }
        if(!record!!.started) {
            val r=record!!
            val candidate=r.match.copy(hostSide=if(host) localSide!! else peerSide!!)
            persist(r.copy(requests=r.requests+("ready:$session:$control" to candidate.digest())))
        }
        localReady=true
        if(host) { emit(Kind.READY,listOf(control.toString())); maybeStart() }
        else emit(Kind.READY,listOf(control.toString()))
    }
    private fun onReady(p: Packet) {
        require(synced && !busy && sameBase(p) && p.fields.single().toLong()==control && !pausePending)
        peerReady=true
        if(host) maybeStart()
    }
    private fun maybeStart() {
        if(!localReady || !peerReady) return
        val r=record!!
        if(!r.started) {
            require(localSide!=null && peerSide==localSide!!.other())
            paused=false
            transact(uid(),player,"START:$session:$control",r.match.copy(hostSide=localSide!!))
        } else { paused=false; emit(Kind.RESUME,listOf(control.toString())) }
    }
    fun pause() {
        paused=true; localReady=false; peerReady=false
        if(authenticated && synced) { if(host) pauseHost() else { pausePending=true; emit(Kind.PAUSE) } }
    }
    private fun pauseHost() {
        offer?.let { id -> val r=record!!; persist(r.copy(requests=r.requests+("cancel:$id" to r.match.digest()))) }
        control++; paused=true; localReady=false; peerReady=false; offer=null; offerer=null
        emit(Kind.CONTROL,listOf(control.toString()))
    }
    fun action(action: Action) {
        check(playable && match!!.side(player)==match!!.state.sideToMove)
        val bytes=MatchCodec.encodeAction(action)
        val p=packet(Kind.ACTION,data=bytes)
        // Retain the actor's exact intent before sending; it survives a dropped request/Prepare.
        val r=record!!; persist(r.copy(requests=r.requests + (p.request to hash(bytes))))
        if(host) actionRequest(p) else { paused=true; emit(Kind.ACTION,data=bytes,request=p.request) }
    }
    private fun actionRequest(p: Packet) {
        val r=record!!
        r.requests[p.request]?.let { require(it==hash(p.data)) { "Changed duplicate request" } }
        if(r.tx?.request==p.request) { sendPrepare(r.tx); return }
        require(playable && sameBase(p) && r.match.side(p.sender)==r.match.state.sideToMove)
        val action=MatchCodec.decodeAction(p.data)
        val next=(GameEngine.apply(r.match.state,action) as? Transition.Applied)?.state ?: error("Illegal move")
        transact(p.request,p.sender,"ACTION:"+p.data.hex(),r.match.copy(state=next))
    }
    private fun transact(request: String, actor: String, operation: String, next: FriendMatch) {
        val r=record!!; check(!busy)
        val tx=Transaction(request,r.sequence+1,actor,operation,r.match.digest(),next)
        persist(r.copy(tx=tx,requests=r.requests + (request to if(operation.startsWith("ACTION:")) hash(MatchCodec.encodeAction((next.state.events.last() as Event.Played).action)) else tx.identity())))
        sendPrepare(tx)
    }
    private fun sendPrepare(tx: Transaction) = emit(Kind.PREPARE,
        listOf(tx.sequence.toString(),tx.actor,tx.operation,tx.base,tx.identity()),tx.candidate.bytes(),tx.request)
    private fun prepared(p: Packet) {
        require(!host && synced)
        val r=record!!
        val candidate=FriendMatch.decode(p.data)
        val tx=Transaction(p.request,p.fields[0].toLong(),p.fields[1],p.fields[2],p.fields[3],candidate)
        require(tx.identity()==p.fields[4])
        val old=r.tx
        if(old?.request==tx.request) { require(old.identity()==tx.identity()); emit(Kind.PREPARED,listOf(tx.receipt("prepared-peer")),request=tx.request); return }
        require(!busy && tx.sequence==r.sequence+1 && tx.base==r.match.digest())
        require(candidate.id==r.match.id && candidate.host==r.match.host && candidate.guest==r.match.guest)
        val expected=when {
            tx.operation.startsWith("START:") -> {
                require(!r.started && r.requests["ready:"+tx.operation.removePrefix("START:")]==candidate.digest())
                if(localReady && peerReady && !pausePending) paused=false
                GameEngine.newGame()
            }
            tx.operation.startsWith("ACTION:") -> {
                require(r.started && r.match.hostSide==candidate.hostSide && r.match.side(tx.actor)==r.match.state.sideToMove)
                val action=(candidate.state.events.lastOrNull() as? Event.Played)?.action ?: error("Missing action")
                require(tx.operation=="ACTION:"+MatchCodec.encodeAction(action).hex())
                if(tx.actor==player) require(r.requests[tx.request]==hash(MatchCodec.encodeAction(action))) { "Missing local consent" }
                // A locally requested move temporarily blocked input, without invalidating readiness.
                if(localReady && peerReady && !pausePending) paused=false
                (GameEngine.apply(r.match.state,action) as? Transition.Applied)?.state ?: error("Illegal move")
            }
            tx.operation.startsWith("DRAW:") -> {
                val parts=tx.operation.split(':'); require(parts.size==4)
                val offerId=parts[1]; val from=parts[2]; val acceptedBy=parts[3]
                require(r.requests["offer:$offerId"]=="$from|${r.match.digest()}")
                require(tx.actor==acceptedBy && from!=acceptedBy && setOf(from,acceptedBy)==setOf(player,peerId))
                if(acceptedBy==player) require(r.requests["accept:$offerId"]==tx.request)
                offer=null; offerer=null
                (GameEngine.completeAgreedDraw(r.match.state) as? Transition.Applied)?.state ?: error("Invalid draw")
            }
            else -> error("Unknown operation")
        }
        require(MatchCodec.nearbyState(expected).contentEquals(MatchCodec.nearbyState(candidate.state))) { "Candidate disagreement" }
        persist(r.copy(tx=tx))
        emit(Kind.PREPARED,listOf(tx.receipt("prepared-peer")),request=tx.request)
    }
    private fun matchingTx(p: Packet): Transaction = record!!.tx!!.also {
        require(it.request==p.request && p.sequence in (it.sequence-1)..it.sequence && p.digest in listOf(it.base,it.candidate.digest()))
    }
    fun offerDraw() {
        check(playable && match!!.side(player)==match!!.state.sideToMove)
        val p=packet(Kind.DRAW_OFFER, fields=listOf(uid()))
        if(host) onOffer(p) else { paused=true; emit(Kind.DRAW_OFFER,p.fields) }
    }
    private fun onOffer(p: Packet) {
        if(host) {
            require(playable && sameBase(p) && record!!.match.side(p.sender)==record!!.match.state.sideToMove)
            offer=p.fields.single(); offerer=p.sender; paused=true; localReady=false; peerReady=false
            val r=record!!; persist(r.copy(requests=r.requests+("offer:$offer" to "$offerer|${r.match.digest()}")))
            emit(Kind.DRAW_OFFER,listOf(offer!!,offerer!!))
        } else {
            require(sameBase(p) && p.fields.size==2 && !busy)
            offer=p.fields[0]; offerer=p.fields[1]; paused=true; localReady=false; peerReady=false
            val r=record!!; persist(r.copy(requests=r.requests+("offer:$offer" to "$offerer|${r.match.digest()}")))
        }
    }
    fun answerDraw(accept: Boolean) {
        check(offer != null && authenticated)
        val p=packet(if(accept) Kind.DRAW_ACCEPT else Kind.DRAW_CANCEL,listOf(offer!!))
        if(accept) {
            check(offerer!=player)
            val r=record!!; persist(r.copy(requests=r.requests+("accept:$offer" to p.request)))
        }
        if(host) receiveLocalControl(p) else send(p)
    }
    private fun receiveLocalControl(p: Packet) {
        if(p.kind==Kind.DRAW_CANCEL) { pauseHost(); return }
        require(offerer!=player && !busy)
        val r=record!!
        transact(p.request,player,"DRAW:$offer:$offerer:$player",r.match.copy(state=(GameEngine.completeAgreedDraw(r.match.state) as Transition.Applied).state))
        offer=null; offerer=null
    }
    fun rematch(switchSides: Boolean) {
        val r=record!!
        check(authenticated && synced && !busy && r.started && r.match.state.outcome!=Outcome.ONGOING)
        if(!host) { emit(Kind.REMATCH,listOf(if(switchSides) "SWITCH" else "SAME")); return }
        val next=r.match.copy(id=uid(),hostSide=if(switchSides) r.match.hostSide.other() else r.match.hostSide,state=GameEngine.newGame())
        emit(Kind.REMATCH,data=next.bytes())
        previousMatch=r.match.id
        persist(FriendRecord(next,r.secret))
        localSide=next.side(player); peerSide=next.side(peerId)
        publishSetup()
    }
    fun heartbeat() { if(authenticated) emit(Kind.HEARTBEAT) }
}
