package com.maswadkar.lambandtiger

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maswadkar.lambandtiger.game.*

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
internal val light =
    lightColorScheme(
        primary = Color(0xFF246657),
        onPrimary = Color.White,
        secondary = Color(0xFF80531B),
        background = Color(0xFFF7F3EA),
        surface = Color(0xFFF7F3EA),
        surfaceVariant = Color(0xFFEAECE2),
        surfaceContainerHighest = Color(0xFFE6EBDD),
        secondaryContainer = Color(0xFFE6EBDD),
        onSurface = Color(0xFF23332F),
        onBackground = Color(0xFF23332F),
        outline = Color(0xFF246657),
        outlineVariant = Color(0xFFCED4C9),
        onSurfaceVariant = Color(0xFF465249),
    )
internal val dark =
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
        onBackground = Color(0xFFF0F0E6),
        outline = Color(0xFF8CAB9B),
        outlineVariant = Color(0xFF46594D),
        onSurfaceVariant = Color(0xFFCCD6CC),
    )

@Composable
fun LambAndTiger(vm: GameViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val friend: com.maswadkar.lambandtiger.friend.FriendViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val ads = com.maswadkar.lambandtiger.ads.LocalAds.current
    val friendState by friend.ui.collectAsStateWithLifecycle()
    val completedKey = when (s.screen) {
        Screen.GAME -> s.match?.takeIf { it.state.outcome != Outcome.ONGOING }?.let { "solo:${it.id}" }
        Screen.NEARBY -> friendState.match?.takeIf { it.state.outcome != Outcome.ONGOING }?.let { "nearby:${it.id}" }
        else -> null
    }
    SideEffect { ads?.updateLocation(s.screen == Screen.HOME, s.screen == Screen.SETTINGS, completedKey) }
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
                        if (s.screen != Screen.GAME && s.screen != Screen.HOME && s.screen != Screen.NEARBY) Row(
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
                            Screen.HOME -> Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f)) { Home(s, vm) }
                                com.maswadkar.lambandtiger.ads.HomeBanner()
                            }
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF7F3EA),
            shape = RoundedCornerShape(24.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.brand_logo),
                contentDescription = stringResource(R.string.play_title),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(156.dp).padding(16.dp)
                    .testTag("home-brand-logo"),
            )
        }
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

private val difficultyArtwork = listOf(
    R.drawable.challenge_sleepy, R.drawable.challenge_lamb, R.drawable.challenge_paw,
    R.drawable.challenge_schemer, R.drawable.challenge_grandmaster,
)
private val difficultyTitles = listOf(
    R.string.difficulty_title_0, R.string.difficulty_title_1, R.string.difficulty_title_2,
    R.string.difficulty_title_3, R.string.difficulty_title_4,
)
private val difficultyLevels = listOf(
    R.string.difficulty_level_0, R.string.difficulty_level_1, R.string.difficulty_level_2,
    R.string.difficulty_level_3, R.string.difficulty_level_4,
)

@Composable
private fun Setup(s: UiState, vm: GameViewModel) {
    Column(Modifier.fillMaxSize()) {
        // Keep the primary action reachable even when choices need to scroll.
        SetupChoices(s, vm, Modifier.weight(1f))
        Surface(shadowElevation = 3.dp) {
            Button(
                onClick = vm::requestStart, enabled = !s.busy,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .heightIn(min = 52.dp).testTag("setup-start"),
            ) {
                Text(stringResource(R.string.start_game), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SetupChoices(s: UiState, vm: GameViewModel, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Title(R.string.solo_setup)
        Text(stringResource(R.string.choose_side), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Side.entries.forEach { side ->
                val selected = s.setupSide == side
                Surface(
                    modifier = Modifier.weight(1f).selectable(
                        selected = selected, role = Role.RadioButton,
                        onClick = { vm.setup(side = side) },
                    ).testTag("setup-side-${side.name}"),
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        Modifier.heightIn(min = 64.dp).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            painterResource(if (side == Side.LAMB) R.drawable.challenge_lamb
                                else R.drawable.challenge_tiger),
                            contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape),
                        )
                        Text(sideLabel(side), modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Text(stringResource(R.string.lambs_first), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.difficulty), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Difficulty.entries.forEachIndexed { i, d ->
                val selected = s.setupDifficulty == d
                Surface(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = selected, role = Role.RadioButton,
                        onClick = { vm.setup(difficulty = d) },
                    ).testTag("setup-difficulty-${d.name}"),
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        Modifier.heightIn(min = 76.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(painterResource(difficultyArtwork[i]), contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(CircleShape))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(difficultyTitles[i]), modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                if (selected) Surface(shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary) {
                                    Icon(painterResource(R.drawable.ic_check), contentDescription = null,
                                        modifier = Modifier.size(22.dp).padding(4.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Text(stringResource(difficultyLevels[i]),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold)
                            Text(stringResource(difficultyDescriptions[i]),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
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

/** Board interaction is direct; Hint and Undo remain visible below it. */
@Composable
internal fun GameScreen(s: UiState, vm: GameViewModel) {
    val match = s.match ?: return
    val finished = match.state.outcome != Outcome.ONGOING
    var resultDismissed by rememberSaveable(match.id) { mutableStateOf(false) }
    var resultReady by remember(match.id, finished) { mutableStateOf(false) }
    LaunchedEffect(match.id, finished) {
        if (finished) {
            // Let the final confirmed capture/move settle before presenting its result.
            if (!s.preferences.reduced) kotlinx.coroutines.delay(400)
            resultReady = true
        }
    }
    var menu by remember { mutableStateOf(false) }
    var moving by remember(match.id) { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val navigation: @Composable (Boolean) -> Unit = { showTitle ->
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=4.dp), verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=vm::back) {
                Icon(painterResource(R.drawable.ic_back), null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.back))
            }
            if (showTitle) Text(stringResource(R.string.play_title),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            else Spacer(Modifier.weight(1f))
            Box {
                TextButton(onClick={ menu=true },modifier=Modifier.testTag("game-menu")) { Text(stringResource(R.string.game_menu)) }
                DropdownMenu(expanded=menu,onDismissRequest={ menu=false }) {
                    if (finished) DropdownMenuItem(text={ Text(stringResource(R.string.view_result)) },
                        onClick={ menu=false;resultDismissed=false })
                    DropdownMenuItem(text={ Text(stringResource(R.string.rules)) },onClick={menu=false;vm.navigate(Screen.RULES)},enabled=!s.busy)
                    DropdownMenuItem(text={ Text(stringResource(R.string.settings)) },onClick={menu=false;vm.navigate(Screen.SETTINGS)},enabled=!s.busy)
                    DropdownMenuItem(text={ Text(stringResource(R.string.save_home)) },onClick={menu=false;vm.back()},enabled=!s.busy)
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        Column(Modifier.fillMaxSize()) {
            if (!landscape) navigation(true)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val railWidth=if(maxWidth>=840.dp) 300.dp else 244.dp
                val scrolling=maxHeight<680.dp || LocalDensity.current.fontScale>1.3f
                val playable=vm.playable(s) && !moving
                val board: @Composable (Modifier) -> Unit = { modifier ->
                    key(match.id) {
                        BoardView(match.state,s.selected,if(playable) GameEngine.available(match.state) else emptyList(),
                            s.hint,vm::tap,modifier.testTag("play-board"),last=s.lastAction,immersive=true,
                            reducedMotion=s.preferences.reduced,motionEnabled=!s.paused,onMotionRunning={ moving=it },
                            showPointLabels=false)
                    }
                }
                if (landscape) Row(Modifier.fillMaxSize()) {
                    board(Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(railWidth).fillMaxHeight()
                        .verticalScroll(scroll).padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        navigation(false)
                        MatchIdentity(s)
                        TabletopTurn(s,vm)
                        TabletopCounts(match.state)
                        TabletopActions(s,vm,playable,onShowResult={ resultDismissed=false })
                    }
                } else {
                    val boardHeight=(maxWidth-36.dp)*.8f+60.dp
                    Column(Modifier.fillMaxSize().then(if(scrolling) Modifier.verticalScroll(scroll) else Modifier),
                        horizontalAlignment=Alignment.CenterHorizontally) {
                        MatchIdentity(s,Modifier.padding(horizontal=18.dp,vertical=10.dp))
                        TabletopTurn(s,vm,Modifier.padding(horizontal=18.dp,vertical=8.dp))
                        board(if(scrolling) Modifier.fillMaxWidth().height(boardHeight)
                            else Modifier.fillMaxWidth().weight(1f))
                        TabletopCounts(match.state,Modifier.padding(horizontal=18.dp,vertical=14.dp))
                        TabletopActions(s,vm,playable,Modifier.padding(horizontal=18.dp,vertical=8.dp),
                            onShowResult={ resultDismissed=false })
                    }
                }
            }
        }
    }
    if (s.message != null) {
        AlertDialog(
            modifier = Modifier.testTag("game-error-dialog"),
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = vm::dismissMessage,
            text = {
                Text(stringResource(s.message), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                        .semantics { liveRegion = LiveRegionMode.Polite })
            },
            confirmButton = {
                TextButton(onClick = vm::dismissMessage) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    } else if (finished && resultReady && !resultDismissed) {
        GameResultDialog(s,vm,onDismiss={ resultDismissed=true })
    }
}

@Composable
private fun MatchIdentity(s: UiState, modifier: Modifier = Modifier) {
    val match=s.match ?: return
    FlowRow(modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically) {
            AnimalIcon(match.human,Modifier.size(36.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if(match.human==Side.LAMB) R.string.you_lambs_compact else R.string.you_tigers_compact),
                style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
        }
        Row(Modifier.heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically) {
            AnimalIcon(match.human.other(),Modifier.size(36.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(stringResource(difficultyTitles[match.difficulty.ordinal]),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
                Text(stringResource(difficultyLevels[match.difficulty.ordinal]),style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TabletopTurn(s: UiState,vm: GameViewModel,modifier: Modifier=Modifier) {
    val match=s.match ?: return
    val state=match.state
    val title=when {
        state.outcome==Outcome.TIGERS_WIN -> R.string.win_tigers
        state.outcome==Outcome.LAMBS_WIN -> R.string.win_lambs
        state.outcome!=Outcome.ONGOING -> R.string.draw
        s.busy -> R.string.saving
        s.paused -> R.string.paused
        state.sideToMove!=match.human -> R.string.thinking
        else -> R.string.your_turn
    }
    val prompt = when {
        s.hint != null && vm.playable(s) -> stringResource(when (s.hint) {
            is Action.Place -> R.string.hint_place_visual
            is Action.Step -> R.string.hint_step_visual
            is Action.Capture -> R.string.hint_capture_visual
        })
        vm.playable(s) -> stringResource(R.string.turn_prompt, stringResource(title),
            stringResource(if(state.position.phase==Phase.PLACEMENT && match.human==Side.LAMB)
                R.string.place_compact else R.string.move_compact))
        else -> stringResource(title)
    }
    // The visual hint relies on the board ring/arrow; speak the hinted points instead.
    val spoken=listOfNotNull(s.hint?.takeIf { vm.playable(s) }?.let { actionLabel(it) } ?: prompt,
        stringResource(if(state.position.phase==Phase.PLACEMENT) R.string.phase_placement else R.string.phase_movement),
        stringResource(R.string.reserve_count,state.position.reserve),
        stringResource(R.string.captured_count,state.position.captured),s.lastAction?.let { stringResource(R.string.last_move,moveLabel(it)) }).joinToString(". ")
    // Reserve two lines for turns and hints alike; feedback never pushes the board down.
    // Convert each line before multiplying: Android's enlarged fonts scale nonlinearly.
    val bannerHeight = with(LocalDensity.current) { 22.sp.toDp() * 2 } + 12.dp
    Column(modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
        Surface(Modifier.fillMaxWidth().height(bannerHeight).testTag("turn-banner"),
            shape=RoundedCornerShape(12.dp),
            color=MaterialTheme.colorScheme.primary.copy(alpha=.09f),
            contentColor=MaterialTheme.colorScheme.primary) {
            key(prompt) {
                Box(Modifier.padding(horizontal=12.dp,vertical=6.dp).verticalScroll(rememberScrollState()),
                    contentAlignment=Alignment.Center) {
                    Text(prompt,style=MaterialTheme.typography.titleMedium.copy(fontSize=18.sp,lineHeight=22.sp),
                        fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center,
                        modifier=Modifier.semantics {contentDescription=spoken;liveRegion=LiveRegionMode.Polite})
                }
            }
        }
        if(s.paused && state.outcome==Outcome.ONGOING) Button(onClick=vm::resume,enabled=!s.busy) {Text(stringResource(R.string.resume))}
    }
}

@Composable
internal fun TabletopCounts(state: GameState,modifier: Modifier=Modifier) {
    Row(modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        for ((value, label) in listOf(state.position.reserve.toString() to R.string.to_place_compact,
            stringResource(R.string.capture_value,state.position.captured) to R.string.captured_suffix)) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontSize=28.sp,fontWeight=FontWeight.SemiBold)) { append(value) }
                append(" ")
                append(stringResource(label))
            },modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,
                textAlign=TextAlign.Center)
        }
    }
}

@Composable
private fun TabletopActions(s: UiState,vm: GameViewModel,playable: Boolean,modifier: Modifier=Modifier,onShowResult: () -> Unit) {
    val match=s.match ?: return
    val finished=match.state.outcome!=Outcome.ONGOING
    Box(modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
        // Retain the exact control footprint when replacing it with View Result.
        Column(Modifier.fillMaxWidth().then(if(finished) Modifier.alpha(0f).clearAndSetSemantics {} else Modifier),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp),
                maxItemsInEachRow=if(LocalDensity.current.fontScale>1.3f) 1 else 2) {
                OutlinedButton(onClick=vm::hint,enabled=playable,modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(12.dp)) {
                    Icon(painterResource(R.drawable.ic_hint),null,Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.hint))
                }
                OutlinedButton(onClick=vm::undo,enabled=playable && GameEngine.undoPrefix(match.state,match.human)!=null,
                    modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(12.dp)) {
                    Icon(painterResource(R.drawable.ic_undo),null,Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.undo))
                }
            }
        }
        if(finished) OutlinedButton(onClick=onShowResult,modifier=Modifier.testTag("view-result")) {
            Text(stringResource(R.string.view_result))
        }
    }
}

@Composable
private fun GameResultDialog(s: UiState, vm: GameViewModel, onDismiss: () -> Unit) {
    val match = s.match ?: return
    val outcome = match.state.outcome
    val ads = com.maswadkar.lambandtiger.ads.LocalAds.current
    fun rematch(switch: Boolean) {
        val proceed = { vm.openSetup(switch = switch); vm.requestStart() }
        if (ads == null) proceed() else ads.betweenGames("solo:${match.id}", proceed)
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(24.dp).testTag("game-result-dialog"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(when (outcome) {
                        Outcome.TIGERS_WIN -> R.string.win_tigers
                        Outcome.LAMBS_WIN -> R.string.win_lambs
                        else -> R.string.draw
                    }),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(when (outcome) {
                        Outcome.TIGERS_WIN -> R.string.reason_tigers
                        Outcome.LAMBS_WIN -> R.string.reason_lambs
                        Outcome.REPETITION_DRAW -> R.string.reason_repetition
                        else -> R.string.reason_agreement
                    }),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(stringResource(if (match.human == Side.LAMB) R.string.you_are_lambs else R.string.you_are_tigers),
                    style = MaterialTheme.typography.labelLarge)
                Button(onClick = { rematch(false) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.play_again))
                }
                OutlinedButton(onClick = { rematch(true) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.switch_sides))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.view_board)) }
                    TextButton(onClick = vm::home) { Text(stringResource(R.string.home)) }
                }
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
                selected = n == selected,
                // A legal destination is available, not an already selected piece.
                // Keep the visual cue without reporting multiple selections to accessibility.
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (available) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent,
                    labelColor = if (available) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                onClick = { onTap(n) },
                enabled = enabled,
                modifier =
                    Modifier.heightIn(min = 48.dp).widthIn(min = 52.dp).semantics {
                        contentDescription = description
                    },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(n.name + if (capture) " +" else "")
                        if (piece != Occupant.EMPTY) AnimalIcon(
                            if (piece == Occupant.LAMB) Side.LAMB else Side.TIGER, Modifier.size(20.dp))
                    }
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
        com.maswadkar.lambandtiger.ads.PrivacyControls()
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
            reducedMotion = s.preferences.reduced,
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
