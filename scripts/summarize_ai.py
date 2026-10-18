#!/usr/bin/env python3
"""Summarize evaluateAi output using side-swapped seeds as bootstrap units.

Usage: python3 scripts/summarize_ai.py path/to/evaluation.log [seeds=200]
Only completed seed pairs enter the observed score/CI. Censoring is separately
reported with conservative score/CI bounds, never relabelled as an actual draw.
This is a head-to-head sample, not an Elo rating or proof of optimal play.
"""

import csv
import json
import math
import random
import sys
from collections import defaultdict
from pathlib import Path


def summarize(path, seeds):
    rows = defaultdict(dict)
    for line in Path(path).read_text().splitlines():
        if not line or line[0] not in "0123" or line[1:2] != ",":
            continue
        pair, seed, side, outcome, score, actions, nodes, ms, censored = next(csv.reader([line]))
        key = (int(seed), side)
        assert 0 <= key[0] < seeds and side in ("LAMB", "TIGER")
        assert key not in rows[int(pair)], f"Duplicate row: {pair}/{key}"
        rows[int(pair)][key] = {
            "score": float(score), "censored": censored == "true",
            "outcome": outcome, "actions": int(actions), "milliseconds": int(ms),
        }
    result = []
    for pair, name in enumerate(("Easy vs Very Easy", "Medium vs Easy", "Hard vs Medium", "Expert vs Hard")):
        games = rows[pair]
        complete = [
            (games[(seed, "LAMB")]["score"] + games[(seed, "TIGER")]["score"]) / 2
            for seed in range(seeds)
            if all((seed, side) in games and not games[(seed, side)]["censored"] for side in ("LAMB", "TIGER"))
        ]
        assert all(math.isfinite(score) for score in complete)
        randomizer = random.Random(17017 + pair)
        boot = sorted(sum(randomizer.choices(complete, k=len(complete))) / len(complete) for _ in range(10000)) if complete else []
        mean = sum(complete) / len(complete) if complete else None
        lower, upper = (boot[249], boot[9749]) if boot else (None, None)
        censored = sum(game["censored"] for game in games.values())
        bounded = [
            [games[(seed, side)] for side in ("LAMB", "TIGER")]
            for seed in range(seeds)
            if all((seed, side) in games for side in ("LAMB", "TIGER"))
        ]
        worst = [sum(0 if g["censored"] else g["score"] for g in pair_games) / 2 for pair_games in bounded]
        best = [sum(1 if g["censored"] else g["score"] for g in pair_games) / 2 for pair_games in bounded]
        bounds = [sum(worst) / len(worst), sum(best) / len(best)] if bounded else [None, None]
        randomizer = random.Random(17017 + pair)
        worst_boot = sorted(sum(randomizer.choices(worst, k=len(worst))) / len(worst) for _ in range(10000)) if worst else []
        worst_ci = [worst_boot[249], worst_boot[9749]] if worst_boot else [None, None]
        side_scores = {}
        for side in ("LAMB", "TIGER"):
            scores = [game["score"] for (_, s), game in games.items() if s == side and not game["censored"]]
            side_scores[side] = sum(scores) / len(scores) if scores else None
        result.append({
            "comparison": name, "games": len(games), "complete_pairs": len(complete),
            "censored": censored, "score": mean, "ci95": [lower, upper],
            "side_scores": side_scores,
            "score_bounds": bounds, "worst_case_ci95": worst_ci,
            "outcomes": {outcome: sum(g["outcome"] == outcome for g in games.values()) for outcome in sorted({g["outcome"] for g in games.values()})},
            "meets_sample_target": len(games) == 2 * seeds and censored <= .05 * len(games) and bounds[0] is not None and bounds[0] > .55 and worst_ci[0] > .5,
        })
    return result


if __name__ == "__main__":
    print(json.dumps(summarize(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 200), indent=2))
