package com.maswadkar.lambandtiger

import com.maswadkar.lambandtiger.game.*

/** Presentation of one committed engine action, never a source of game state. */
internal data class BoardMotion(val before: GameState, val after: GameState, val action: Action) {
    val durationMillis: Int
        get() = when (action) {
            is Action.Place -> 260
            is Action.Step -> 240
            is Action.Capture -> 360
        }
}

/** Restoration, undo, history replacement and agreed draws must snap, not replay moves. */
internal fun boardMotion(before: GameState, after: GameState): BoardMotion? {
    if (after.eventCount != before.eventCount + 1 || before.fixtureOrigin != after.fixtureOrigin)
        return null
    if (after.events.dropLast(1) != before.events) return null
    val action = (after.events.lastOrNull() as? Event.Played)?.action ?: return null
    val applied = (GameEngine.apply(before, action) as? Transition.Applied)?.state ?: return null
    if (applied.position != after.position || applied.outcome != after.outcome) return null
    return BoardMotion(before, after, action)
}
