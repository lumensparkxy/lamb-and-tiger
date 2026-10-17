# Solo-first implementation evidence

Recorded 2026-10-17 under D041, reviewed for branch delivery under D042. Work is on `codex/solo-first`, based on `ec1d4dd20a0777545832cd7322ac436133e20e76`. Revision-bound delivery references are recorded on the relevant GitHub issues. Existing planning/harness work is preserved. Nearby integration/testing remains deferred; this is a locally verified Solo development build, not complete V1 or release readiness.

## Implemented scope

- Three modules and pinned Kotlin/Compose build. `game` is pure Kotlin; `nearby` is an inert transport boundary. Play Friend explains that it is unavailable in this development build.
- D017 topology, immutable positions/actions/history, legal moves, captures, wins, repetition, replay, isolated fixtures and bounded computer policies.
- Both Solo sides and all five approved difficulty choices; legal input/highlights, latest-action/capture feedback, terminal results, Play Again and Switch Sides.
- App-private SQLite saves, durable pending operations, exact-intent recovery, protected replacement, contextual Continue, per-action saving, revision-bound computer cancellation, Hint and full-turn Undo. Production restoration replays the approved opening; fixtures cannot be imported.
- Modern/Midnight Prowl, optional sound/haptics, reduced motion, adaptive board and 48 dp point controls, illustrated rules and six isolated interactive lessons.
- English/Tamil resources with all 117 keys present in both languages. Tamil remains a draft pending fluent review.
- Doctor, dependency locks/checksums, build/unit/lint/emulator commands, and CI configuration. Remote verification is tracked in [PR #15](https://github.com/lumensparkxy/lamb-and-tiger/pull/15).

## Verified automated checks

Commands and environment are in [README](../../README.md). Local checks use JBR 25.0.3, Gradle 9.6.0, compile SDK 37 / target 36 / minimum 29 and Build Tools 36.0.0. The emulator is Pixel9a, Android 16 / API 36, `emulator-5554`.

| Check | Result and scope |
| --- | --- |
| Debug build | Passed; installable APK with the approved `.debug` ID. |
| Pure engine | 14 tests passed, including grouped E01–E21 cases, 10,000 seeded transitions, immutable views, replay/Undo, exact graph counts and search legality. |
| Tactical coverage | 20 fifth-capture fixtures, placement containment cases and 20 defensive fixtures spanning both sides/phases. Fixed defensive labels are independently enumerated two-ply outcomes and verified against all immediate replies; no perfect-play or opening-reachability claim. |
| Storage codec | 5 JVM tests passed: canonical golden digest/actions, round trips, truncation, malformed/unknown formats, history/snapshot agreement and fixture rejection. |
| Android | 14 tests passed: storage failure boundaries/restart/replacement/slot isolation, both-side Solo games/rematches, Hint/Undo, lifecycle/selection, settings, learning isolation and unavailable Nearby. |
| Accessible point controls | The complete-game test activates the actual semantic point buttons for both sides. Its focused check and the final full 14-test suite passed. This is not an additional unique test or a full TalkBack qualification. |
| Search timing | 20 Expert samples on the API 36 emulator: p95 1000 ms, max 1000 ms under a 1-second search deadline. Legal fallback and termination passed. This excludes the UI's thinking delay and is not the physical-device latency gate. |
| Lint | Zero errors. Four reviewed app warnings: approved initial target 36 rather than 37, newer Gradle available, newer Kotlin/compiler-plugin patch available. Nearby lint has no findings. |
| Repository/configuration | Whitespace, local document links, unique D001–D042, matching language keys, dependency locks and SHA-256 verification metadata checked. No credentials/local SDK paths/build outputs added to shared configuration. |

There are **33 unique passing automated tests** (14 engine + 5 codec + 14 Android). The inert Nearby module has no unit-test source and no transport tests; a NO-SOURCE task is not connectivity evidence.

Defects found during verification were fixed: duplicated generated classes in the synced workspace (outputs now use a checkout-specific directory under Gradle's local user home), and equal terminal-loss scores that could choose an immediate loss over continued resistance (search now prefers earlier wins and later unavoidable losses). Large-text status wrapping and system-bar theme contrast were also corrected. Additional regressions verify that leaving/reopening setup during the initial save cannot reopen a game, and that a damaged match still preserves saved preferences on the recovery screen.

## Rendered and lifecycle evidence

Inspected Home, setup, game, replacement confirmation, themes/settings and learning screens. Final game status uses the approved mode/phase/count wording. Checked original board geometry and shape distinctions in light/dark themes; alternative point controls retain accessible labels when canvas node labels are hidden at large font sizes.

Window cases included the normal Pixel9a display, 320 × 640, 600 × 448 and 1000 × 850 at density 160, and an undersized 280 × 450 window. These are display sizes; system insets reduce the content window. Supporting content scrolls, wide layouts put controls beside the board, and a window below the approved content minimum pauses play and explains how to continue. Resize preserved the two-move match. English and regional Tamil (`ta-IN`, `ta-LK`) were checked at 200% text; unsupported `de-DE` fell back to English. Fluent terminology/pronunciation review is still required.

Manual force-stop/relaunch opened Home and restored the exact two-move match through Continue, paused, with 14 lambs remaining and the latest move retained. Cancelling a new-game replacement preserved that save. Automated games also verified terminal outcomes and fresh rematches on both sides. The rendered capture lesson completed A–I over C, and returning to Continue retained the original two-move real match. The final APK also restored an Expert/Tiger match after the computer opening: one move, 14 lambs remaining, paused, with the saved difficulty intact. Scoped launch logs contained no fatal exception or ANR entry.

Measured selected text/button colour pairs are 6.75:1–14.91:1 across the two themes, above the 4.5:1 target. This is a palette calculation plus rendered inspection, not a claim that every screen has undergone a full accessibility audit. TalkBack 16 is installed and its service was bound briefly; injected ADB input does not establish spoken navigation quality, so the complete TalkBack/pronunciation pass remains open. Original font size, display size/density, locale override and accessibility settings were restored.

## Difficulty evaluation

Evaluated **1,600 games**: 200 side-swapped seeds per adjacent comparison, 400 games each. Of these, 1,597 reached a game outcome and three Expert games were censored by the evaluation guard. All four comparisons satisfy the selected aggregate progression target (>55% score, 95% interval above 50%, censoring below 5%).

| Stronger vs weaker | Completed-pair score | 95% paired-seed CI | Stronger as Lamb | Stronger as Tiger | Censored |
| --- | ---: | --- | ---: | ---: | ---: |
| Easy vs Very Easy | 88.38% | 86.62–90.12% | 76.75% | 100.00% | 0/400 |
| Medium vs Easy | 74.38% | 71.62–76.88% | 65.00% | 83.75% | 0/400 |
| Hard vs Medium | 65.12% | 62.25–68.00% | 67.75% | 62.50% | 0/400 |
| Expert vs Hard | 73.35% | 71.70–75.00% | 97.75% | 48.98% | 3/400 |

Score is win = 1, legitimate repetition draw = 0.5, loss = 0. The score/CI uses complete side-swapped pairs (200 for each lower comparison; 197 for Expert). Side scores use completed games on that side. Confidence intervals are percentile bootstrap intervals over whole seed pairs, 10,000 resamples with fixed analysis seeds; games within a pair are not treated as independent observations.

For Expert, assigning every censored outcome a loss gives a conservative all-400 score of **73.00%**, with a paired bootstrap interval of **71.38–74.63%**; assigning them wins gives an upper score bound of **73.75%**. The three raw outcomes remain ONGOING/unknown. They were never converted to draws or actual losses. The guard is 2,000 actions or 120 seconds checked between actions; it is not an app game rule.

Very Easy–Hard reuse their complete comparisons from `ai-evaluation-baseline.log`; their policies did not change when Expert-only containment/capture features were added. That exploratory job was intentionally stopped during its superseded Expert trial, so its final Gradle status is termination, not a rules failure. `ai-evaluation-expert.log` completed successfully for the revised Expert policy. `ai-evaluation-final.csv` combines only the four selected complete batches, checking exactly one row per (comparison, seed, side). Superseded Expert rows are excluded.

The runs use host JBR 25.0.3 with three workers and the same node budgets/one-second search ceiling used by the policies. This is seeded head-to-head evidence, not a held-out perfect-play oracle, universal skill rating, or physical-device latency/strength qualification. Device deadlines can change completed search depth. Per-side results are reported because the game is asymmetric.

Reproduce all comparisons with the README command. `-Ppairs=3` selects only Expert vs Hard for focused tuning. Analyze using `python3 scripts/summarize_ai.py work/solo-qa-20261017/ai-evaluation-final.csv`.

Final CSV SHA-256: `3318c99923d2b2f71fff5537430e09d14e267f95eab4d84f3bc5776d78985453`.

## Artifact identity and local evidence

- Debug APK: `artifacts/local/lamb-and-tiger-debug.apk`, 30,583,075 bytes.
- APK SHA-256: `bd0f6c08db4697110e0ff6786244fa15b8b0d1ea98888a422a6e0a8e2e767c9d`.
- APK signature verified (v2, one debug signer). Application ID `com.maswadkar.lambandtiger.debug`, version `0.1-solo` / 1. Declared permissions are VIBRATE and the generated own-app signature receiver permission; no internet or Nearby radio permissions.
- Source/configuration snapshot: 48 files; SHA-256 of `work/solo-qa-20261017/source-manifest.json` is `0e522b10100d82a7a81f0f23e73841259a41c0efe97452f50f6668ebf92a7c92`. This binds the tested production/build/test inputs; the pre-commit review verified all 48 file hashes still match.
- Ignored local evidence directory: `work/solo-qa-20261017/`. Current automated evidence is `qualified-verification.log`, `qualified-test-runner.log` and `reports/` (including the full 14-test Android XML report). The directory also contains earlier focused checks, AI CSV/log/summary, scoped launch log, resource/contrast checks and rendered screenshots (`qualified-home.png`, `qualified-game.png`, `final-home.png`, `final-game.png`, `final-protected-replace.png`, `final-narrow320.png`, `final-landscape600.png`, `final-tablet1000.png`, `final-tamil200.png`, `final-english200.png`, dark-theme/settings and undersized-window captures).

Earlier incomplete Expert trials and screenshots are retained as diagnostic history. The first three complete comparisons use the unchanged Very Easy–Hard policies; Expert is evaluated separately after adding legal capture-opportunity and partial-containment features. Final reporting excludes the superseded Expert rows. Device logs, screenshots and APKs remain outside version control.

## Remaining gates

Nearby transport and two-physical-phone testing are deferred. Fluent Tamil review, complete TalkBack review, physical sound/haptic checks, a nominated physical AI/Hint latency reference, and the broader API 29/34/37 runtime matrix remain separate from API 36 emulator evidence. Remote results and subsequent CI-only fixes are tracked in PR #15; the local checks above bind the original tested snapshot. Signing custody, distribution, audience/store disclosures and publication are release work. D042 authorizes the reviewed branch commit/push and issue evidence updates. It does not authorize merging or release publication.

## Pre-commit review

Reviewed engine topology/transitions, search legality and cancellation, codec validation, durable SQLite recovery/replacement, app lifecycle/navigation, Compose interaction/accessibility controls, tests and CI configuration. No blocking implementation findings remained. Corrected stale delivery/AI-status wording and recorded D042 without changing the tested production/build inputs. The source manifest, APK and AI CSV checksums match the evidence above; XML reports confirm 33 tests with zero failures/errors/skips. Re-ran the documented doctor and build/unit/lint command successfully; unchanged tests were up-to-date. Existing API 36 rendered/instrumented evidence remains applicable; no new physical or Nearby check was performed.

Issue updates distinguish implemented Solo criteria from pending platform/language/accessibility/release gates. Issues remain open: branch delivery is not a merge or complete V1 acceptance. The pre-existing diagnostic scaffold and its historical smoke report are preserved separately from the production modules. Generated artifacts and device logs remain ignored. Staged whitespace checks passed with `cr-at-eol` enabled for the standard CRLF Windows Gradle wrappers; local Markdown targets and the D001–D042 sequence passed.

## Remote CI follow-up under D043

[PR #15](https://github.com/lumensparkxy/lamb-and-tiger/pull/15) runs the Linux build/unit/lint job and API 29/34/36/37.0 emulator jobs. The initial [run 39067438872](https://github.com/lumensparkxy/lamb-and-tiger/actions/runs/39067438872) exposed a missing verification entry for `org.apache.groovy:groovy-bom:4.0.29` POM in lint resolution. The existing lock already selected this version. Added only its exact SHA-256 (`c24277dec93f146bcda25f5ae4391d6527e384e2132efa32184c1e852b42bca9`), verified against Maven Central’s published checksum and the local cached POM. The API 29/34/36 emulator jobs similarly exposed the missing `org.junit:junit-bom:5.13.3` POM checksum before instrumentation could start; added its exact Maven Central-verified SHA-256 (`e3b93e9bb8871969cf11c0e8ff10f507841db1885ca10578e290846f660fd68e`). Dependency verification remains strict. Local app/Nearby lint and all 14 API 36 Android tests passed with dependency metadata refreshed. The superseded API 37 job was still running when the fix was pushed; its result is not counted as a test failure or pass.

The original 48-file manifest and APK above identify the local d4be01d snapshot; the subsequent verification-metadata edit does not change production source. Final remote results are recorded against the PR head in its verification section. Remote emulator success does not establish physical Nearby, fluent Tamil, full TalkBack or release readiness.

The second [run 39068159544](https://github.com/lumensparkxy/lamb-and-tiger/actions/runs/39068159544) hit a premature-EOF API 37 system-image download. Action source/log inspection also showed emulator-runner overriding PATH to the preinstalled tools 12.0 despite setup-android selecting build 16111833. The workflow now aligns the `latest` alias to the pinned tools and retries image installation at most three times; test execution is not retried or ignored. API 37 had remained offline with the default AVD in the superseded first run. CI now specifies a Pixel 2 profile, 2 GB RAM and 256 MB heap so virtual hardware is explicit across the matrix. These setup changes require the subsequent remote results in PR #15; they do not prove the cause of the earlier boot stall.
