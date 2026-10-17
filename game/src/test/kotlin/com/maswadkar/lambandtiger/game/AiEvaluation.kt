package com.maswadkar.lambandtiger.game

import java.util.concurrent.Callable
import java.util.concurrent.Executors

/** Offline reproducible experiment; caps censor results and never change game rules. */
fun main(args: Array<String>) {
    val seeds = args.firstOrNull()?.toInt() ?: 200
    val threads = args.getOrNull(1)?.toInt() ?: 2
    val pairs = args.getOrNull(2)?.split(',')?.map(String::toInt) ?: (0..3).toList()
    require(pairs.isNotEmpty() && pairs.distinct() == pairs && pairs.all { it in 0..3 })
    val pool = Executors.newFixedThreadPool(threads)
    println("pair,seed,strongSide,outcome,strongScore,actions,nodes,milliseconds,censored")
    val tasks = buildList {
        for (pair in pairs) for (seed in 0 until seeds) for (strongSide in Side.entries) add(
            Callable {
                var s = GameEngine.newGame()
                var nodes = 0L
                val start = System.nanoTime()
                var censored = false
                while (s.outcome == Outcome.ONGOING && s.eventCount < 2000) {
                    if (System.nanoTime() - start > 120_000_000_000L) {
                        censored = true
                        break
                    }
                    val difficulty =
                        Difficulty.entries[if (s.position.turn == strongSide) pair + 1 else pair]
                    val deadline = System.nanoTime() + 1_000_000_000L
                    val choice =
                        Computer.choose(s, difficulty, seed.toLong() * 10000 + s.eventCount) {
                            System.nanoTime() > deadline
                        }!!
                    nodes += choice.nodes
                    s = (GameEngine.apply(s, choice.action) as Transition.Applied).state
                }
                if (s.outcome == Outcome.ONGOING) censored = true
                val score =
                    when (s.outcome) {
                        Outcome.TIGERS_WIN -> if (strongSide == Side.TIGER) 1.0 else 0.0
                        Outcome.LAMBS_WIN -> if (strongSide == Side.LAMB) 1.0 else 0.0
                        Outcome.REPETITION_DRAW,
                        Outcome.AGREED_DRAW -> 0.5
                        Outcome.ONGOING -> Double.NaN
                    }
                val row =
                    "$pair,$seed,$strongSide,${s.outcome},$score,${s.eventCount},$nodes,${(System.nanoTime()-start)/1_000_000},$censored"
                synchronized(System.out) { println(row) }
            }
        )
    }
    try {
        pool.invokeAll(tasks).forEach { it.get() }
    } finally {
        pool.shutdown()
    }
}
