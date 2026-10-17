# Routine implementation choices and acceptance targets

Status: technical/visual working choices under D039, 2026-10-17. Existing product rules remain authoritative. These choices remove routine interruptions during an authorized implementation run; measurable results, rendered review and Tamil review are still required.

## Pure engine and app coordination

Use explicit immutable Kotlin classes with defensive copies and private backing storage; expose no mutable aliases. Avoid adding a collection library solely for a 23-node board. Sealed action/result types and stable textual IDs implement D038; public construction goes through validation. Keep the board topology in one source derived from D017. Rendering reads normalized coordinates independently of legality. Add a separate fixture factory available to tests/tutorial assembly, never to production Continue import.

One app coordinator serializes UI, AI, storage and protocol events. Publish immutable screen state through StateFlow; Compose renders that state and submits intents. Screens do not directly mutate the engine or database. ViewModel owns configuration-lifetime state. Cancellable AI work runs off the main thread with a match ID, local revision and generation token; check all three before admission. Pauses increment task generation and cancel work; an eligible explicit Resume can schedule a fresh search from the current state.

Use a small sealed route model and explicit back stack owned by app rather than a new navigation framework initially. Restore only safe UI identifiers; a fresh launch always opens Home. Input gates depend on mode, turn, pause, operation pending, validity and completion. Keep terminal and recovery screens separate. A Play Friend entry remains visible in a Solo-first development build but explains that Nearby is unavailable until its integration gate is satisfied; it must never simulate successful pairing.

## AI and Hint

Start with a seeded uniform legal-action baseline and an iterative-deepening alpha-beta policy. Both call engine legalActions/applyAction; neither repeats capture/adjacency rules. Terminal outcomes dominate evaluation. Heuristic factors are captured lambs, tiger legal mobility/capture threats and lamb containment; weights are tuning details, not new rules. Use complete repetition history in search state; depth exhaustion or a benchmark cap never creates an in-game draw. Cancellation checks occur regularly within search.

Implement easiest and strongest first, then tune the middle levels using fixed seed streams and node budgets. Initial experimental budgets: 1, 500, 2,500, 10,000 and 40,000 expanded nodes for Very Easy through Expert, with a 1-second foreground search ceiling and a best-completed-iteration fallback. Budgets may change based on measured results without changing approved difficulty names or promising optimal play. Return a legal fallback if interrupted before an iteration completes; cancellation due to pause/revision change discards the result entirely.

Hint uses the strongest available bounded policy on the current Solo human turn, returns one legal action without applying it, and shows no guarantee of optimality. It uses separate random state and cannot advance match/repetition history. No Nearby strategy hint or Undo path exists.

| Evaluation | Proposed acceptance target, not an achieved result |
| --- | --- |
| Legality | Zero illegal actions, invalid-state publications or crashes across approved fixtures and 10,000 seeded generated transitions. Reproducible seeds and history on failure. |
| Tactical fixtures | Expand E01–E21 into complete consistent fixtures; add at least 20 reviewed forced-win/avoid-loss and containment cases spanning both sides and phases. Classify expected best actions from verified bounded search/manual rule analysis; do not invent a perfect-play oracle. |
| Relative strength | At least 200 paired seeds per adjacent-level comparison, swapping sides (400 games per comparison). Stronger policy's aggregate score >55%, counting legitimate draws as half; publish confidence intervals and results separately by side. |
| Inconclusive long games | Evaluation-only cap at 2,000 actions or 120 seconds per game. Report censored games separately; no synthetic draw. If >5% are censored or interval includes 50%, increase evidence/retune before claiming clear progression. |
| Response time | On the nominated reference physical device: p95 <=1 second for AI and Hint, with no main-thread search. Report emulator timings separately until a physical reference exists. |
| Lifecycle | Pause/new match/Undo cancels or invalidates results; no duplicate moves after restart; hint does not change subsequent deterministic evaluation seed streams. |

These are engineering targets for evaluation, not evidence that five meaningfully distinct levels can be guaranteed in advance. If tuning cannot establish D026 progression, present the measured limitation for a product decision; never silently remove/rename levels or claim Expert is unbeatable.

## Visual and interaction choices

Use Material 3 controls, original vector piece symbols and programmatic board lines from the approved topology. Do not ship the source screenshot or assume its artwork license. Lambs use a rounded light piece with a lamb mark; tigers use an angular amber piece with a tiger mark. Distinguish selection/destinations/captures by outline, shape and text as well as colour. Node IDs A–W and accessible descriptions remain stable across themes.

Modern: warm light surface, dark text/board lines, restrained green selection accent. Midnight Prowl: deep neutral surface, light text/board lines, amber/teal accents. Pick exact tokens during Compose implementation and measure text contrast >=4.5:1 and control/board distinguishability in rendered output. No decorative texture behind the board. Use the system font to support Tamil without adding unreviewed font assets.

Use board-above-controls in narrow layouts and a side panel at >=600 dp available width when both regions meet the minimum usable sizes. Insets never shrink controls below their accessible sizes. At dense points, do not invent overlapping 48 dp hit regions that select the wrong node: provide the approved explicit point list with 48 dp rows and current legal-action labels. TalkBack uses semantic node controls and predictable focus, with a concise turn/result announcement. Scroll and wrap supporting text at 200% scale.

Use short generated move/capture/result tones with Sound Effects Off by default; no external music asset. Use supported platform haptic feedback with independent Vibration Off. Honour system restrictions and explain unavailable feedback. Decorative transitions are brief and removed with Reduced Motion; essential state changes remain clear. Rotation/resize cannot change board, roles, history, pending transaction or pause state.

## Tutorial and language preparation

Lesson sequence: opening/placement (E01–E02), lamb movement after reserve exhaustion (E12), tiger step (E03), optional straight single capture and illegal turns/chains (E04–E11), trapping/fifth capture (E13–E16/E20), then repetition/agreed draw (E17–E19). Use complete isolated fixtures; failure explanations follow the actual engine reason. Explain agreement through an illustrative lesson, not a functioning second-player or Pass & Play mode. No tutorial progress is persisted.

All UI copy is resource-based, including accessibility, validation and error text. Preserve approved English labels in the brief. Provide `values` and `values-ta`; system locale selects Tamil variants, unsupported locales use English. Stable difficulty IDs do not use translated text. Draft Tamil translation can be authored during implementation, but fluent review of terminology, difficulty names, wrapping and TalkBack pronunciation is a feature-completion gate. No reviewer or reviewed Tamil copy exists yet; do not label machine draft text as approved.

## Boundaries for uninterrupted work

Routine refactoring, bug fixes, evidence-driven heuristic tuning, exact colour/spacing choices, compatibility corrections and tests within these contracts can proceed without individual approval once implementation is authorized. A change to approved game rules, save-loss/recovery policy, peer identity/authority, five-level product scope, language scope, release identity or publishing/signing remains a material decision. Report a failed check honestly; an uninterrupted run cannot promise to eliminate all unforeseen blockers.

## Implemented Expert evaluation

The initial common mobility/capture-count evaluation gave an inconclusive Expert progression trial. Expert now also weights legal capture opportunities and partially trapped tigers, using immutable metrics supplied by `game`; policies still do not define legality. Terminal wins/losses dominate and prefer earlier wins/later unavoidable losses. Node budgets and the 1-second deadline are unchanged. Very Easy–Hard policies are unchanged. See the [implementation evidence](../implementation/solo-first-evidence.md) for completed comparisons and remaining physical latency qualification.
