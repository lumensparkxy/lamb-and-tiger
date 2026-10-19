package com.maswadkar.lambandtiger.friend

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maswadkar.lambandtiger.game.*
import com.maswadkar.lambandtiger.nearby.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class FriendUi(
    val phase: String = "idle", val peers: Map<String,String> = emptyMap(), val code: String = "", val peerName: String = "",
    val saved: Boolean = false, val savedHost: Boolean = false, val match: FriendMatch? = null, val player: String = "",
    val localSide: Side? = null, val peerSide: Side? = null, val ready: Boolean = false, val peerReady: Boolean = false,
    val playable: Boolean = false, val busy: Boolean = false, val paused: Boolean = true, val selected: Node? = null,
    val leaving: Boolean = false, val offer: Boolean = false, val offerer: Boolean = false, val error: Boolean = false,
)
internal class FriendViewModel(app: Application) : AndroidViewModel(app), Transport.Listener {
    private val store=FriendStore(app)
    private val transport=CdmBluetoothTransport(app)
    val pairing = CompanionPairing(app)
    val label get()=transport.label
    private val main=Handler(Looper.getMainLooper())
    private val _ui=MutableStateFlow(FriendUi())
    val ui=_ui.asStateFlow()
    private val commands=Channel<() -> Unit>(Channel.UNLIMITED)
    private var session: FriendSession?=null
    private var channel: FramedChannel?=null
    @Volatile private var generation=0
    private var lastReceived=0L
    private var linkUp=false
    private var foreground=true
    private var activeScreen=false
    private var requestedHost=false
    private var continuing=false
    init {
        viewModelScope.launch(Dispatchers.IO) {
            try { for(command in commands) try { command() } catch(e: Exception) {
                if(e is CancellationException) throw e
                android.util.Log.w("NearbyGame", "Blocked: ${e.javaClass.simpleName}")
                session?.block(e.javaClass.simpleName)
                main.post { transport.stop(); linkUp=false; _ui.value=_ui.value.copy(phase="error",error=true,playable=false,busy=false,paused=true) }
            } } finally { store.close() }
        }
        enqueue { refreshSaved() }
        viewModelScope.launch {
            while(isActive) {
                delay(2000)
                if(linkUp) {
                    if(SystemClock.elapsedRealtime()-lastReceived > 6000) { stopped("HeartbeatTimeout") }
                    else {
                        channel?.retransmit()
                        enqueue { session?.heartbeat() }
                    }
                }
            }
        }
    }
    private fun enqueue(block: () -> Unit) { commands.trySend(block) }
    private fun refreshSaved() {
        val saved=store.load()
        _ui.value=_ui.value.copy(saved=saved!=null,savedHost=saved?.match?.host==store.player,player=store.player)
    }
    private fun publish() {
        val s=session ?: return
        val old=_ui.value
        _ui.value=old.copy(phase=if(s.authenticated && s.synced) "session" else old.phase,
            match=s.match, player=s.player,localSide=s.localSide,peerSide=s.peerSide,ready=s.localReady,peerReady=s.peerReady,
            playable=s.playable && foreground && activeScreen,busy=s.busy,paused=s.paused,offer=s.offer!=null,offerer=s.offerer==s.player,
            saved=old.saved || s.record?.started==true, savedHost=s.record?.match?.host==s.player)
    }
    fun enter() { activeScreen=true }
    fun leave() { pairing.cancel(); CompanionPairing.clearAssociations(getApplication()); activeScreen=false; _ui.value=_ui.value.copy(leaving=false); stop() }
    fun foreground(value: Boolean) { foreground=value; if(!value && activeScreen) stop() }
    fun permissionDenied() { _ui.value=_ui.value.copy(phase="permission",error=true) }
    fun start(host: Boolean, resume: Boolean, companion: String? = null) {
        if(!foreground || !activeScreen) return
        transport.stop(); channel=null; linkUp=false; generation++
        val ticket=generation
        requestedHost=host; continuing=resume
        _ui.value=_ui.value.copy(phase=if(host) "hosting" else "joining",peers=emptyMap(),code="",error=false,match=null,selected=null)
        enqueue {
            val saved=store.load()
            check(resume || saved?.tx == null || saved.tx.stage==Stage.ACKED) { "Unresolved save must be recovered" }
            session=FriendSession(store,host,resume) { packet ->
                val bytes=packet.bytes()
                main.post { if(ticket==generation && linkUp) runCatching { channel?.enqueue(bytes) }.onFailure { stopped("Backpressure") } }
            }
            main.post { if(ticket==generation && foreground && activeScreen) run { transport.selectCompanion(companion); transport.start(host,this) } }
        }
    }
    fun stop() {
        transport.stop(); linkUp=false; channel=null; generation++
        _ui.value=_ui.value.copy(phase="idle",peers=emptyMap(),code="",playable=false,paused=true,ready=false,peerReady=false,selected=null)
        enqueue { session?.disconnect(); refreshSaved() }
    }
    fun requestLeave(): Boolean {
        val s=_ui.value
        if(s.match?.state?.outcome==Outcome.ONGOING || s.busy) {
            _ui.value=s.copy(leaving=true,playable=false,paused=true)
            pause(); return true
        }
        stop(); return false
    }
    fun cancelLeave() { _ui.value=_ui.value.copy(leaving=false) }
    fun connect(id: String) { transport.connect(id); _ui.value=_ui.value.copy(phase="connecting",peers=emptyMap()) }
    fun confirm() { transport.confirm(); _ui.value=_ui.value.copy(phase="confirm_wait",code="") }
    override fun peer(id: String,name: String,found: Boolean) {
        _ui.value=_ui.value.copy(peers=if(found) _ui.value.peers+(id to name) else _ui.value.peers-id)
    }
    override fun confirmation(name: String,code: String) { _ui.value=_ui.value.copy(phase="confirm",peerName=name,code=code,peers=emptyMap()) }
    override fun connected() {
        linkUp=true; lastReceived=SystemClock.elapsedRealtime(); _ui.value=_ui.value.copy(phase="sync",code="")
        val ticket=generation
        channel=FramedChannel({ transport.send(it) }) { bytes ->
            enqueue { if(ticket==generation) { session!!.receive(Packet.decode(bytes)); publish() } }
        }
        enqueue { session!!.hello() }
    }
    override fun bytes(value: ByteArray) {
        lastReceived=SystemClock.elapsedRealtime()
        runCatching { channel?.accept(value) }.onFailure { stopped("InvalidFrame") }
    }
    override fun stopped(reason: String) {
        android.util.Log.w("NearbyGame", "Transport stopped: $reason")
        stop(); _ui.value=_ui.value.copy(phase=when(reason) {
            "DiscoveryTimeout" -> "discovery_timeout"
            "ConnectionTimeout" -> "connection_timeout"
            "ConfirmationTimeout" -> "confirmation_timeout"
            else -> "disconnected"
        },error=true)
    }
    fun select(side: Side) = enqueue { session!!.select(side); publish() }
    fun ready() = enqueue { session!!.ready(); publish() }
    fun pause() = enqueue { session?.pause(); publish() }
    fun rematch(switchSides: Boolean) = enqueue { session!!.rematch(switchSides); publish() }
    fun offer() = enqueue { session!!.offerDraw(); publish() }
    fun answer(accept: Boolean) = enqueue { session!!.answerDraw(accept); publish() }
    fun tap(node: Node) {
        val s=_ui.value; val m=s.match ?: return
        if(!s.playable || m.state.sideToMove!=m.side(s.player)) return
        val actions=GameEngine.available(m.state)
        val action=actions.firstOrNull { it.destination==node && (it is Action.Place || it.origin==s.selected) }
        if(action!=null) {
            _ui.value=s.copy(playable=false,selected=null)
            enqueue { session!!.action(action); publish() }
        } else _ui.value=s.copy(selected=if(s.selected==node) null else node.takeIf { n -> actions.any { it.origin==n } })
    }
    override fun onCleared() { generation++; linkUp=false; pairing.cancel(); transport.close(); CompanionPairing.clearAssociations(getApplication()); commands.close(); /* actor drains before closing storage */
    }
}
