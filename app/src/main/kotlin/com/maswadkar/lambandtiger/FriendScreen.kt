package com.maswadkar.lambandtiger

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maswadkar.lambandtiger.friend.FriendViewModel
import com.maswadkar.lambandtiger.game.*
import com.maswadkar.lambandtiger.nearby.NearbyPermissions

@Composable
internal fun FriendScreen(model: FriendViewModel, preferences: Preferences, onHome: () -> Unit, onRules: () -> Unit, onSettings: () -> Unit) {
    val s by model.ui.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var host by remember { mutableStateOf(true) }
    var resume by remember { mutableStateOf(false) }
    var replace by remember { mutableStateOf(false) }
    var draw by remember { mutableStateOf(false) }
    val view=androidx.compose.ui.platform.LocalView.current
    var lastMatch by remember { mutableStateOf(s.match?.id) }
    var lastCount by remember { mutableIntStateOf(s.match?.state?.eventCount ?: 0) }
    LaunchedEffect(s.match?.id,s.match?.state?.eventCount) {
        val m=s.match
        if(m!=null && m.id==lastMatch && m.state.eventCount>lastCount) {
            if(preferences.vibration && m.state.position.turn.other()==m.side(s.player))
                view.performHapticFeedback(if(Build.VERSION.SDK_INT >= 30) android.view.HapticFeedbackConstants.CONFIRM else android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            if(preferences.sound) {
                val tone=runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC,35) }.getOrNull()
                if(tone!=null) try { tone.startTone(android.media.ToneGenerator.TONE_PROP_ACK,100); kotlinx.coroutines.delay(180) } finally { tone.release() }
            }
        }
        if(m!=null) { lastMatch=m.id; lastCount=m.state.eventCount }
    }
    var points by remember { mutableStateOf(false) }
    var moving by remember(s.match?.id) { mutableStateOf(false) }
    DisposableEffect(model) { model.enter(); onDispose { if((context as? android.app.Activity)?.isChangingConfigurations != true) model.leave() } }
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if(NearbyPermissions.runtimeFor(Build.VERSION.SDK_INT).all { context.checkSelfPermission(it)==PackageManager.PERMISSION_GRANTED })
            model.start(host,resume) else model.permissionDenied()
    }
    fun start(isHost: Boolean, continuing: Boolean) {
        host=isHost; resume=continuing
        val needed=NearbyPermissions.runtimeFor(Build.VERSION.SDK_INT)
        if(needed.all { context.checkSelfPermission(it)==PackageManager.PERMISSION_GRANTED }) model.start(host,resume)
        else permissions.launch(needed.toTypedArray())
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.play_friend),style=MaterialTheme.typography.headlineMedium)
        Text(stringResource(when(s.phase) {
            "hosting"->R.string.friend_hosting; "joining"->R.string.friend_joining; "confirm"->R.string.friend_compare
            "confirm_wait"->R.string.friend_confirm_wait; "connecting"->R.string.friend_connecting; "sync"->R.string.friend_sync
            "discovery_timeout"->R.string.friend_discovery_timeout; "connection_timeout"->R.string.friend_connection_timeout
            "confirmation_timeout"->R.string.friend_confirmation_timeout
            "permission"->R.string.friend_permission; "error"->R.string.friend_error; "disconnected"->R.string.friend_disconnected
            "session"->R.string.friend_connected; else->R.string.friend_intro
        }))
        if(s.phase in listOf("idle","permission","error","disconnected","discovery_timeout","connection_timeout","confirmation_timeout")) {
            if(s.saved) Button(onClick={ start(s.savedHost,true) }) { Text(stringResource(R.string.friend_continue)) }
            Button(onClick={ if(s.saved) { host=true; replace=true } else start(true,false) }) { Text(stringResource(R.string.friend_host)) }
            OutlinedButton(onClick={ if(s.saved) { host=false; replace=true } else start(false,false) }) { Text(stringResource(R.string.friend_join)) }
        }
        if(s.phase=="hosting") Text(model.label)
        s.peers.forEach { (id,name) -> OutlinedButton(onClick={ model.connect(id) }) { Text(name) } }
        if(s.code.isNotEmpty()) {
            Text(s.peerName); Text(s.code,style=MaterialTheme.typography.headlineLarge)
            Button(onClick=model::confirm) { Text(stringResource(R.string.friend_confirm)) }
            OutlinedButton(onClick=model::stop) { Text(stringResource(R.string.friend_reject)) }
        }
        if(s.phase=="session" && s.match==null && !s.busy) {
            Text(stringResource(R.string.friend_sides))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Side.entries.forEach { side -> FilterChip(selected=s.localSide==side,onClick={ model.select(side) },label={ Text(stringResource(if(side==Side.LAMB) R.string.lamb else R.string.tiger)) }) }
            }
            Text(stringResource(R.string.friend_peer_side, s.peerSide?.let { stringResource(if(it==Side.LAMB) R.string.lamb else R.string.tiger) } ?: stringResource(R.string.friend_choosing)))
            if(s.localSide!=null && s.localSide==s.peerSide) Text(stringResource(R.string.friend_side_conflict))
            Text(stringResource(R.string.friend_lambs_first))
            Button(onClick=model::ready,enabled=s.localSide!=null && s.peerSide!=null && s.localSide!=s.peerSide) {
                Text(stringResource(if(s.ready) R.string.friend_cancel_ready else R.string.friend_ready))
            }
            Text(stringResource(if(s.peerReady) R.string.friend_peer_ready else R.string.friend_peer_waiting))
        }
        s.match?.let { m ->
            val state=m.state
            val mine=m.side(s.player)
            Text(stringResource(if(mine==Side.LAMB) R.string.you_are_lambs else R.string.you_are_tigers))
            val myTurn=s.playable && state.sideToMove==mine
            val canAct=myTurn && !moving
            Text(stringResource(when {
                s.busy->R.string.friend_confirming_move
                state.outcome==Outcome.TIGERS_WIN->R.string.win_tigers
                state.outcome==Outcome.LAMBS_WIN->R.string.win_lambs
                state.outcome!=Outcome.ONGOING->R.string.draw
                s.paused->R.string.paused
                myTurn->R.string.your_turn
                else->R.string.friend_wait_turn
            }))
            Text(stringResource(R.string.reserve_compact,state.position.reserve)+" · "+stringResource(R.string.captured_compact,state.position.captured))
            key(m.id) {
                BoardView(state,s.selected,if(canAct) GameEngine.available(state) else emptyList(),null,model::tap,
                    Modifier.fillMaxWidth().aspectRatio(1.1f), (state.events.lastOrNull() as? Event.Played)?.action,
                    reducedMotion=preferences.reduced,motionEnabled=!s.paused,onMotionRunning={moving=it})
            }
            if(state.outcome==Outcome.ONGOING && s.phase=="session" && !s.busy) {
                if(s.offer) {
                    Text(stringResource(R.string.friend_draw_pending))
                    if(!s.offerer) Button(onClick={ model.answer(true) }) { Text(stringResource(R.string.friend_accept_draw)) }
                    OutlinedButton(onClick={ model.answer(false) }) { Text(stringResource(if(s.offerer) R.string.friend_withdraw else R.string.friend_decline)) }
                } else if(s.paused) {
                    Button(onClick=model::ready) { Text(stringResource(if(s.ready) R.string.friend_cancel_ready else R.string.resume)) }
                    Text(stringResource(if(s.peerReady) R.string.friend_peer_ready else R.string.friend_peer_waiting))
                } else {
                    OutlinedButton(onClick=model::pause) { Text(stringResource(R.string.friend_pause)) }
                    OutlinedButton(onClick={ draw=true },enabled=canAct) { Text(stringResource(R.string.friend_offer_draw)) }
                }
            }
            if(state.outcome!=Outcome.ONGOING) {
                Text(stringResource(when(state.outcome) { Outcome.TIGERS_WIN->R.string.reason_tigers; Outcome.LAMBS_WIN->R.string.reason_lambs; Outcome.REPETITION_DRAW->R.string.reason_repetition; else->R.string.reason_agreement }))
                if(s.phase=="session" && !s.busy) {
                    Button(onClick={ model.rematch(false) }) { Text(stringResource(R.string.play_again)) }
                    OutlinedButton(onClick={ model.rematch(true) }) { Text(stringResource(R.string.switch_sides)) }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={ model.pause(); onRules() }) { Text(stringResource(R.string.rules)) }
                TextButton(onClick={ model.pause(); onSettings() }) { Text(stringResource(R.string.settings)) }
            }
            TextButton(onClick={ points=!points }) { Text(stringResource(R.string.point_picker)) }
            if(points) PointPicker(state,s.selected,if(canAct) GameEngine.available(state) else emptyList(),canAct,model::tap)
        }
        if(s.busy) Text(stringResource(R.string.friend_confirming_move))
        if(s.phase !in listOf("idle","permission","error","disconnected","discovery_timeout","connection_timeout","confirmation_timeout"))
            TextButton(onClick=model::stop) { Text(stringResource(R.string.friend_disconnect)) }
    }
    if(s.leaving) AlertDialog(onDismissRequest=model::cancelLeave,
        title={ Text(stringResource(R.string.leave_title)) }, text={ Text(stringResource(R.string.friend_leave_body)) },
        confirmButton={ TextButton(onClick=onHome) { Text(stringResource(R.string.save_home)) } },
        dismissButton={ TextButton(onClick=model::cancelLeave) { Text(stringResource(R.string.keep_playing)) } })
    if(replace) AlertDialog(onDismissRequest={ replace=false },title={ Text(stringResource(R.string.replace_title)) },
        text={ Text(stringResource(R.string.friend_replace)) },
        confirmButton={ TextButton(onClick={ replace=false; start(host,false) }) { Text(stringResource(R.string.replace)) } },
        dismissButton={ TextButton(onClick={ replace=false }) { Text(stringResource(R.string.cancel)) } })
    if(draw) AlertDialog(onDismissRequest={ draw=false },text={ Text(stringResource(R.string.friend_draw_question)) },
        confirmButton={ TextButton(onClick={ draw=false; model.offer() }) { Text(stringResource(R.string.friend_send_offer)) } },
        dismissButton={ TextButton(onClick={ draw=false }) { Text(stringResource(R.string.cancel)) } })
}
