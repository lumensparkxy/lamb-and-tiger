package com.maswadkar.lambandtiger.game

import java.util.Collections

enum class Node {
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
    I,
    J,
    K,
    L,
    M,
    N,
    O,
    P,
    Q,
    R,
    S,
    T,
    U,
    V,
    W;

    val bit: Int
        get() = 1 shl ordinal
}

enum class Side {
    LAMB,
    TIGER;

    fun other() = if (this == LAMB) TIGER else LAMB
}

enum class Occupant {
    EMPTY,
    LAMB,
    TIGER,
}

enum class Phase {
    PLACEMENT,
    MOVEMENT,
}

enum class Outcome {
    ONGOING,
    TIGERS_WIN,
    LAMBS_WIN,
    REPETITION_DRAW,
    AGREED_DRAW,
}

data class Position(
    val tigers: Int,
    val lambs: Int,
    val turn: Side,
    val reserve: Int,
    val captured: Int,
) {
    val phase
        get() = if (reserve > 0) Phase.PLACEMENT else Phase.MOVEMENT

    fun at(node: Node): Occupant =
        when {
            tigers and node.bit != 0 -> Occupant.TIGER
            lambs and node.bit != 0 -> Occupant.LAMB
            else -> Occupant.EMPTY
        }
}

sealed interface Action {
    data class Place(val to: Node) : Action

    data class Step(val from: Node, val to: Node) : Action

    data class Capture(val from: Node, val over: Node, val to: Node) : Action
}

val Action.destination: Node
    get() =
        when (this) {
            is Action.Place -> to
            is Action.Step -> to
            is Action.Capture -> to
        }
val Action.origin: Node?
    get() =
        when (this) {
            is Action.Place -> null
            is Action.Step -> from
            is Action.Capture -> from
        }

sealed interface Event {
    data class Played(val action: Action) : Event

    data object Agreement : Event
}

data class Edge(val from: Node, val to: Node)

data class Path(val from: Node, val over: Node, val to: Node)

data class Point(val x: Float, val y: Float)

data class TigerActivity(val mobility: Int, val captures: Int, val mobileTigers: Int)

object Board {
    private val lines =
        listOf(
            "ACIOT",
            "ADJPU",
            "AEKQV",
            "AFLRW",
            "BCDEFG",
            "HIJKLM",
            "NOPQRS",
            "TUVW",
            "BHN",
            "GMS",
        )
    val edges: List<Edge> =
        Collections.unmodifiableList(
            lines.flatMap { line ->
                line.zipWithNext { a, b ->
                    Edge(Node.valueOf(a.toString()), Node.valueOf(b.toString()))
                }
            }
        )
    val captures: List<Path> =
        Collections.unmodifiableList(
            lines
                .flatMap { line ->
                    line.windowed(3).flatMap { s ->
                        val a = Node.valueOf(s[0].toString())
                        val b = Node.valueOf(s[1].toString())
                        val c = Node.valueOf(s[2].toString())
                        listOf(Path(a, b, c), Path(c, b, a))
                    }
                }
                .sortedWith(compareBy({ it.from.ordinal }, { it.over.ordinal }, { it.to.ordinal }))
        )
    private val adjacency =
        Node.entries.associateWith { n ->
            edges
                .filter { it.from == n || it.to == n }
                .map { if (it.from == n) it.to else it.from }
                .sortedBy { it.ordinal }
                .let { Collections.unmodifiableList(it) }
        }

    fun neighbours(node: Node): List<Node> = adjacency.getValue(node)

    val points: Map<Node, Point> =
        Collections.unmodifiableMap(
            buildMap {
                put(Node.A, Point(0f, 0f))
                listOf(
                        listOf(-3f, -1f, -1f / 3, 1f / 3, 1f, 3f) to 2f,
                        listOf(-4f, -2f, -2f / 3, 2f / 3, 2f, 4f) to 4f,
                        listOf(-5f, -3f, -1f, 1f, 3f, 5f) to 6f,
                        listOf(-4f, -4f / 3, 4f / 3, 4f) to 8f,
                    )
                    .fold(1) { offset, (xs, y) ->
                        xs.forEachIndexed { i, x -> put(Node.entries[offset + i], Point(x, y)) }
                        offset + xs.size
                    }
            }
        )
}

internal class History(val previous: History?, val event: Event?, val size: Int) {
    fun append(event: Event) = History(this, event, size + 1)

    fun events(): List<Event> {
        val list = ArrayList<Event>(size)
        var h: History? = this
        while (h?.event != null) {
            list.add(h.event!!)
            h = h.previous
        }
        list.reverse()
        return Collections.unmodifiableList(list)
    }
}

class GameState
internal constructor(
    val position: Position,
    val outcome: Outcome,
    val fixtureOrigin: Position?,
    internal val history: History,
    repetitions: Map<Position, Int>,
) {
    val ruleset: String
        get() = GameEngine.RULESET

    val events: List<Event>
        get() = history.events()

    val eventCount: Int
        get() = history.size

    val repetitions: Map<Position, Int> = Collections.unmodifiableMap(HashMap(repetitions))
    val sideToMove: Side?
        get() = position.turn.takeIf { outcome == Outcome.ONGOING }

    val isFixture
        get() = fixtureOrigin != null
}

sealed interface Transition {
    data class Applied(val state: GameState, val action: Action? = null) : Transition

    data class Rejected(val reason: String) : Transition

    data class Invalid(val reason: String, val eventIndex: Int? = null) : Transition
}

sealed interface LegalActions {
    data class Available(val actions: List<Action>) : LegalActions

    data class Finished(val outcome: Outcome) : LegalActions

    data class Invalid(val reason: String) : LegalActions
}

object GameEngine {
    const val RULESET = "aadu-puli-23-d017-v1"
    val opening = Position(Node.A.bit or Node.D.bit or Node.E.bit, 0, Side.LAMB, 15, 0)

    fun newGame() =
        GameState(opening, Outcome.ONGOING, null, History(null, null, 0), mapOf(opening to 1))

    fun structuralError(p: Position): String? =
        when {
            (p.tigers or p.lambs) ushr 23 != 0 || p.tigers and p.lambs != 0 -> "InvalidOccupancy"
            Integer.bitCount(p.tigers) != 3 -> "InvalidTigers"
            p.reserve !in 0..15 ||
                p.captured !in 0..5 ||
                Integer.bitCount(p.lambs) + p.reserve + p.captured != 15 -> "InvalidCounts"
            else -> null
        }

    /** Isolated synthetic origin for tests/tutorials; never admitted by production restore. */
    fun fixture(p: Position): Transition {
        structuralError(p)?.let {
            return Transition.Invalid(it)
        }
        if (p.captured == 5 && tigerMobility(p) == 0)
            return Transition.Invalid("ContradictoryOutcomes")
        val result = winner(p)
        if (result == Outcome.ONGOING && p.turn == Side.LAMB && actions(p).isEmpty())
            return Transition.Invalid("LambHasNoLegalAction")
        return Transition.Applied(GameState(p, result, p, History(null, null, 0), mapOf(p to 1)))
    }

    fun legalActions(s: GameState): LegalActions {
        structuralError(s.position)?.let {
            return LegalActions.Invalid(it)
        }
        if (s.outcome != Outcome.ONGOING) return LegalActions.Finished(s.outcome)
        val actions = actions(s.position)
        if (actions.isEmpty())
            return LegalActions.Invalid(
                if (s.position.turn == Side.LAMB) "LambHasNoLegalAction" else "OutcomeMismatch"
            )
        return LegalActions.Available(Collections.unmodifiableList(actions))
    }

    fun available(s: GameState): List<Action> =
        (legalActions(s) as? LegalActions.Available)?.actions ?: emptyList()

    /** Raw position query for tactical evaluation, without defining a new turn or transition. */
    fun tigerMobility(p: Position): Int {
        var count = 0
        for (node in Node.entries) if (p.tigers and node.bit != 0) {
            for (to in Board.neighbours(node)) if ((p.tigers or p.lambs) and to.bit == 0) count++
        }
        for (path in Board.captures) if (
            p.tigers and path.from.bit != 0 &&
                p.lambs and path.over.bit != 0 &&
                (p.tigers or p.lambs) and path.to.bit == 0
        )
            count++
        return count
    }

    /** Legal mobility and containment features; policies do not reproduce board legality. */
    fun tigerActivity(p: Position): TigerActivity {
        var mobility = 0
        var captures = 0
        var mobile = 0
        val occupied = p.tigers or p.lambs
        for (node in Node.entries) if (p.tigers and node.bit != 0) {
            for (to in Board.neighbours(node)) if (occupied and to.bit == 0) {
                mobility++
                mobile = mobile or node.bit
            }
        }
        for (path in Board.captures) if (
            p.tigers and path.from.bit != 0 &&
                p.lambs and path.over.bit != 0 &&
                occupied and path.to.bit == 0
        ) {
            mobility++
            captures++
            mobile = mobile or path.from.bit
        }
        return TigerActivity(mobility, captures, Integer.bitCount(mobile))
    }

    private fun actions(p: Position): List<Action> {
        val result = ArrayList<Action>()
        if (p.turn == Side.LAMB && p.reserve > 0) {
            Node.entries
                .filter { p.at(it) == Occupant.EMPTY }
                .forEach { result.add(Action.Place(it)) }
            return result
        }
        val species = if (p.turn == Side.LAMB) Occupant.LAMB else Occupant.TIGER
        Node.entries
            .filter { p.at(it) == species }
            .forEach { from ->
                Board.neighbours(from)
                    .filter { p.at(it) == Occupant.EMPTY }
                    .forEach { result.add(Action.Step(from, it)) }
            }
        if (p.turn == Side.TIGER)
            Board.captures.forEach { path ->
                if (
                    p.at(path.from) == Occupant.TIGER &&
                        p.at(path.over) == Occupant.LAMB &&
                        p.at(path.to) == Occupant.EMPTY
                )
                    result.add(Action.Capture(path.from, path.over, path.to))
            }
        return result
    }

    private fun winner(p: Position): Outcome =
        when {
            p.captured == 5 -> Outcome.TIGERS_WIN
            tigerMobility(p) == 0 -> Outcome.LAMBS_WIN
            else -> Outcome.ONGOING
        }

    fun apply(s: GameState, action: Action): Transition {
        val legal = legalActions(s)
        if (legal is LegalActions.Invalid) return Transition.Invalid(legal.reason)
        if (legal is LegalActions.Finished) return Transition.Rejected("Finished")
        if (action !in (legal as LegalActions.Available).actions)
            return Transition.Rejected("IllegalAction")
        return advanceLegal(s, action)
    }

    /** Module-internal search path; callers must take the action from legalActions(s). */
    internal fun advanceLegal(s: GameState, action: Action): Transition {
        val p = s.position
        val next =
            when (action) {
                is Action.Place -> p.copy(lambs = p.lambs or action.to.bit, reserve = p.reserve - 1)
                is Action.Step ->
                    if (p.turn == Side.LAMB)
                        p.copy(lambs = (p.lambs xor action.from.bit) or action.to.bit)
                    else p.copy(tigers = (p.tigers xor action.from.bit) or action.to.bit)
                is Action.Capture ->
                    p.copy(
                        tigers = (p.tigers xor action.from.bit) or action.to.bit,
                        lambs = p.lambs xor action.over.bit,
                        captured = p.captured + 1,
                    )
            }.copy(turn = p.turn.other())
        structuralError(next)?.let {
            return Transition.Invalid(it)
        }
        var result = winner(next)
        val occurrences = HashMap(s.repetitions)
        if (result == Outcome.ONGOING) {
            val count = (occurrences[next] ?: 0) + 1
            occurrences[next] = count
            if (count == 3) result = Outcome.REPETITION_DRAW
        }
        if (result == Outcome.ONGOING && next.turn == Side.LAMB && actions(next).isEmpty())
            return Transition.Invalid("LambHasNoLegalAction")
        return Transition.Applied(
            GameState(
                next,
                result,
                s.fixtureOrigin,
                s.history.append(Event.Played(action)),
                occurrences,
            ),
            action,
        )
    }

    fun completeAgreedDraw(s: GameState): Transition =
        if (s.outcome != Outcome.ONGOING) Transition.Rejected("Finished")
        else
            GameState(
                    s.position,
                    Outcome.AGREED_DRAW,
                    s.fixtureOrigin,
                    s.history.append(Event.Agreement),
                    s.repetitions,
                )
                .let { Transition.Applied(it) }

    fun replay(
        events: List<Event>,
        ruleset: String = RULESET,
        allowAgreement: Boolean = false,
    ): Transition {
        if (ruleset != RULESET) return Transition.Invalid("UnknownRuleset")
        return replayFrom(newGame(), events, allowAgreement)
    }

    fun replayFixture(origin: Position, events: List<Event>): Transition {
        val initial = fixture(origin)
        return if (initial is Transition.Applied) replayFrom(initial.state, events, true)
        else initial
    }

    private fun replayFrom(
        initial: GameState,
        events: List<Event>,
        allowAgreement: Boolean,
    ): Transition {
        var s = initial
        for ((i, e) in events.withIndex()) {
            val next =
                when (e) {
                    is Event.Played -> apply(s, e.action)
                    Event.Agreement ->
                        if (allowAgreement) completeAgreedDraw(s)
                        else Transition.Invalid("AgreementNotAdmitted")
                }
            if (next !is Transition.Applied) return Transition.Invalid("HistoryMismatch:$next", i)
            s = next.state
        }
        return Transition.Applied(s)
    }

    fun undoPrefix(s: GameState, human: Side): Int? {
        if (s.isFixture || s.outcome != Outcome.ONGOING || s.position.turn != human) return null
        val events = s.events
        return events.indices.lastOrNull {
            events[it] is Event.Played && (if (it % 2 == 0) Side.LAMB else Side.TIGER) == human
        }
    }
}
