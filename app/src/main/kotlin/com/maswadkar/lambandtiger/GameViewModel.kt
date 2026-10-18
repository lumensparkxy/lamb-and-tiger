package com.maswadkar.lambandtiger

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maswadkar.lambandtiger.data.*
import com.maswadkar.lambandtiger.game.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class Screen {
    HOME,
    SETUP,
    GAME,
    SETTINGS,
    LEARN,
    RULES,
    TUTORIAL,
    NEARBY,
    RECOVERY,
}

data class Preferences(
    val dark: Boolean = false,
    val sound: Boolean = false,
    val vibration: Boolean = false,
    val reduced: Boolean = false,
)

data class UiState(
    val screen: Screen = Screen.HOME,
    val returnTo: Screen = Screen.HOME,
    val match: SoloMatch? = null,
    val busy: Boolean = true,
    val paused: Boolean = true,
    val error: Boolean = false,
    val hasSave: Boolean = false,
    val selected: Node? = null,
    val hint: Action? = null,
    val lastAction: Action? = null,
    val message: Int? = null,
    val setupSide: Side = Side.LAMB,
    val setupDifficulty: Difficulty = Difficulty.EASY,
    val replaceDialog: Boolean = false,
    val leaveDialog: Boolean = false,
    val preferences: Preferences = Preferences(),
    val lesson: Int = 0,
    val lessonState: GameState? = null,
    val lessonSuccess: Boolean = false,
    val feedbackTick: Int = 0,
    val feedbackHuman: Boolean = false,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = MatchStore(application)
    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()
    private var generation = 0L
    private var navigationGeneration = 0L
    private var foreground = true

    fun foreground(value: Boolean) {
        foreground = value
        if (!value) pause()
    }

    private var computerJob: Job? = null
    private val preferenceMutex = Mutex()
    private var restoreExistingScreen = false

    init {
        viewModelScope.launch {
            try {
                val values =
                    withContext(Dispatchers.IO) {
                        val prefs =
                            Preferences(
                                store.preference("dark", "false").toBoolean(),
                                store.preference("sound", "false").toBoolean(),
                                store.preference("vibration", "false").toBoolean(),
                                store.preference("reduced", systemReduced().toString()).toBoolean(),
                            )
                        Pair(
                            prefs,
                            runCatching {
                                    Difficulty.valueOf(
                                        store.preference("difficulty", Difficulty.EASY.name)
                                    )
                                }
                                .getOrDefault(Difficulty.EASY),
                        )
                    }
                _ui.value =
                    _ui.value.copy(preferences = values.first, setupDifficulty = values.second)
                val match = withContext(Dispatchers.IO) { store.load() }
                _ui.value =
                    _ui.value.copy(
                        match = match,
                        lastAction = (match?.state?.events?.lastOrNull() as? Event.Played)?.action,
                        busy = false,
                        hasSave = match?.state?.outcome == Outcome.ONGOING,
                        screen =
                            if (restoreExistingScreen && match != null) Screen.GAME
                            else Screen.HOME,
                    )
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private fun systemReduced() =
        android.provider.Settings.Global.getFloat(
            getApplication<Application>().contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f

    fun restoreScreen() {
        restoreExistingScreen = true
        if (!_ui.value.busy && _ui.value.match != null)
            _ui.value = _ui.value.copy(screen = Screen.GAME)
    }

    private fun fail(e: Exception) {
        if (e is CancellationException) throw e
        Log.e("LambAndTiger", "Match operation failed: ${e.javaClass.simpleName}")
        cancelComputer()
        _ui.value =
            _ui.value.copy(
                busy = false,
                error = true,
                paused = true,
                hasSave = true,
                screen = Screen.RECOVERY,
                message = R.string.recovery_explanation,
            )
    }

    private fun cancelComputer() {
        generation++
        computerJob?.cancel()
        computerJob = null
    }

    fun pause() {
        cancelComputer()
        _ui.value = _ui.value.copy(paused = true, hint = null)
    }

    fun navigate(screen: Screen) {
        val current = _ui.value
        if (current.busy) return
        navigationGeneration++
        if (current.screen == Screen.GAME && screen in listOf(Screen.RULES, Screen.SETTINGS))
            pause()
        _ui.value =
            _ui.value.copy(
                screen = screen,
                returnTo = current.screen,
                message = null,
                selected = if (screen == Screen.GAME) current.selected else null,
            )
    }

    fun home() {
        navigationGeneration++
        pause()
        _ui.value =
            _ui.value.copy(
                screen = Screen.HOME,
                leaveDialog = false,
                message = null,
                selected = null,
            )
    }

    fun back() {
        val s = _ui.value
        when (s.screen) {
            Screen.GAME ->
                if (s.match?.state?.outcome == Outcome.ONGOING) {
                    pause()
                    _ui.value = _ui.value.copy(leaveDialog = true)
                } else home()
            Screen.SETTINGS,
            Screen.RULES -> _ui.value = s.copy(screen = s.returnTo, message = null)
            Screen.TUTORIAL -> navigate(Screen.LEARN)
            else -> home()
        }
    }

    fun keepPlaying() {
        _ui.value = _ui.value.copy(leaveDialog = false)
    } // explicit Resume remains required

    fun setup(side: Side? = null, difficulty: Difficulty? = null) {
        if (_ui.value.busy) return
        _ui.value =
            _ui.value.copy(
                setupSide = side ?: _ui.value.setupSide,
                setupDifficulty = difficulty ?: _ui.value.setupDifficulty,
            )
    }

    fun openSetup(switch: Boolean = false) {
        navigationGeneration++
        pause()
        val s = _ui.value
        _ui.value =
            s.copy(
                screen = Screen.SETUP,
                setupSide = s.match?.human?.let { if (switch) it.other() else it } ?: Side.LAMB,
                setupDifficulty = s.match?.difficulty ?: s.setupDifficulty,
                message = null,
            )
    }

    fun requestStart() {
        if (_ui.value.hasSave || _ui.value.error) _ui.value = _ui.value.copy(replaceDialog = true)
        else start()
    }

    fun cancelReplace() {
        _ui.value = _ui.value.copy(replaceDialog = false)
    }

    fun start() {
        if (_ui.value.busy) return
        cancelComputer()
        val before = _ui.value
        val navigationTicket = navigationGeneration
        _ui.value = before.copy(busy = true, replaceDialog = false, message = null)
        viewModelScope.launch {
            try {
                val match =
                    withContext(Dispatchers.IO) {
                        store.start(
                            before.setupSide,
                            before.setupDifficulty,
                            before.match,
                            replaceInvalid = before.error,
                        )
                    }
                // Back can leave setup while its durable initial save is completing.
                // Keep that saved match paused without reopening a screen the player left.
                val stayedInSetup =
                    _ui.value.screen == before.screen && navigationTicket == navigationGeneration
                _ui.value =
                    _ui.value.copy(
                        match = match,
                        busy = false,
                        paused = !foreground || !stayedInSetup,
                        error = false,
                        hasSave = true,
                        screen = if (stayedInSetup) Screen.GAME else _ui.value.screen,
                        selected = null,
                        hint = null,
                        lastAction = null,
                    )
                scheduleComputer()
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun continueGame() {
        if (_ui.value.error) navigate(Screen.RECOVERY)
        else {
            _ui.value =
                _ui.value.copy(screen = Screen.GAME, paused = true, selected = null, hint = null)
        }
    }

    fun resume() {
        if (!_ui.value.busy && !_ui.value.error) {
            _ui.value = _ui.value.copy(paused = false, message = null)
            scheduleComputer()
        }
    }

    fun recover() {
        if (_ui.value.busy) return
        _ui.value = _ui.value.copy(busy = true)
        viewModelScope.launch {
            try {
                val match = withContext(Dispatchers.IO) { store.recover() }
                _ui.value =
                    _ui.value.copy(
                        match = match,
                        lastAction = (match?.state?.events?.lastOrNull() as? Event.Played)?.action,
                        busy = false,
                        error = false,
                        paused = true,
                        hasSave = match?.state?.outcome == Outcome.ONGOING,
                        screen = if (match == null) Screen.HOME else Screen.GAME,
                        message = null,
                        selected = null,
                        hint = null,
                    )
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun playable(s: UiState = ui.value) =
        s.screen == Screen.GAME &&
            !s.busy &&
            !s.paused &&
            !s.error &&
            s.match?.state?.sideToMove == s.match?.human &&
            s.match != null

    fun dismissMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    fun tap(node: Node) {
        val s = _ui.value
        if (s.screen == Screen.TUTORIAL) {
            tutorialTap(node)
            return
        }
        if (!playable(s)) return
        val m = s.match ?: return
        val actions = GameEngine.available(m.state)
        if (m.state.position.turn == Side.LAMB && m.state.position.reserve > 0) {
            val a = Action.Place(node)
            if (a in actions) submit(a, Actor.HUMAN)
            else _ui.value = s.copy(message = R.string.illegal_action)
            return
        }
        val selected = s.selected
        if (selected == node) {
            _ui.value = s.copy(selected = null, hint = null, message = null)
            return
        }
        val candidate = actions.firstOrNull { it.origin == selected && it.destination == node }
        if (selected != null && candidate != null) {
            submit(candidate, Actor.HUMAN)
            return
        }
        val species = if (m.human == Side.LAMB) Occupant.LAMB else Occupant.TIGER
        _ui.value =
            if (m.state.position.at(node) == species)
                s.copy(selected = node, hint = null, message = null)
            else s.copy(message = R.string.illegal_action)
    }

    private fun submit(action: Action, actor: Actor) {
        val s = _ui.value
        val m = s.match ?: return
        if (s.busy || s.paused || s.error) return
        _ui.value = s.copy(busy = true, selected = null, hint = null, message = null)
        viewModelScope.launch {
            try {
                val next = withContext(Dispatchers.IO) { store.apply(m, action, actor) }
                _ui.value =
                    _ui.value.copy(
                        match = next,
                        busy = false,
                        lastAction = action,
                        hasSave = next.state.outcome == Outcome.ONGOING,
                        feedbackTick = _ui.value.feedbackTick + 1,
                        feedbackHuman = actor == Actor.HUMAN,
                    )
                scheduleComputer()
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private fun scheduleComputer() {
        val s = _ui.value
        val m = s.match ?: return
        if (
            !foreground ||
                s.screen != Screen.GAME ||
                s.paused ||
                s.busy ||
                s.error ||
                m.state.sideToMove != m.human.other()
        )
            return
        cancelComputer()
        val ticket = generation
        computerJob = viewModelScope.launch {
            // Let the longest board motion settle before the computer reply.
            delay(420)
            val chosen =
                withContext(Dispatchers.Default) {
                    val job = currentCoroutineContext().job
                    val deadline = System.nanoTime() + 1_000_000_000L
                    Computer.choose(
                        m.state,
                        m.difficulty,
                        m.id.hashCode().toLong() xor m.revision,
                    ) {
                        !job.isActive || System.nanoTime() >= deadline
                    }
                }
            val current = _ui.value
            if (
                ticket == generation &&
                    current.match?.id == m.id &&
                    current.match.revision == m.revision &&
                    !current.paused &&
                    current.screen == Screen.GAME &&
                    chosen != null
            )
                submit(chosen.action, Actor.COMPUTER)
        }
    }

    fun hint() {
        if (!playable()) return
        val m = _ui.value.match ?: return
        val ticket = generation
        _ui.value = _ui.value.copy(busy = true, message = null)
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.Default) {
                    val end = System.nanoTime() + 1_000_000_000L
                    Computer.choose(m.state, Difficulty.EXPERT, 712L) { System.nanoTime() >= end }
                }
            val s = _ui.value
            if (s.match?.id == m.id && s.match.revision == m.revision)
                _ui.value =
                    s.copy(
                        busy = false,
                        hint = result?.action.takeIf { ticket == generation && !s.paused },
                        selected =
                            result?.action?.origin.takeIf { ticket == generation && !s.paused },
                    )
        }
    }

    fun undo() {
        if (!playable()) return
        val m = _ui.value.match ?: return
        if (GameEngine.undoPrefix(m.state, m.human) == null) return
        cancelComputer()
        _ui.value = _ui.value.copy(busy = true, selected = null, hint = null)
        viewModelScope.launch {
            try {
                val next = withContext(Dispatchers.IO) { store.undo(m) }
                _ui.value =
                    _ui.value.copy(
                        match = next,
                        busy = false,
                        lastAction = (next.state.events.lastOrNull() as? Event.Played)?.action,
                        message = null,
                    )
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun preference(key: String, value: Boolean) {
        val p = _ui.value.preferences
        val next =
            when (key) {
                "dark" -> p.copy(dark = value)
                "sound" -> p.copy(sound = value)
                "vibration" -> p.copy(vibration = value)
                else -> p.copy(reduced = value)
            }
        _ui.value = _ui.value.copy(preferences = next)
        viewModelScope.launch {
            preferenceMutex.withLock {
                try {
                    withContext(Dispatchers.IO) { store.setPreference(key, value.toString()) }
                } catch (e: Exception) {
                    _ui.value =
                        _ui.value.copy(
                            preferences =
                                if (_ui.value.preferences == next) p else _ui.value.preferences,
                            message = R.string.preference_failed,
                        )
                }
            }
        }
    }

    fun lesson(index: Int) {
        val p =
            when (index) {
                0 -> GameEngine.opening
                1 -> Position(mask("ADE"), mask("BCFGHILMNOP"), Side.LAMB, 0, 4)
                2 -> GameEngine.opening.copy(turn = Side.TIGER)
                3 -> Position(mask("AEW"), mask("CO"), Side.TIGER, 13, 0)
                4 -> Position(mask("ADE"), mask("C"), Side.TIGER, 10, 4)
                else -> Position(mask("ADE"), mask("BCFGHILMNOP"), Side.LAMB, 0, 4)
            }
        val state = (GameEngine.fixture(p) as Transition.Applied).state
        _ui.value =
            _ui.value.copy(
                screen = Screen.TUTORIAL,
                lesson = index,
                lessonState = state,
                selected = null,
                lessonSuccess = false,
                message = null,
            )
    }

    private fun mask(s: String) = s.fold(0) { m, c -> m or Node.valueOf(c.toString()).bit }

    private fun tutorialTap(node: Node) {
        val s = _ui.value
        val state = s.lessonState ?: return
        if (s.lessonSuccess || s.lesson == 5) return
        val actions = GameEngine.available(state)
        val action =
            if (state.position.turn == Side.LAMB && state.position.reserve > 0)
                actions.firstOrNull { it.destination == node }
            else actions.firstOrNull { it.origin == s.selected && it.destination == node }
        if (action == null) {
            _ui.value =
                s.copy(
                    selected = node.takeIf { n -> actions.any { it.origin == n } },
                    message =
                        if (actions.any { it.origin == node }) null else R.string.illegal_action,
                )
            return
        }
        if (
            s.lesson == 3 && action !is Action.Capture || s.lesson == 4 && action !is Action.Capture
        ) {
            _ui.value = s.copy(message = R.string.try_capture)
            return
        }
        _ui.value =
            s.copy(
                lessonState = (GameEngine.apply(state, action) as Transition.Applied).state,
                lessonSuccess = true,
                selected = null,
                message = R.string.lesson_complete,
            )
    }

    fun repetitionLesson() {
        val s = _ui.value
        var state = s.lessonState ?: return
        if (s.lesson != 5 || s.lessonSuccess) return
        val cycle =
            listOf(
                Action.Step(Node.P, Node.Q),
                Action.Step(Node.D, Node.J),
                Action.Step(Node.Q, Node.P),
                Action.Step(Node.J, Node.D),
            )
        state = (GameEngine.apply(state, cycle[state.eventCount % 4]) as Transition.Applied).state
        _ui.value =
            s.copy(
                lessonState = state,
                lessonSuccess = state.outcome != Outcome.ONGOING,
                message = if (state.outcome != Outcome.ONGOING) R.string.lesson_complete else null,
            )
    }

    override fun onCleared() {
        cancelComputer()
        store.close()
    }
}
