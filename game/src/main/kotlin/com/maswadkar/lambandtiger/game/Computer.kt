package com.maswadkar.lambandtiger.game

import kotlin.random.Random

enum class Difficulty(val nodes: Int) {
    VERY_EASY(1),
    EASY(500),
    MEDIUM(2500),
    HARD(10000),
    EXPERT(40000),
}

data class SearchResult(val action: Action, val nodes: Int, val depth: Int)

object Computer {
    fun choose(
        state: GameState,
        difficulty: Difficulty,
        seed: Long,
        shouldStop: () -> Boolean = { false },
    ): SearchResult? {
        val legal = GameEngine.available(state)
        if (legal.isEmpty()) return null
        val random = Random(seed)
        if (difficulty == Difficulty.VERY_EASY)
            return SearchResult(legal[random.nextInt(legal.size)], 1, 0)
        val side = state.position.turn
        var used = 0
        var interrupted = false
        fun value(s: GameState): Int {
            // Prefer an earlier forced win and keep playing when a loss is unavoidable.
            val distance = s.eventCount - state.eventCount
            val tigerScore =
                when (s.outcome) {
                    Outcome.TIGERS_WIN -> 100000 - distance
                    Outcome.LAMBS_WIN -> -100000 + distance
                    Outcome.REPETITION_DRAW,
                    Outcome.AGREED_DRAW -> 0
                    Outcome.ONGOING -> {
                        if (difficulty == Difficulty.EXPERT) {
                            val activity = GameEngine.tigerActivity(s.position)
                            s.position.captured * 400 +
                                activity.mobility * 12 +
                                activity.captures * 40 - (3 - activity.mobileTigers) * 180
                        } else s.position.captured * 400 + GameEngine.tigerMobility(s.position) * 12
                    }
                }
            return if (side == Side.TIGER) tigerScore else -tigerScore
        }
        fun search(s: GameState, depth: Int, alphaIn: Int, betaIn: Int): Int {
            if (used >= difficulty.nodes || shouldStop()) {
                interrupted = true
                return value(s)
            }
            used++
            if (depth == 0 || s.outcome != Outcome.ONGOING) return value(s)
            var alpha = alphaIn
            var beta = betaIn
            val max = s.position.turn == side
            var best = if (max) -1000000 else 1000000
            for (a in GameEngine.available(s).sortedBy { if (it is Action.Capture) 0 else 1 }) {
                val next = (GameEngine.advanceLegal(s, a) as? Transition.Applied)?.state ?: continue
                val score = search(next, depth - 1, alpha, beta)
                best = if (max) maxOf(best, score) else minOf(best, score)
                if (max) alpha = maxOf(alpha, best) else beta = minOf(beta, best)
                if (beta <= alpha || interrupted) break
            }
            return best
        }
        val ordered = legal.shuffled(random).sortedBy { if (it is Action.Capture) 0 else 1 }
        // Even before a full iteration completes, retain a legal fallback.
        var selected = ordered.first()
        var completed = 0
        for (depth in 1..12) {
            var best = -1000001
            var candidate = selected
            for (a in ordered) {
                val next = (GameEngine.advanceLegal(state, a) as Transition.Applied).state
                val score = search(next, depth - 1, -1000000, 1000000)
                if (score > best) {
                    best = score
                    candidate = a
                }
                if (interrupted) break
            }
            if (interrupted) break
            selected = candidate
            completed = depth
            if (best >= 99000 || used >= difficulty.nodes) break
        }
        return SearchResult(selected, used, completed)
    }
}
