package com.maswadkar.lambandtiger

import com.maswadkar.lambandtiger.game.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class ComputerBudgetTest {
    @Test
    fun expertBudgetAndLegalFallbackOnAndroid() {
        val times = mutableListOf<Long>()
        val random = Random(17017)
        repeat(20) { index ->
            var state = GameEngine.newGame()
            repeat(index * 2) {
                if (state.outcome == Outcome.ONGOING)
                    state =
                        (GameEngine.apply(state, GameEngine.available(state).random(random))
                                as Transition.Applied)
                            .state
            }
            if (state.outcome == Outcome.ONGOING) {
                val start = System.nanoTime()
                val end = start + 1_000_000_000
                val result =
                    Computer.choose(state, Difficulty.EXPERT, index.toLong()) {
                        System.nanoTime() >= end
                    }!!
                val elapsed = (System.nanoTime() - start) / 1_000_000
                times.add(elapsed)
                assertTrue(result.action in GameEngine.available(state))
                assertTrue("Budget did not terminate: $elapsed ms", elapsed < 2500)
            }
        }
        val sorted = times.sorted()
        android.util.Log.i(
            "SoloPerformance",
            "Expert samples=${times.size}, p95_ms=${sorted[((sorted.size-1)*.95).toInt()]}, max_ms=${sorted.last()}",
        )
    }
}
