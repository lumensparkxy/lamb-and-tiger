package com.maswadkar.lambandtiger

import android.content.pm.PackageManager
import android.content.Intent
import android.bluetooth.BluetoothAdapter
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.withResumed
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.maswadkar.lambandtiger.friend.FriendUi
import com.maswadkar.lambandtiger.friend.FriendViewModel
import com.maswadkar.lambandtiger.game.*
import com.maswadkar.lambandtiger.nearby.NearbyPermissions

@Composable
internal fun FriendScreen(model: FriendViewModel, preferences: Preferences, onHome: () -> Unit, onRules: () -> Unit, onSettings: () -> Unit) {
    val s by model.ui.collectAsStateWithLifecycle()
    val ads = com.maswadkar.lambandtiger.ads.LocalAds.current
    val context=LocalContext.current
    var host by rememberSaveable { mutableStateOf(true) }
    var resume by rememberSaveable { mutableStateOf(false) }
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
    DisposableEffect(model) { model.enter(); onDispose { if((context as? android.app.Activity)?.isChangingConfigurations != true) model.leave() } }
    var companion by rememberSaveable { mutableStateOf<String?>(null) }
    var startPending by rememberSaveable { mutableStateOf(false) }
    var setupStage by rememberSaveable { mutableStateOf("") }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(startPending) {
        if (startPending) lifecycle.withResumed {
            startPending = false
            model.start(host, resume, companion)
        }
    }
    val discoverable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (setupStage == "discoverable") {
            setupStage = ""
            if (result.resultCode > 0) startPending = true else model.permissionDenied()
        }
    }
    fun afterPermissions() {
        if (host) runCatching {
            setupStage = "discoverable"
            discoverable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120))
        }.onFailure { model.permissionDenied() }
        else startPending = true
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (setupStage != "permission") return@rememberLauncherForActivityResult
        setupStage = ""
        if (NearbyPermissions.runtimeFor(Build.VERSION.SDK_INT, host).all {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) afterPermissions()
        else model.permissionDenied()
    }
    fun permissionStep() {
        val needed = NearbyPermissions.runtimeFor(Build.VERSION.SDK_INT, host)
        if (needed.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) afterPermissions()
        else { setupStage = "permission"; permissions.launch(needed.toTypedArray()) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (setupStage != "picker") return@rememberLauncherForActivityResult
        setupStage = ""
        companion = if (result.resultCode == android.app.Activity.RESULT_OK) model.pairing.result(result.data) else null
        if (companion != null) permissionStep() else { model.pairing.cancel(); model.permissionDenied() }
    }
    fun start(isHost: Boolean, continuing: Boolean) {
        model.stop(); model.pairing.cancel()
        host = isHost; resume = continuing; companion = null; startPending = false
        setupStage = ""
        if (host) permissionStep() else model.pairing.request(
            { setupStage = "picker"; picker.launch(IntentSenderRequest.Builder(it).build()) }, model::permissionDenied)
    }
    FriendContent(s, preferences, model.label, FriendActions(
        start = ::start, connect = model::connect, confirm = model::confirm, stop = model::stop,
        select = model::select, ready = model::ready, tap = model::tap, pause = model::pause,
        rematch = { switch ->
            val match = model.ui.value.match
            if (match != null && match.state.outcome != Outcome.ONGOING) {
                val proceed = { if (model.ui.value.match?.id == match.id) model.rematch(switch) }
                if (ads == null) proceed() else ads.betweenGames("nearby:${match.id}", proceed)
            }
        }, offer = model::offer, answer = model::answer,
        back = { if (!model.requestLeave()) onHome() }, home = onHome,
        cancelLeave = model::cancelLeave,
        rules = { model.pause(); onRules() }, settings = { model.pause(); onSettings() },
    ))
}

/** UI callbacks keep pairing, persistence and match authority in FriendViewModel. */
internal data class FriendActions(
    val start: (Boolean, Boolean) -> Unit = { _, _ -> },
    val connect: (String) -> Unit = {}, val confirm: () -> Unit = {}, val stop: () -> Unit = {},
    val select: (Side) -> Unit = {}, val ready: () -> Unit = {}, val tap: (Node) -> Unit = {},
    val pause: () -> Unit = {}, val rematch: (Boolean) -> Unit = {}, val offer: () -> Unit = {},
    val answer: (Boolean) -> Unit = {}, val back: () -> Unit = {}, val home: () -> Unit = {},
    val cancelLeave: () -> Unit = {}, val rules: () -> Unit = {}, val settings: () -> Unit = {},
)

private val retryPhases = setOf("idle", "permission", "error", "disconnected", "discovery_timeout",
    "connection_timeout", "confirmation_timeout")

private fun connectionMessage(phase: String) = when (phase) {
    "hosting" -> R.string.friend_hosting; "joining" -> R.string.friend_joining
    "confirm" -> R.string.friend_compare; "confirm_wait" -> R.string.friend_confirm_wait
    "connecting" -> R.string.friend_connecting; "sync" -> R.string.friend_sync
    "discovery_timeout" -> R.string.friend_discovery_timeout
    "connection_timeout" -> R.string.friend_connection_timeout
    "confirmation_timeout" -> R.string.friend_confirmation_timeout
    "permission" -> R.string.friend_permission; "error" -> R.string.friend_error
    "disconnected" -> R.string.friend_disconnected; "session" -> R.string.friend_connected
    else -> R.string.friend_intro
}

@Composable
internal fun FriendContent(snapshot: FriendUi, preferences: Preferences, label: String, actions: FriendActions) {
    // The coordinator withholds its match during an unsettled transaction. Keep displaying
    // the last confirmed position while busy, never an unconfirmed candidate or a setup screen.
    val s = if (snapshot.match == null &&
        ((snapshot.busy && snapshot.phase == "session") || snapshot.phase in retryPhases))
        snapshot.copy(match = snapshot.confirmedMatch) else snapshot
    var menu by remember { mutableStateOf(false) }
    var replaceHost by remember { mutableStateOf<Boolean?>(null) }
    var draw by remember { mutableStateOf(false) }
    var points by remember { mutableStateOf(false) }
    var pairing by rememberSaveable { mutableStateOf(false) }
    var moving by remember(s.match?.id) { mutableStateOf(false) }
    var resultDismissed by rememberSaveable(s.match?.id) { mutableStateOf(false) }
    var errorDismissed by remember(s.phase, s.error) { mutableStateOf(false) }
    var offerDismissed by remember(s.offer, s.offerer) { mutableStateOf(false) }
    val match = s.match.takeUnless { pairing }
    val finished = match != null && match.state.outcome != Outcome.ONGOING
    var resultReady by remember(match?.id, finished) { mutableStateOf(false) }
    LaunchedEffect(match?.id, finished) {
        if (finished) {
            if (!preferences.reduced) kotlinx.coroutines.delay(400)
            resultReady = true
        }
    }
    LaunchedEffect(s.match?.id, s.phase) { if (s.phase == "session") pairing = false }
    val canAct = match != null && s.playable && !s.busy && !s.paused && !s.offer &&
        s.phase == "session" && !finished && match.state.sideToMove == match.side(s.player) && !moving
    LaunchedEffect(canAct) { if (!canAct) draw = false }
    LaunchedEffect(match?.id, finished) { if (match == null || finished) points = false }
    fun start(host: Boolean, resume: Boolean) { pairing = true; actions.start(host, resume) }
    fun disconnect() { pairing = true; actions.stop() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = actions.back) {
                Icon(painterResource(R.drawable.ic_back), null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.back))
            }
            Text(stringResource(R.string.play_title), Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Box {
                TextButton(onClick = { menu = true }, modifier = Modifier.testTag("friend-menu")) {
                    Text(stringResource(R.string.game_menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (finished) DropdownMenuItem(text = { Text(stringResource(R.string.view_result)) },
                        onClick = { menu = false; resultDismissed = false })
                    if (match != null && !finished) DropdownMenuItem(
                        text = { Text(stringResource(R.string.friend_offer_draw)) }, enabled = canAct,
                        onClick = { menu = false; draw = true })
                    // Retain the existing accessible alternative without taking space from the board.
                    if (match != null) DropdownMenuItem(text = { Text(stringResource(R.string.point_picker)) },
                        onClick = { menu = false; points = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.rules)) }, enabled = !s.busy,
                        onClick = { menu = false; actions.rules() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.settings)) }, enabled = !s.busy,
                        onClick = { menu = false; actions.settings() })
                    if (s.phase !in retryPhases || match != null) DropdownMenuItem(
                        text = { Text(stringResource(R.string.friend_disconnect)) },
                        onClick = { menu = false; disconnect() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.save_home)) },
                        onClick = { menu = false; actions.back() })
                }
            }
        }
        if (match == null) {
            FriendPairing(s, label, actions, onStart = { host ->
                if (s.saved) replaceHost = host else start(host, false)
            }, onContinue = { start(s.savedHost, true) }, onStop = ::disconnect)
        } else {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val scrolling = maxHeight < 680.dp || LocalDensity.current.fontScale > 1.3f
                val boardHeight = (maxWidth - 36.dp) * .8f + 60.dp
                val railWidth = if (maxWidth >= 840.dp) 300.dp else 244.dp
                val board: @Composable (Modifier) -> Unit = { modifier ->
                    key(match.id) {
                        BoardView(match.state, s.selected,
                            if (canAct) GameEngine.available(match.state) else emptyList(), null, actions.tap,
                            modifier.testTag("friend-board"),
                            last = (match.state.events.lastOrNull() as? Event.Played)?.action,
                            immersive = true, reducedMotion = preferences.reduced,
                            motionEnabled = s.phase == "session" && !s.paused,
                            onMotionRunning = { moving = it }, showPointLabels = false)
                    }
                }
                val controls: @Composable (Modifier) -> Unit = { modifier ->
                    FriendMatchControls(s, finished, modifier,
                        onResult = { resultDismissed = false }, onOffer = { offerDismissed = false },
                        onContinue = { start(s.savedHost, true) }, onStop = ::disconnect, actions = actions)
                }
                if (maxWidth > maxHeight) Row(Modifier.fillMaxSize()) {
                    board(Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(railWidth).fillMaxHeight()
                        .verticalScroll(rememberScrollState()).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FriendIdentity(match.side(s.player))
                        FriendTurn(s)
                        TabletopCounts(match.state)
                        controls(Modifier)
                    }
                } else Column(Modifier.fillMaxSize().then(if (scrolling)
                    Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
                    FriendIdentity(match.side(s.player), Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                    FriendTurn(s, Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    board(if (scrolling) Modifier.fillMaxWidth().height(boardHeight)
                        else Modifier.fillMaxWidth().weight(1f))
                    TabletopCounts(match.state, Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
                    controls(Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                }
            }
        }
    }
    if (s.leaving) AlertDialog(onDismissRequest = actions.cancelLeave,
        title = { Text(stringResource(R.string.leave_title)) }, text = { Text(stringResource(R.string.friend_leave_body)) },
        confirmButton = { TextButton(onClick = actions.home) { Text(stringResource(R.string.save_home)) } },
        dismissButton = { TextButton(onClick = actions.cancelLeave) { Text(stringResource(R.string.keep_playing)) } })
    else if (s.error && s.phase != "idle" && !errorDismissed) AlertDialog(onDismissRequest = { errorDismissed = true },
        modifier = Modifier.testTag("friend-error-dialog"),
        text = { Text(stringResource(connectionMessage(s.phase)), Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { errorDismissed = true }) { Text(stringResource(android.R.string.ok)) } })
    else if (finished && resultReady && !resultDismissed) {
        FriendResult(s, actions, onDismiss = { resultDismissed = true }, onContinue = { start(s.savedHost, true) },
            onStop = ::disconnect)
    } else if (s.offer && !offerDismissed) AlertDialog(onDismissRequest = { offerDismissed = true },
        modifier = Modifier.testTag("friend-offer-dialog"),
        text = { Text(stringResource(R.string.friend_draw_pending)) },
        confirmButton = {
            if (!s.offerer) TextButton(onClick = { actions.answer(true) }, enabled = s.phase == "session" && !s.busy) {
                Text(stringResource(R.string.friend_accept_draw))
            }
        }, dismissButton = {
            TextButton(onClick = { actions.answer(false) }, enabled = s.phase == "session" && !s.busy) {
                Text(stringResource(if (s.offerer) R.string.friend_withdraw else R.string.friend_decline))
            }
        })
    if (replaceHost != null) AlertDialog(onDismissRequest = { replaceHost = null },
        title = { Text(stringResource(R.string.replace_title)) }, text = { Text(stringResource(R.string.friend_replace)) },
        confirmButton = { TextButton(onClick = { val host = replaceHost!!; replaceHost = null; start(host, false) }) {
            Text(stringResource(R.string.replace))
        } }, dismissButton = { TextButton(onClick = { replaceHost = null }) { Text(stringResource(R.string.cancel)) } })
    if (draw) AlertDialog(onDismissRequest = { draw = false }, text = { Text(stringResource(R.string.friend_draw_question)) },
        confirmButton = { TextButton(onClick = { draw = false; actions.offer() }, enabled = canAct) {
            Text(stringResource(R.string.friend_send_offer))
        } }, dismissButton = { TextButton(onClick = { draw = false }) { Text(stringResource(R.string.cancel)) } })
    if (points && match != null) Dialog(onDismissRequest = { points = false }) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                PointPicker(match.state, s.selected, if (canAct) GameEngine.available(match.state) else emptyList(), canAct, actions.tap)
                TextButton(onClick = { points = false }) { Text(stringResource(R.string.view_board)) }
            }
        }
    }
}

@Composable
private fun FriendPairing(s: FriendUi, label: String, actions: FriendActions,
    onStart: (Boolean) -> Unit, onContinue: () -> Unit, onStop: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.play_friend), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimalIcon(Side.LAMB, Modifier.size(48.dp)); AnimalIcon(Side.TIGER, Modifier.size(48.dp))
            Text(stringResource(R.string.play_friend_subtitle), style = MaterialTheme.typography.titleMedium)
        }
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .09f)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (s.busy && s.phase == "session") R.string.friend_confirming_move
                    else connectionMessage(s.phase)), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (s.phase == "hosting") Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (s.phase in setOf("hosting", "joining", "connecting", "sync", "confirm_wait") || s.busy)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                if (s.code.isNotEmpty()) {
                    Text(s.peerName, style = MaterialTheme.typography.titleMedium)
                    Text(s.code, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("friend-confirmation-code"))
                    Button(onClick = actions.confirm, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.friend_confirm))
                    }
                    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.friend_reject)) }
                }
            }
        }
        if (s.phase in retryPhases) {
            if (s.saved) Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.friend_continue))
            }
            FriendStartCard(R.string.friend_host, R.string.friend_host_description, true) { onStart(true) }
            FriendStartCard(R.string.friend_join, R.string.friend_join_description, false) { onStart(false) }
        }
        s.peers.forEach { (id, name) ->
            OutlinedButton(onClick = { actions.connect(id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(name) }
        }
        if (s.phase == "session" && s.match == null) {
            Text(stringResource(R.string.friend_sides), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = if (LocalDensity.current.fontScale > 1.3f) 1 else 2) {
                Side.entries.forEach { side ->
                    val selected = s.localSide == side
                    Surface(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .selectable(selected, enabled = !s.busy, role = Role.RadioButton,
                        onClick = { actions.select(side) }).testTag("friend-side-${side.name}"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                        Row(Modifier.heightIn(min = 64.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnimalIcon(side, Modifier.size(40.dp))
                            Text(stringResource(if (side == Side.LAMB) R.string.lamb else R.string.tiger), Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (selected) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                        }
                    }
                }
            }
            Text(stringResource(R.string.friend_peer_side, s.peerSide?.let {
                stringResource(if (it == Side.LAMB) R.string.lamb else R.string.tiger)
            } ?: stringResource(R.string.friend_choosing)))
            if (s.localSide != null && s.localSide == s.peerSide)
                Text(stringResource(R.string.friend_side_conflict), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.friend_lambs_first), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = actions.ready, enabled = !s.busy && s.localSide != null && s.peerSide != null && s.localSide != s.peerSide,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("friend-ready")) {
                Text(stringResource(if (s.ready) R.string.friend_cancel_ready else R.string.friend_ready))
            }
            Text(stringResource(if (s.peerReady) R.string.friend_peer_ready else R.string.friend_peer_waiting))
        }
        if (s.phase !in retryPhases && s.code.isEmpty()) TextButton(onClick = onStop) {
            Text(stringResource(R.string.friend_disconnect))
        }
    }
}

@Composable
private fun FriendStartCard(title: Int, description: Int, host: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag(if (host) "friend-host" else "friend-join"),
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Text(stringResource(description), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FriendIdentity(side: Side, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimalIcon(side, Modifier.size(36.dp)); Spacer(Modifier.width(8.dp))
            Text(stringResource(if (side == Side.LAMB) R.string.you_lambs_compact else R.string.you_tigers_compact),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimalIcon(side.other(), Modifier.size(36.dp)); Spacer(Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.play_friend), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(if (side.other() == Side.LAMB) R.string.lamb else R.string.tiger),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FriendTurn(s: FriendUi, modifier: Modifier = Modifier) {
    val match = s.match ?: return
    val state = match.state
    val prompt = when {
        s.phase != "session" -> stringResource(R.string.friend_disconnected_compact)
        s.busy -> stringResource(R.string.friend_confirming_move)
        state.outcome == Outcome.TIGERS_WIN -> stringResource(R.string.win_tigers)
        state.outcome == Outcome.LAMBS_WIN -> stringResource(R.string.win_lambs)
        state.outcome != Outcome.ONGOING -> stringResource(R.string.draw)
        s.offer -> stringResource(R.string.friend_draw_pending)
        s.paused -> stringResource(R.string.turn_prompt, stringResource(R.string.paused),
            stringResource(if (s.peerReady) R.string.friend_peer_ready else R.string.friend_peer_waiting))
        s.playable && state.sideToMove == match.side(s.player) -> stringResource(R.string.turn_prompt,
            stringResource(R.string.your_turn), stringResource(if (state.position.phase == Phase.PLACEMENT &&
                match.side(s.player) == Side.LAMB) R.string.place_compact else R.string.move_compact))
        else -> stringResource(R.string.friend_wait_turn)
    }
    val spoken = listOf(prompt, stringResource(if (state.position.phase == Phase.PLACEMENT)
        R.string.phase_placement else R.string.phase_movement), stringResource(R.string.reserve_count, state.position.reserve),
        stringResource(R.string.captured_count, state.position.captured)).joinToString(". ")
    val height = with(LocalDensity.current) { 22.sp.toDp() * 2 } + 12.dp
    Surface(modifier.fillMaxWidth().height(height).testTag("friend-turn"), shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = .09f), contentColor = MaterialTheme.colorScheme.primary) {
        key(prompt) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center) {
                Text(prompt, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { contentDescription = spoken; liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}

@Composable
private fun FriendMatchControls(s: FriendUi, finished: Boolean, modifier: Modifier = Modifier,
    onResult: () -> Unit, onOffer: () -> Unit, onContinue: () -> Unit, onStop: () -> Unit, actions: FriendActions) {
    // Resume only a match that is actually saved; otherwise return to Host/Join.
    val label = when {
        finished -> R.string.view_result
        s.phase != "session" -> if (s.saved) R.string.friend_continue else R.string.friend_disconnect
        s.offer -> R.string.friend_review_draw
        s.paused -> if (s.ready) R.string.friend_cancel_ready else R.string.resume
        else -> R.string.friend_pause
    }
    OutlinedButton(onClick = {
        when {
            finished -> onResult()
            s.phase != "session" -> if (s.saved) onContinue() else onStop()
            s.offer -> onOffer()
            s.paused -> actions.ready()
            else -> actions.pause()
        }
    }, enabled = finished || !s.busy, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
        .testTag("friend-match-action"), shape = RoundedCornerShape(12.dp)) {
        Text(stringResource(label))
    }
}

@Composable
private fun FriendResult(s: FriendUi, actions: FriendActions, onDismiss: () -> Unit, onContinue: () -> Unit,
    onStop: () -> Unit) {
    val match = s.match ?: return
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)
                .testTag("friend-result-dialog"), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(when (match.state.outcome) {
                    Outcome.TIGERS_WIN -> R.string.win_tigers; Outcome.LAMBS_WIN -> R.string.win_lambs; else -> R.string.draw
                }), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(when (match.state.outcome) {
                    Outcome.TIGERS_WIN -> R.string.reason_tigers; Outcome.LAMBS_WIN -> R.string.reason_lambs
                    Outcome.REPETITION_DRAW -> R.string.reason_repetition; else -> R.string.reason_agreement
                }), textAlign = TextAlign.Center)
                if (s.phase == "session") {
                    Button(onClick = { actions.rematch(false) }, enabled = !s.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.play_again)) }
                    OutlinedButton(onClick = { actions.rematch(true) }, enabled = !s.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.switch_sides)) }
                } else {
                    Text(stringResource(R.string.friend_disconnected))
                    Button(onClick = if (s.saved) onContinue else onStop,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(if (s.saved) R.string.friend_continue else R.string.friend_disconnect))
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.view_board)) }
                    TextButton(onClick = actions.home) { Text(stringResource(R.string.home)) }
                }
            }
        }
    }
}
