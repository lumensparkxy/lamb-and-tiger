# Issue #4, step 2 — immutable game-state and action contract

Status: **approved by the user**, 2026-10-17, recorded in D038. This contract defines the `game` domain boundary within the module architecture approved in D037. Approval does not create Kotlin classes, implement an engine, select persistence technology, or define the Nearby protocol.

Authority: [approved rules and examples E01–E21](../product/game-rules.md), [product brief](../product/brief.md) D020–D024/D028/D032, and [approved module boundaries](module-boundaries.md). Nearby investigation remains deferred under D036. All type and operation names below describe the approved semantics; they are not an existing Kotlin API.

## 1. Immutable model

| Concept | Approved contents / meaning |
| --- | --- |
| `NodeId` | Exactly the approved A–W points. No arbitrary coordinate can become a node. |
| `Side` | `Lamb` or `Tiger`, independent of human/computer identity or host/join roles. |
| `Occupant` | `Empty`, `Lamb`, or `Tiger`. Same-species pieces have no rule identity. |
| `BoardDefinition` | The fixed D017 topology: 23 nodes, 38 undirected edges, 56 directed straight capture paths. Rendering coordinates do not determine legality. |
| `Position` | Occupancy for all 23 nodes, `turnMarker`, `reserveLambs`, and `capturedLambs`. `phase` is derived: Placement when reserve > 0, Movement when reserve = 0. |
| `Outcome` | `Ongoing`, `TigersWin(FiveCaptured)`, `LambsWin(AllTigersTrapped)`, `Draw(ThreefoldRepetition)`, or `Draw(ByAgreement)`. No interruption, resignation, or invalid-state outcome. |
| `GameHistory` | Immutable origin plus ordered accepted domain events. Production origin is always the approved opening; events contain board actions and, if admitted by `app`, an agreed-draw completion. An agreed-draw event is not a board action. |
| `GameState` | Ruleset identity, current `Position`, `Outcome`, and `GameHistory`. Repetition counts and board-action count are derived from history; an immutable cache may be kept only if it agrees with replay. |

`turnMarker` starts at Lamb and alternates after each accepted board action. In an ongoing state it is the side to move. In a completed state it is retained only as canonical bookkeeping, including the next-side marker used for a repetition result. The public `sideToMove` query returns **no side** after completion: a terminal marker never grants another turn. This approved representation does not change ending timing.

Values must be deeply immutable: no caller-owned mutable board, list, map, or nested array may remain aliased into an accepted state. Factories copy/freeze inputs, expose no mutable backing storage, and validate before publishing a state. A read-only collection reference alone is insufficient. Old snapshots must remain unchanged after any later action or import.

Phase is derived to avoid contradictory runtime fields. A future saved representation may include phase for inspection, but restoration must verify it against reserve rather than accept inconsistent values. Concrete collection types, serialization, on-disk schema, wire encoding, hashes, and version migration remain later choices.

## 2. Opening, invariants, and validation

`newGame()` creates tigers at A/D/E; all other nodes empty; reserve 15; captured 0; Lamb to move; Placement; Ongoing; zero board actions. Record the initial position once for repetition. A rematch creates another fresh state; roles/difficulty are supplied separately by `app`.

A valid state must satisfy:

1. Exactly 23 known points, one occupant per point, exactly three tigers, and no overlapping/unknown pieces.
2. Reserve in 0–15, captured in 0–5, and `onBoardLambs + reserveLambs + capturedLambs = 15`. A reachable game stops at the fifth capture; counts above five are invalid, not an additional victory.
3. Phase agrees with reserve; placements reduce reserve by one, captures increase captured by one without changing reserve, and ordinary moves change neither count.
4. Position, turn marker, outcome, action count, and repetition data agree with replay under the same ruleset. An Ongoing state cannot already satisfy a win or third occurrence. History cannot continue after completion.
5. A position reported as an ongoing lamb turn has a legal lamb action. Otherwise return the D016 invalid-state error; do not pass or invent an outcome.

Structural validation is not a reachability proof. Production restore/replay starts from the approved opening and validates every recorded event. An arbitrary board that satisfies piece counts is not an admissible saved match merely because its shape looks valid. Synthetic tactical/tutorial fixtures use an explicitly separate fixture entry point with supplied history context; they cannot be imported as production Continue games. Incomplete example occupancies must be expanded into consistent full fixtures before engine testing.

Invalid input is reported with structured domain reasons such as `UnknownRuleset`, `InvalidOccupancy`, `InvalidCounts`, `HistoryMismatch`, `OutcomeMismatch`, or `LambHasNoLegalAction`. Human-readable translations and recovery UI belong to `app`. A declared terminal outcome that contradicts history is an error, not an instruction to repair or overwrite the save.

## 3. Board actions

| Action | Required conditions | Successful change |
| --- | --- | --- |
| `PlaceLamb(to)` | Ongoing lamb turn, reserve > 0, known empty destination | Add one lamb; reserve decreases by one. |
| `Step(from, to)` | Ongoing turn; origin has the acting side's piece; destination empty; one approved edge. A lamb may step only when reserve = 0. | Move that piece along the edge. |
| `Capture(from, over, to)` | Ongoing tiger turn; origin tiger, middle lamb, destination empty; exact approved directed capture triple | Move the tiger; remove one lamb; increment captured by one. |

One action consumes one turn. Ordinary tiger steps remain legal when captures are available. A capture has exactly one middle node and landing; chains are not an action type. Captured lambs never replenish reserve. No `Pass`, `Resign`, timeout, or no-progress action exists.

The engine derives the acting species from the action and position and checks it against the turn. `app` additionally verifies that the submitting human/computer/peer is entitled to act as that side. An engine-approved action is not proof of peer identity or network authorization.

## 4. Queries and transition result

| Operation | Contract |
| --- | --- |
| `validate(candidate)` | Return a validated immutable state or structured invalid-state details. Never normalize a corrupt save into apparently valid play. |
| `legalActions(state)` | Return `Available(actions)` for a valid ongoing state, `Finished(outcome)` for a valid completed state, or `InvalidState(details)`. An invalid lamb turn is not represented as an ordinary empty list. |
| `applyAction(state, action)` | Return `Applied(newState, actionEffect)`, `Rejected(reason)`, or `InvalidState(details)`. Rejection and invalid-state results publish no new accepted state or history entry. |
| `replay(origin, events)` | Validate the origin and every event, then return the final immutable state or the first failing event index/reason. Production callers use the approved opening. |
| `completeAgreedDraw(state)` | Separate completion operation for an app-validated Nearby agreement; never a board action or AI choice. See section 7. |

`Rejected` describes a bad action against an otherwise valid state: occupied/unknown destination, wrong side, wrong phase, missing piece, absent edge, invalid capture, or already-finished game. It leaves all input values and repetition counts unchanged. Invalid-state errors instead tell `app` to block further play and enter D032 recovery. Invalid input takes precedence over treating a request as a routine illegal tap.

`actionEffect` describes the accepted origin/destination, captured point if any, and resulting outcome for UI feedback. It reports no save, delivery, animation, or peer acknowledgement success.

For a given complete state and action, the result is identical. Queries do not modify state, consult clocks, or consume randomness. Return legal actions in a stable order: action kind Place/Step/Capture, then node IDs A–W lexicographically by their action fields. Computer choice may use explicitly supplied randomness outside the rules transition.

Conceptually publish a transition atomically after these checks:

1. Validate the input state and reject actions after completion.
2. Check action legality, then calculate new occupancy and counts without mutating input.
3. Calculate the alternate turn marker and derived phase; append the board action to candidate history.
4. Check wins first, including all-tiger mobility independent of whose turn marker is next. The fifth capture yields Tigers Win; all three tigers trapped yields Lambs Win. If an inconsistent synthetic/imported position asserts contradictory terminal facts, report invalid state rather than inventing a tie-break rule.
5. If no win, count the resulting position once using the alternate side marker; its third occurrence yields Draw — Threefold Repetition.
6. If still ongoing, verify the next lamb turn has an action. A failed invariant is an invalid-state result with no published transition, never an automatic pass.
7. Publish the new state and derived effect only after validation succeeds. An internal failure preserves input and diagnostic evidence; it cannot silently roll back any previously accepted action.

A candidate that fails before publication is not a completed legal action. Once `Applied` is returned, the pure transition is complete, but persistence and Nearby commitment remain separate responsibilities. If either is uncertain, `app` must preserve the latest accepted state/evidence, block play, and follow D021/D032; it must not mislabel unsaved or unconfirmed data as settled.

## 5. Repetition and equality

Within the same ruleset, a repetition key is exact species occupancy at A–W, turn marker, derived phase, reserve count, and captured count. Do not include piece identity, action count, outcome, mode, difficulty, visual selection, or timestamps. Compare full canonical values; hash equality alone is not sufficient.

The approved baseline counts the initial position once and each completed **non-winning** board action's resulting position once. Preserve winning actions in history but do not perform or count a repetition evaluation for them; wins already ended play. A repetition-ending action does contribute its third occurrence. Offers, acceptance, hints, redraw, invalid attempts, refresh, save/load, and resume contribute none.

Two equal repetition keys do not imply equal `GameState`: their histories may differ. AI caches, saves, and future reconciliation must account for that difference. A board-only equality test is insufficient for Continue or Nearby agreement.

## 6. History, replay, and Solo Undo boundary

Production replay begins with the opening counted once. It reconstructs counts and outcomes by applying recorded events, checks any supplied snapshot/cache against the result, and rejects unsupported or inconsistent input. Replaying the log to rebuild the same state is not a new gameplay occurrence. Importing a cached repetition map must never add its counts to a newly replayed map.

History stores actions/events rather than recursively embedding whole previous `GameState` objects. Immutable position checkpoints may be an optimization later; they are not another source of truth.

Undo is not a board action. Under D024, `app` verifies Solo mode, ongoing/unpaused/valid human turn, and the presence of a prior human action. It selects the history prefix immediately before the latest human action, thereby removing that action and the computer reply. The engine can reconstruct that prefix exactly; the restored position is not counted again. Repeat Undo selects earlier human turns; never remove the computer's opening placement alone. `app` saves the retained history and discards the undone future; V1 has no Redo or Nearby takeback.

A history prefix is usable for explicit authorized Solo Undo or analysis, not silent recovery. D032 recovery must establish the latest confirmed state and full retained history. If it cannot, remain blocked; do not pick an earlier valid prefix and call the game recovered.

## 7. Session concerns stay in `app`

Mode, player identity/roles, difficulty, match identity, save version/status, connectivity, readiness, pause reasons, draw-offer state, and UI selection belong to the match/session layer. They are not repetition-key fields and cannot alter engine rules.

`completeAgreedDraw` preserves the position and repetition data, records one agreement-completion event, and changes only an ongoing valid outcome to Draw — By agreement. It rejects completed or invalid states. A repeated call does not append another event. This operation assumes `app` has established the D028 prerequisites: Nearby mode, a valid own-turn offer, explicit opponent acceptance tied to the same match/position, and the required result agreement. It cannot itself prove consent. Raw transport data must never directly invoke it. Pending or uncertain acceptance stays in `app` and cannot be presented as a settled draw; ordering and reconciliation mechanisms remain unresolved.

On replay, structural validity of an agreement event cannot prove that both people consented. Authenticity and commitment of imported history are additional storage/protocol obligations, outside this contract. Solo must never offer or admit this completion operation.

UI and computer submissions must be bound by `app` to the current match and an exact session revision, including history changes. Increment that revision when committed state/history changes, including Undo or replacement, so a late computer result cannot be accepted merely because the same board recurs. The engine evaluates supplied state only; this is not a network sequencing or duplicate-message specification.

## 8. Review scenarios for later engine tests

These are approved acceptance checks for later implementation, **not executed tests**.

| Check | Authority and expected result |
| --- | --- |
| Opening and immutability | A/D/E tigers, reserve 15, Lamb first, one initial occurrence. Applying a legal action cannot change the prior snapshot or caller-owned inputs. |
| Placement and phase | E01/E02/E12: reject occupied placement and early lamb movement; the final placement changes phase and yields a tiger turn unless a win ends play. |
| Board and capture geometry | E03–E11: adjacent steps only; A–C–I capture valid, B–C–I turn invalid, N–T absent; reject occupied landing/jumping tigers/lamb jumps; optional capture and no chain. |
| Outcome boundaries | E13–E16/E20: all three tigers must be trapped; an available capture prevents Lambs Win; fifth capture wins immediately with reserve unchanged; fourth capture alone does not. Win check precedes repetition. |
| Repetition | E17: Z first at origin, second after action 4, third after action 8; terminal draw rejects action 9. E18: species occupancy and the specified state fields determine the key. |
| Rejected and corrupt inputs | Invalid attempts do not change state/history; malformed counts/history/unknown ruleset are explicit errors. E21 no-lamb-action fault blocks play without pass/result. |
| Replay and Undo | Full replay equals the accepted state. Load/resume adds no occurrence. An explicitly selected Undo prefix removes both human action and reply with the corresponding occurrences; stale AI results remain app-blocked even if occupancy repeats. |
| Agreement boundary | E19/D028: offer alone changes no engine state; valid app-admitted acceptance ends an ongoing Nearby game without a position occurrence; no conversion of a completed win; no Solo agreement path. |

## 9. Decisions still needed

D038 approves this representation, API/result distinction, repetition/history model, and app/engine completion boundary. The rationale is deterministic replay and validation, immutable snapshots, and a clear separation between legal game transitions and app-owned persistence or peer agreement. Concrete Kotlin collection/API choices may be finalized during authorized implementation, but must preserve these semantics. Saved-state format/versioning, trusted restoration, AI strategy/evaluation, production dependencies, and Nearby authority/order/recovery/result agreement remain separate architecture steps. Issue #4 remains incomplete.

D039 now collects the [storage/restoration proposal](saved-state-and-restoration.md) and remaining prerequisites into one [final review package](implementation-readiness.md). This approval does not authorize implementation, a production scaffold, or resumed Nearby investigation.
