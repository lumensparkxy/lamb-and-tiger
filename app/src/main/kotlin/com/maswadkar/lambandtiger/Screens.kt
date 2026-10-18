package com.maswadkar.lambandtiger

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maswadkar.lambandtiger.game.*
import kotlin.math.hypot

private val difficultyNames =
    listOf(
        R.string.difficulty_0,
        R.string.difficulty_1,
        R.string.difficulty_2,
        R.string.difficulty_3,
        R.string.difficulty_4,
    )
private val difficultyDescriptions =
    listOf(
        R.string.difficulty_desc_0,
        R.string.difficulty_desc_1,
        R.string.difficulty_desc_2,
        R.string.difficulty_desc_3,
        R.string.difficulty_desc_4,
    )
private val lessons =
    listOf(
        R.string.lesson_0,
        R.string.lesson_1,
        R.string.lesson_2,
        R.string.lesson_3,
        R.string.lesson_4,
        R.string.lesson_5,
    )
private val instructions =
    listOf(
        R.string.lesson_instruction_0,
        R.string.lesson_instruction_1,
        R.string.lesson_instruction_2,
        R.string.lesson_instruction_3,
        R.string.lesson_instruction_4,
        R.string.lesson_instruction_5,
    )
private val light =
    lightColorScheme(
        primary = Color(0xFF285747),
        onPrimary = Color.White,
        secondary = Color(0xFF80531B),
        background = Color(0xFFF8F6F0),
        surface = Color(0xFFF8F6F0),
        surfaceVariant = Color(0xFFEAECE2),
        surfaceContainerHighest = Color(0xFFE6EBDD),
        secondaryContainer = Color(0xFFE6EBDD),
        onSurface = Color(0xFF202B26),
        onSurfaceVariant = Color(0xFF465249),
    )
private val dark =
    darkColorScheme(
        primary = Color(0xFFA8D4BC),
        onPrimary = Color(0xFF16372B),
        secondary = Color(0xFFE8BD78),
        background = Color(0xFF131E1A),
        surface = Color(0xFF131E1A),
        surfaceVariant = Color(0xFF29382F),
        surfaceContainerHighest = Color(0xFF29382F),
        secondaryContainer = Color(0xFF3B4938),
        onSurface = Color(0xFFF0F0E6),
        onSurfaceVariant = Color(0xFFCCD6CC),
    )

@Composable
fun LambAndTiger(vm: GameViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val friend: com.maswadkar.lambandtiger.friend.FriendViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val goBack = { if(s.screen != Screen.NEARBY || !friend.requestLeave()) vm.back() }
    val view = LocalView.current
    val activity =
        androidx.activity.compose.LocalActivity.current as? androidx.activity.ComponentActivity
    LaunchedEffect(s.preferences.dark) {
        val style =
            if (s.preferences.dark)
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else
                androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                )
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    var lastFeedback by remember { mutableIntStateOf(s.feedbackTick) }
    LaunchedEffect(s.feedbackTick) {
        if (s.feedbackTick > lastFeedback) {
            if (s.preferences.vibration && s.feedbackHuman)
                view.performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.VIRTUAL_KEY
                )
            if (s.preferences.sound) {
                val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }.getOrNull()
                if (tone != null)
                    try {
                        tone.startTone(
                            when {
                                s.match?.state?.outcome != Outcome.ONGOING ->
                                    ToneGenerator.TONE_PROP_PROMPT
                                s.lastAction is Action.Capture -> ToneGenerator.TONE_PROP_BEEP2
                                else -> ToneGenerator.TONE_PROP_ACK
                            },
                            100,
                        )
                        kotlinx.coroutines.delay(180)
                    } finally {
                        tone.release()
                    }
            }
        }
        lastFeedback = s.feedbackTick
    }
    MaterialTheme(colorScheme = if (s.preferences.dark) dark else light) {
        BackHandler(s.screen != Screen.HOME) { goBack() }
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                val tooSmall =
                    if (maxWidth > maxHeight) maxWidth < 480.dp || maxHeight < 320.dp
                    else maxWidth < 320.dp || maxHeight < 480.dp
                LaunchedEffect(tooSmall) { if (tooSmall) { vm.pause(); if(s.screen==Screen.NEARBY) friend.pause() } }
                if (tooSmall)
                    Column(
                        Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(stringResource(R.string.window_small))
                        TextButton(onClick = vm::home) { Text(stringResource(R.string.home)) }
                    }
                else
                    Column(Modifier.fillMaxSize()) {
                        if (s.screen != Screen.GAME) Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (s.screen != Screen.HOME)
                                TextButton(onClick = goBack) {
                                    Text(stringResource(R.string.back))
                                }
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (s.busy)
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                        when (s.screen) {
                            Screen.HOME -> Home(s, vm)
                            Screen.SETUP -> Setup(s, vm)
                            Screen.GAME -> GameScreen(s, vm)
                            Screen.SETTINGS -> Settings(s, vm)
                            Screen.LEARN -> Learning(vm)
                            Screen.RULES -> QuickRules()
                            Screen.TUTORIAL -> Tutorial(s, vm)
                            Screen.NEARBY -> FriendScreen(friend, s.preferences, vm::home, { vm.navigate(Screen.RULES) }, { vm.navigate(Screen.SETTINGS) })
                            Screen.RECOVERY ->
                                Page {
                                    Title(R.string.recovery_title)
                                    Text(stringResource(R.string.recovery_explanation))
                                    Button(onClick = vm::recover, enabled = !s.busy) {
                                        Text(stringResource(R.string.retry_recovery))
                                    }
                                    TextButton(onClick = vm::home) {
                                        Text(stringResource(R.string.home))
                                    }
                                    OutlinedButton(
                                        onClick = { vm.openSetup() },
                                        enabled = !s.busy,
                                    ) {
                                        Text(stringResource(R.string.new_game))
                                    }
                                }
                        }
                    }
            }
        }
        if (s.replaceDialog)
            AlertDialog(
                onDismissRequest = vm::cancelReplace,
                title = { Text(stringResource(R.string.replace_title)) },
                text = { Text(stringResource(R.string.replace_body)) },
                confirmButton = {
                    TextButton(onClick = vm::start) { Text(stringResource(R.string.replace)) }
                },
                dismissButton = {
                    TextButton(onClick = vm::cancelReplace) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        if (s.leaveDialog)
            AlertDialog(
                onDismissRequest = vm::keepPlaying,
                title = { Text(stringResource(R.string.leave_title)) },
                text = { Text(stringResource(R.string.leave_body)) },
                confirmButton = {
                    TextButton(onClick = vm::home, enabled = !s.busy) {
                        Text(stringResource(R.string.save_home))
                    }
                },
                dismissButton = {
                    TextButton(onClick = vm::keepPlaying) {
                        Text(stringResource(R.string.keep_playing))
                    }
                },
            )
    }
}

@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content,
    )
}

@Composable
private fun Title(id: Int) {
    Text(
        stringResource(id),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun Home(s: UiState, vm: GameViewModel) {
    Page {
        Text(
            stringResource(R.string.offline),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.home_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(shape = RoundedCornerShape(24.dp)) {
            BoardView(
                GameEngine.newGame(),
                null,
                emptyList(),
                null,
                {},
                Modifier.fillMaxWidth().height(220.dp),
            )
        }
        if (s.hasSave)
            Button(
                onClick = vm::continueGame,
                modifier = Modifier.fillMaxWidth(),
                enabled = !s.busy,
            ) {
                Text(stringResource(R.string.continue_game))
            }
        ChoiceCard(R.string.play_ai, R.string.play_ai_subtitle, !s.busy) { vm.openSetup() }
        ChoiceCard(R.string.play_friend, R.string.play_friend_subtitle, !s.busy) {
            vm.navigate(Screen.NEARBY)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.navigate(Screen.LEARN) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.how_to_play))
            }
            OutlinedButton(
                onClick = { vm.navigate(Screen.SETTINGS) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.settings))
            }
        }
    }
}

@Composable
private fun ChoiceCard(title: Int, subtitle: Int, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Setup(s: UiState, vm: GameViewModel) {
    Page {
        Title(R.string.solo_setup)
        Text(stringResource(R.string.choose_side), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Side.entries.forEach { side ->
                FilterChip(
                    selected = s.setupSide == side,
                    onClick = { vm.setup(side = side) },
                    label = { Text(sideLabel(side)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(stringResource(R.string.lambs_first))
        Text(stringResource(R.string.difficulty), style = MaterialTheme.typography.titleMedium)
        Difficulty.entries.forEachIndexed { i, d ->
            OutlinedCard(
                onClick = { vm.setup(difficulty = d) },
                modifier = Modifier.fillMaxWidth(),
                border =
                    BorderStroke(
                        if (s.setupDifficulty == d) 2.dp else 1.dp,
                        if (s.setupDifficulty == d) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = s.setupDifficulty == d,
                        onClick = { vm.setup(difficulty = d) },
                    )
                    Column {
                        Text(stringResource(difficultyNames[i]), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(difficultyDescriptions[i]),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        Button(
            onClick = vm::requestStart,
            enabled = !s.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.start_game))
        }
    }
}

@Composable
private fun sideLabel(side: Side) =
    stringResource(if (side == Side.LAMB) R.string.lamb else R.string.tiger)

@Composable
private fun actionLabel(action: Action): String =
    when (action) {
        is Action.Place -> stringResource(R.string.hint_place, action.to.name)
        is Action.Step -> stringResource(R.string.hint_step, action.from.name, action.to.name)
        is Action.Capture ->
            stringResource(
                R.string.hint_capture,
                action.from.name,
                action.to.name,
                action.over.name,
            )
    }

/** The controls are disclosure UI, not a pause: accessible point play stays available
 * while the panel is open. Rules/Settings retain the coordinator's explicit pause rules. */
@Composable
private fun GameScreen(s: UiState, vm: GameViewModel) {
    val match = s.match ?: return
    val state = match.state
    var controls by rememberSaveable(match.id) { mutableStateOf(false) }
    LaunchedEffect(state.outcome) {
        if (state.outcome != Outcome.ONGOING) controls = true
    }
    BackHandler(controls) { controls = false }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val statusHeight = maxHeight * .26f
        val reveal = { controls = true }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                PlayBoard(s, vm, Modifier.weight(1f).fillMaxHeight(), reveal)
                if (controls) PlayControls(s, vm, Modifier.width(300.dp).fillMaxHeight()) { controls = false }
                else Column(
                    Modifier.width(160.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.play_title), fontWeight = FontWeight.Bold)
                    TextButton(onClick = vm::back) { Text(stringResource(R.string.back)) }
                    CompactPlayStatus(s, vm)
                    MatchIdentity(s)
                    TextButton(onClick = reveal, modifier = Modifier.testTag("show-controls").heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.show_controls))
                    }
                }
            }
        } else Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::back) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.play_title), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                if (!controls) TextButton(onClick = reveal,
                    modifier = Modifier.testTag("show-controls").heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.controls))
                }
            }
            Column(Modifier.heightIn(max = statusHeight).verticalScroll(rememberScrollState())) {
                CompactPlayStatus(s, vm, Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                if (!controls) MatchIdentity(s, Modifier.padding(horizontal = 12.dp))
            }
            // Width is the portrait limit. Reserve exactly the natural board height
            // when controls open; on short/large-text windows share the remaining space.
            if (controls) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val boardHeight = minOf((maxWidth - 36.dp) * .8f + 60.dp, maxHeight * .58f)
                    Column(Modifier.fillMaxSize()) {
                        PlayBoard(s, vm, Modifier.fillMaxWidth().height(boardHeight), reveal)
                        PlayControls(s, vm, Modifier.weight(1f).fillMaxWidth()) { controls = false }
                    }
                }
            } else PlayBoard(s, vm, Modifier.weight(1f).fillMaxWidth(), reveal)
        }
    }
}

@Composable
private fun PlayControls(s: UiState, vm: GameViewModel, modifier: Modifier, hide: () -> Unit) {
    val match = s.match ?: return
    val state = match.state
    Surface(modifier.testTag("controls-panel"),
        color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        var distance = 0f
                        detectVerticalDragGestures(
                            onDragStart = { distance = 0f },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                distance += amount
                                if (distance > 48.dp.toPx()) hide()
                            },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { hide() }, modifier = Modifier.testTag("hide-controls")) {
                    Text(stringResource(R.string.hide_controls))
                }
            }
            HorizontalDivider()
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.outcome != Outcome.ONGOING) GameControls(s, vm, showStatus = false)
                else {
                    if (s.paused) Button(onClick = vm::resume, enabled = !s.busy) {
                        Text(stringResource(R.string.resume))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = vm::hint, enabled = vm.playable()) {
                            Text(stringResource(R.string.hint))
                        }
                        OutlinedButton(onClick = vm::undo,
                            enabled = vm.playable() && GameEngine.undoPrefix(state, match.human) != null) {
                            Text(stringResource(R.string.undo))
                        }
                        TextButton(onClick = { vm.navigate(Screen.RULES) }, enabled = !s.busy) {
                            Text(stringResource(R.string.rules))
                        }
                        TextButton(onClick = { vm.navigate(Screen.SETTINGS) }, enabled = !s.busy) {
                            Text(stringResource(R.string.settings))
                        }
                    }
                    TextButton(onClick = vm::back, enabled = !s.busy) {
                        Text(stringResource(R.string.save_home))
                    }
                    Text(stringResource(if (state.position.phase == Phase.PLACEMENT && match.human == Side.LAMB)
                        R.string.place_instruction else R.string.move_instruction))
                }
                PointPicker(state, s.selected,
                    if (vm.playable()) GameEngine.available(state) else emptyList(),
                    vm.playable(), vm::tap)
                GameStatus(s)
                s.lastAction?.let {
                    Text(stringResource(R.string.last_move, moveLabel(it)))
                }
            }
        }
    }
}

@Composable
private fun MatchIdentity(s: UiState, modifier: Modifier = Modifier) {
    val match = s.match ?: return
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
            Image(painterResource(if (match.human == Side.LAMB) R.drawable.lamb_portrait else R.drawable.tiger_portrait),
                contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (match.human == Side.LAMB) R.string.you_are_lambs else R.string.you_are_tigers),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
            Image(painterResource(if (match.human == Side.LAMB) R.drawable.tiger_portrait else R.drawable.lamb_portrait),
                contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(difficultyNames[match.difficulty.ordinal]),
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CompactPlayStatus(s: UiState, vm: GameViewModel, modifier: Modifier = Modifier) {
    val match = s.match ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        val title = when {
            match.state.outcome == Outcome.TIGERS_WIN -> R.string.win_tigers
            match.state.outcome == Outcome.LAMBS_WIN -> R.string.win_lambs
            match.state.outcome != Outcome.ONGOING -> R.string.draw
            s.busy -> R.string.saving
            s.paused -> R.string.paused
            match.state.position.turn != match.human -> R.string.thinking
            else -> R.string.your_turn
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (vm.playable()) Text(
                stringResource(if (match.state.position.phase == Phase.PLACEMENT && match.human == Side.LAMB)
                    R.string.place_compact else R.string.move_compact),
                style = MaterialTheme.typography.bodyMedium)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.reserve_compact, match.state.position.reserve),
                style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.captured_compact, match.state.position.captured),
                style = MaterialTheme.typography.bodySmall)
        }
        if (s.paused && match.state.outcome == Outcome.ONGOING)
            Button(onClick = vm::resume, enabled = !s.busy) { Text(stringResource(R.string.resume)) }
        s.hint?.let { Text(actionLabel(it), style = MaterialTheme.typography.bodyMedium) }
        s.message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun PlayBoard(s: UiState, vm: GameViewModel, modifier: Modifier, reveal: () -> Unit) {
    val state = s.match?.state ?: return
    BoxWithConstraints(modifier.pointerInput(Unit) {
        var distance = 0f
        detectVerticalDragGestures(
            onDragStart = { distance = 0f },
            onVerticalDrag = { change, amount ->
                distance += amount
                if (distance < -48.dp.toPx()) { change.consume(); reveal() }
            },
        )
    }, contentAlignment = Alignment.Center) {
        Image(painterResource(if (maxHeight > maxWidth) R.drawable.woodland_board_portrait else R.drawable.woodland_board), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            alpha = if (s.preferences.dark) .13f else .8f)
        BoardView(state, s.selected,
            if (vm.playable()) GameEngine.available(state) else emptyList(),
            s.hint, vm::tap, Modifier.fillMaxSize().testTag("play-board"),
            last = s.lastAction, immersive = true)
    }
}

@Composable
private fun GameStatus(s: UiState) {
    val match = s.match ?: return
    val p = match.state.position
    Text(stringResource(R.string.play_ai), style = MaterialTheme.typography.labelLarge)
    Text(
        stringResource(
            if (match.human == Side.LAMB) R.string.you_are_lambs else R.string.you_are_tigers
        ),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(difficultyNames[match.difficulty.ordinal]),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(
                if (p.phase == Phase.PLACEMENT) R.string.phase_placement
                else R.string.phase_movement
            ),
            fontWeight = FontWeight.SemiBold,
        )
        Text(stringResource(R.string.event_count, match.state.eventCount))
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.reserve_count, p.reserve))
        Text(stringResource(R.string.captured_count, p.captured))
    }
}

@Composable
private fun GameControls(s: UiState, vm: GameViewModel, showStatus: Boolean = true) {
    val match = s.match ?: return
    val outcome = match.state.outcome
    if (showStatus) GameStatus(s)
    if (outcome != Outcome.ONGOING) {
        Text(
            stringResource(
                when (outcome) {
                    Outcome.TIGERS_WIN -> R.string.win_tigers
                    Outcome.LAMBS_WIN -> R.string.win_lambs
                    else -> R.string.draw
                }
            ),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            stringResource(
                when (outcome) {
                    Outcome.TIGERS_WIN -> R.string.reason_tigers
                    Outcome.LAMBS_WIN -> R.string.reason_lambs
                    Outcome.REPETITION_DRAW -> R.string.reason_repetition
                    else -> R.string.reason_agreement
                }
            )
        )
        Button(
            onClick = {
                vm.openSetup()
                vm.requestStart()
            }
        ) {
            Text(stringResource(R.string.play_again))
        }
        OutlinedButton(
            onClick = {
                vm.openSetup(switch = true)
                vm.requestStart()
            }
        ) {
            Text(stringResource(R.string.switch_sides))
        }
        TextButton(onClick = vm::home) { Text(stringResource(R.string.home)) }
    } else {
        Text(
            stringResource(
                when {
                    s.busy -> R.string.saving
                    s.paused -> R.string.paused
                    match.state.position.turn != match.human -> R.string.thinking
                    else -> R.string.your_turn
                }
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (s.paused)
            Button(onClick = vm::resume, enabled = !s.busy) {
                Text(stringResource(R.string.resume))
            }
        else
            Text(
                stringResource(
                    if (match.state.position.turn == Side.LAMB && match.state.position.reserve > 0)
                        R.string.place_instruction
                    else R.string.move_instruction
                )
            )
        if (s.hint != null)
            Text(
                actionLabel(s.hint),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        if (s.lastAction != null)
            Text(
                stringResource(R.string.last_move, moveLabel(s.lastAction)),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        if (s.message != null)
            Text(
                stringResource(s.message),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = vm::hint, enabled = vm.playable()) {
                Text(stringResource(R.string.hint))
            }
            OutlinedButton(
                onClick = vm::undo,
                enabled = vm.playable() && GameEngine.undoPrefix(match.state, match.human) != null,
            ) {
                Text(stringResource(R.string.undo))
            }
            TextButton(onClick = { vm.navigate(Screen.RULES) }, enabled = !s.busy) {
                Text(stringResource(R.string.rules))
            }
            TextButton(onClick = { vm.navigate(Screen.SETTINGS) }, enabled = !s.busy) {
                Text(stringResource(R.string.settings))
            }
        }
        OutlinedButton(onClick = vm::back, enabled = !s.busy) {
            Text(stringResource(R.string.save_home))
        }
    }
}

@Composable
internal fun BoardView(
    state: GameState,
    selected: Node?,
    actions: List<Action>,
    hint: Action?,
    onTap: (Node) -> Unit,
    modifier: Modifier,
    last: Action? = null,
    immersive: Boolean = false,
) {
    val tigerImage = ImageBitmap.imageResource(R.drawable.tiger_portrait)
    val lambImage = ImageBitmap.imageResource(R.drawable.lamb_portrait)
    val lines = if (immersive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = MaterialTheme.colorScheme.primary
    val canvasLabel = stringResource(if (immersive) R.string.play_board_label else R.string.board_label)
    val destinations =
        actions
            .filter { it is Action.Place || it.origin == selected }
            .map { it.destination }
            .toSet()
    Canvas(
        modifier
            .padding(if (immersive) 0.dp else 14.dp)
            .semantics { contentDescription = canvasLabel }
            .pointerInput(state, selected, actions) {
                detectTapGestures { tap ->
                    if (actions.isNotEmpty()) {
                        val margin = (if (immersive) 18.dp else 25.dp).toPx()
                        val n =
                            Node.entries.minBy {
                                val p =
                                    boardPoint(
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                        margin,
                                        it,
                                        margin + if (immersive) 12.dp.toPx() else 0f,
                                    )
                                hypot(tap.x - p.x, tap.y - p.y)
                            }
                        val p = boardPoint(size.width.toFloat(), size.height.toFloat(), margin, n, margin + if (immersive) 12.dp.toPx() else 0f)
                        if (hypot(tap.x - p.x, tap.y - p.y) <= 24.dp.toPx()) onTap(n)
                    }
                }
            }
    ) {
        val margin = (if (immersive) 18.dp else 25.dp).toPx()
        val verticalMargin = margin + if (immersive) 12.dp.toPx() else 0f
        val scale = minOf((size.width - 2 * margin) / 10, (size.height - 2 * verticalMargin) / 8)
        val pieceRadius = minOf(if (immersive) 16.dp.toPx() else 10.dp.toPx(), scale * .29f)
        fun point(n: Node) = boardPoint(size.width, size.height, margin, n, verticalMargin)
        Board.edges.forEach {
            drawLine(lines.copy(alpha = if (immersive) .95f else .65f), point(it.from), point(it.to), (if (immersive) 2.2.dp else 1.8.dp).toPx())
        }
        fun portrait(bitmap: ImageBitmap, c: Offset, radius: Float) {
            val diameter = (radius * 2).toInt().coerceAtLeast(1)
            drawImage(bitmap, dstOffset = IntOffset((c.x - radius).toInt(), (c.y - radius).toInt()),
                dstSize = IntSize(diameter, diameter))
        }
        Node.entries.forEach { n ->
            val c = point(n)
            val piece = state.position.at(n)
            if (n == last?.origin || n == last?.destination)
                drawRect(
                    selectedColor.copy(alpha = .6f),
                    c - Offset(pieceRadius + 3.dp.toPx(), pieceRadius + 3.dp.toPx()),
                    androidx.compose.ui.geometry.Size(
                        (pieceRadius + 3.dp.toPx()) * 2,
                        (pieceRadius + 3.dp.toPx()) * 2,
                    ),
                    style = Stroke(1.dp.toPx()),
                )
            if (last is Action.Capture && n == last.over) {
                val r = 6.dp.toPx()
                drawLine(lines, c - Offset(r, r), c + Offset(r, r), 2.dp.toPx())
                drawLine(lines, c + Offset(-r, r), c + Offset(r, -r), 2.dp.toPx())
            }
            if (n in destinations) {
                drawCircle(selectedColor.copy(alpha = .14f), (if (immersive) 8.dp else 14.dp).toPx(), c)
                drawCircle(selectedColor, (if (immersive) 8.dp else 13.dp).toPx(), c, style = Stroke(1.5.dp.toPx()))
            }
            when (piece) {
                Occupant.EMPTY -> drawCircle(lines, (if (immersive) 5.dp else 3.dp).toPx(), c)
                Occupant.LAMB -> {
                    drawCircle(Color(0xFFFFFDF3), pieceRadius, c)
                    drawCircle(Color(0xFF354D3D), pieceRadius, c, style = Stroke(2.dp.toPx()))
                    portrait(lambImage, c, pieceRadius * .88f)
                }
                Occupant.TIGER -> {
                    val r = pieceRadius
                    val path =
                        Path().apply {
                            moveTo(c.x, c.y - r)
                            lineTo(c.x + r, c.y + r * .75f)
                            lineTo(c.x - r, c.y + r * .75f)
                            close()
                        }
                    drawPath(path, Color(0xFFE3A344))
                    drawPath(path, Color(0xFF503512), style = Stroke(2.dp.toPx()))
                    portrait(tigerImage, c + Offset(0f, r * .18f), r * .57f)
                }
            }
            if (n == selected || n == hint?.destination)
                drawCircle(selectedColor, 16.dp.toPx(), c, style = Stroke(3.dp.toPx()))
            val paint =
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color =
                        if (lines == Color.Unspecified) android.graphics.Color.BLACK
                        else
                            android.graphics.Color.argb(
                                255,
                                (lines.red * 255).toInt(),
                                (lines.green * 255).toInt(),
                                (lines.blue * 255).toInt(),
                            )
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
            if (fontScale <= 1.3f)
                drawContext.canvas.nativeCanvas.drawText(
                    n.name,
                    c.x,
                    c.y + pieceRadius + 12.dp.toPx(),
                    paint,
                )
            if (actions.any { it is Action.Capture && it.from == selected && it.to == n }) {
                val r = 6.dp.toPx()
                drawLine(selectedColor, c - Offset(r, 0f), c + Offset(r, 0f), 2.dp.toPx())
                drawLine(selectedColor, c - Offset(0f, r), c + Offset(0f, r), 2.dp.toPx())
            }
        }
    }
}

@Composable
internal fun PointPicker(
    state: GameState,
    selected: Node?,
    actions: List<Action>,
    enabled: Boolean,
    onTap: (Node) -> Unit,
) {
    Text(stringResource(R.string.point_picker), style = MaterialTheme.typography.titleMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Node.entries.forEach { n ->
            val piece = state.position.at(n)
            val available = actions.any {
                it.destination == n && (it is Action.Place || it.origin == selected)
            }
            val capture = actions.any { it is Action.Capture && it.from == selected && it.to == n }
            val description =
                stringResource(
                    R.string.point_description,
                    n.name,
                    stringResource(
                        when (piece) {
                            Occupant.EMPTY -> R.string.empty
                            Occupant.LAMB -> R.string.lamb
                            Occupant.TIGER -> R.string.tiger
                        }
                    ),
                    stringResource(
                        if (n == selected) R.string.game_selected
                        else if (capture) R.string.capture_destination
                        else if (available) R.string.available else R.string.game_not_selected
                    ),
                )
            FilterChip(
                selected = n == selected || available,
                onClick = { onTap(n) },
                enabled = enabled,
                modifier =
                    Modifier.heightIn(min = 48.dp).widthIn(min = 52.dp).semantics {
                        contentDescription = description
                    },
                label = {
                    Text(
                        n.name +
                            (if (capture) " +"
                            else if (piece == Occupant.TIGER) " ▲"
                            else if (piece == Occupant.LAMB) " ●" else "")
                    )
                },
            )
        }
    }
}

@Composable
private fun Settings(s: UiState, vm: GameViewModel) {
    Page {
        Title(R.string.settings)
        Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                !s.preferences.dark,
                { vm.preference("dark", false) },
                label = { Text(stringResource(R.string.modern)) },
            )
            FilterChip(
                s.preferences.dark,
                { vm.preference("dark", true) },
                label = { Text(stringResource(R.string.midnight)) },
            )
        }
        Card {
            BoardView(
                GameEngine.newGame(),
                null,
                emptyList(),
                null,
                {},
                Modifier.fillMaxWidth().height(220.dp),
            )
        }
        val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)
        SettingSwitch(R.string.sound, s.preferences.sound) { vm.preference("sound", it) }
        SettingSwitch(
            R.string.vibration,
            s.preferences.vibration,
            vibrator?.hasVibrator() == true,
        ) {
            vm.preference("vibration", it)
        }
        if (vibrator?.hasVibrator() != true) Text(stringResource(R.string.vibration_unavailable))
        SettingSwitch(R.string.reduced_motion, s.preferences.reduced) {
            vm.preference("reduced", it)
        }
        Text(stringResource(R.string.feedback_description))
        if (s.message != null)
            Text(stringResource(s.message), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SettingSwitch(
    label: Int,
    value: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val accessibilityLabel = stringResource(label)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = accessibilityLabel },
        )
    }
}

@Composable
private fun Learning(vm: GameViewModel) {
    Page {
        Title(R.string.how_to_play)
        Text(stringResource(R.string.learning_description))
        Button(onClick = { vm.navigate(Screen.RULES) }) {
            Text(stringResource(R.string.quick_rules))
        }
        Text(stringResource(R.string.guided_tutorial), style = MaterialTheme.typography.titleLarge)
        lessons.forEachIndexed { i, id ->
            OutlinedButton(onClick = { vm.lesson(i) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id))
            }
        }
    }
}

@Composable
private fun Tutorial(s: UiState, vm: GameViewModel) {
    val state = s.lessonState ?: return
    Page {
        Title(lessons[s.lesson])
        Text(stringResource(instructions[s.lesson]))
        BoardView(
            state,
            s.selected,
            if (!s.lessonSuccess && s.lesson != 5) GameEngine.available(state) else emptyList(),
            null,
            vm::tap,
            Modifier.fillMaxWidth().aspectRatio(1.15f),
        )
        if (s.message != null)
            Text(
                stringResource(s.message),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        if (s.lesson == 5) {
            Text(stringResource(R.string.event_count, state.eventCount))
            Button(onClick = vm::repetitionLesson, enabled = !s.lessonSuccess) {
                Text(stringResource(R.string.next_move))
            }
        } else
            PointPicker(state, s.selected, GameEngine.available(state), !s.lessonSuccess, vm::tap)
        OutlinedButton(onClick = { vm.lesson(s.lesson) }) {
            Text(stringResource(R.string.repeat_lesson))
        }
        TextButton(onClick = { vm.navigate(Screen.LEARN) }) {
            Text(stringResource(R.string.lesson_list))
        }
    }
}

private fun boardPoint(width: Float, height: Float, margin: Float, node: Node, verticalMargin: Float = margin): Offset {
    val scale = minOf((width - 2 * margin) / 10, (height - 2 * verticalMargin) / 8)
    val p = Board.points.getValue(node)
    return Offset(width / 2 + p.x * scale, height / 2 + (p.y - 4) * scale)
}

@Composable
private fun moveLabel(action: Action): String =
    when (action) {
        is Action.Place -> stringResource(R.string.move_place, action.to.name)
        is Action.Step -> stringResource(R.string.move_step, action.from.name, action.to.name)
        is Action.Capture ->
            stringResource(
                R.string.move_capture,
                action.from.name,
                action.to.name,
                action.over.name,
            )
    }

@Composable
private fun QuickRules() {
    val capture = remember {
        (GameEngine.fixture(
                Position(
                    Node.A.bit or Node.E.bit or Node.W.bit,
                    Node.C.bit or Node.O.bit,
                    Side.TIGER,
                    13,
                    0,
                )
            ) as Transition.Applied)
            .state
    }
    val trapped = remember {
        (GameEngine.fixture(
                Position(
                    GameEngine.opening.tigers,
                    "BCFGHIJKLMNOPQR".fold(0) { m, c -> m or Node.valueOf(c.toString()).bit },
                    Side.TIGER,
                    0,
                    0,
                )
            ) as Transition.Applied)
            .state
    }
    Page {
        Title(R.string.quick_rules)
        BoardView(
            GameEngine.newGame(),
            null,
            emptyList(),
            null,
            {},
            Modifier.fillMaxWidth().height(250.dp),
        )
        Text(stringResource(R.string.rules_text), lineHeight = 26.sp)
        Text(stringResource(R.string.lesson_3), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.lesson_instruction_3))
        BoardView(
            capture,
            Node.A,
            GameEngine.available(capture),
            Action.Capture(Node.A, Node.C, Node.I),
            {},
            Modifier.fillMaxWidth().height(250.dp),
        )
        Text(stringResource(R.string.reason_lambs), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.trap_explanation))
        BoardView(trapped, null, emptyList(), null, {}, Modifier.fillMaxWidth().height(250.dp))
    }
}
