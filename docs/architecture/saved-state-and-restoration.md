# Saved-state, versioning, and restoration contract

Status: approved by the user in D041, 2026-10-17. D041 authorizes Solo-first implementation. Implements the behaviour already approved in D020–D024/D028/D032 and the [D038 domain contract](game-state-and-actions.md). The Solo implementation and failure-injection checks are tracked in the [implementation evidence](../implementation/solo-first-evidence.md); [codec V1](storage-codec-v1.md) records the implemented byte format.

## Choice and boundary

Use an app-owned repository backed by Android framework SQLite (`SQLiteOpenHelper`), with all writes serialized through one coordinator. Store relational metadata plus explicitly versioned event payloads. Use transactions for updates spanning events, snapshots, slot ownership, and protocol records. This avoids a Room/KSP compiler dependency for two active save slots; Room remains an alternative if storage grows, without changing this contract. Preferences are a separate table and never participate in game replay.

SQLite supplies transaction APIs; it does not prove our crash-recovery design. Instrumented failure tests remain required. [Android SQLiteDatabase reference](https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase).

`game` receives domain values only. The database, JSON encoding, Android lifecycle, and transport receipts remain in `app`. No filesystem paths or database objects enter the engine.

## Versioned records

| Record | Required fields and constraints |
| --- | --- |
| Database | Schema version 1; explicit migrations only; foreign keys enabled; no destructive fallback or downgrade reset. |
| Mode slot | Exactly one current match reference per Solo/Nearby mode. A replacement changes only that mode's reference, atomically after required confirmation/start agreement. |
| Match | UUID match ID; payload format 1; ruleset `aadu-puli-23-d017-v1`; mode; creation metadata; local side; Solo difficulty stable ID or Nearby player IDs/coordinator ID; lifecycle status; monotonic local revision. |
| Domain events | Contiguous zero-based event index, explicit event tag and node fields; only D038 board actions or an admitted agreed-draw event. No enum ordinals or serialized Kotlin object graphs. |
| Snapshot | All 23 occupancies, turn marker, reserve/captured counts, outcome, event count, derived repetition map, and canonical history digest. A cache checked against replay, never the source of truth. |
| Pending operation | Unique operation ID; expected match/revision/digest; actor; exact action or Undo prefix/replacement request; status and validation evidence. At most one gameplay mutation per match in flight. |
| Nearby journal | Session-independent transaction ID/sequence, proposal and decision, candidate digest, prepare/commit receipts and recovery status. See the [protocol](nearby-match-protocol.md). Never infer consent from a board snapshot. |
| Preferences | Theme, sound, vibration, reduced-motion setting, most recently started Solo difficulty. Unknown preference values use approved defaults with diagnostics; this never repairs a game save. |

Difficulty IDs are `VERY_EASY`, `EASY`, `MEDIUM`, `HARD`, `EXPERT`; labels stay exactly as D026. Version identity is independent of app versionCode. Timestamps are descriptive and never resolve conflicts or select the newest authoritative match.

Canonical digest input uses a documented binary codec version 1: explicit byte tags; unsigned big-endian length prefixes/counts; UTF-8 identifiers; fixed A–W occupancy order; full opening identity and ordered events; turn/count/outcome fields. Reject unknown tags, duplicate fields, invalid UTF-8, negative/overflowing counts, and trailing data. SHA-256 covers these canonical bytes. Compare full canonical values during final reconciliation; a hash is neither proof of consent nor sufficient history equality. Kotlin-specific serialization is excluded. Golden byte/digest vectors must be checked in with codec implementation before peers use it.

## Solo acceptance and saving

1. The coordinator verifies eligibility and exact current revision. Illegal taps never create accepted events.
2. Validate the proposed action, then durably record its exact intent against that revision before publishing any changed board. If this first write fails, reject admission, preserve input, and report that saving failed.
3. Compute the immutable candidate through `game`; persist the event, validated snapshot, revision increment, and completed intent in one transaction. Publish the confirmed board and allow the next action only after commit succeeds. The engine's `Applied` remains distinct from this persistence receipt.
4. Failure after candidate computation blocks play and retains the intent/candidate. Do not present the old snapshot as the latest confirmed position or claim successful saving. Retry reconciles the exact persisted intent, never invents a new action.
5. On process restart, an unresolved intent leads to recovery. If its actor, base revision and exact action validate, reconstruct and finish that same operation idempotently, then offer explicit Resume. If already committed, return the stored receipt without a second event. Inconsistent evidence stays blocked.

For a user-confirmed Undo, persist the selected retained history prefix and new local revision atomically; rebuild repetition from that prefix without adding an occurrence. Remove the abandoned future from active gameplay history. Pending-operation evidence can be retained until completion but cannot expose Redo. A late AI result is rejected by match ID, revision and task generation even if the board repeats.

New-game replacement confirmation is tied to the current slot revision. Cancelled setup preserves the old slot. Initial state and slot replacement are one transaction; a failed start leaves the old save intact. Nearby adds both peers' durable start decision before replacing slots. A completed match retains its final snapshot for an existing result screen but is not playable Continue or a match archive.

## Restoration and lifecycle

Read schema/payload/ruleset first; validate slot, roles and mode; replay all retained events from the D017 opening; compare snapshot/counts/outcome/repetition with replay; reconcile pending records. Never pick the longest log, latest timestamp, or a valid earlier prefix merely to make recovery succeed. Retain corrupt input for diagnosis and show D032 recovery choices. Do not auto-delete a database after a read/migration failure.

Configuration changes retain the coordinator via ViewModel. Small selection/navigation identifiers may use SavedStateHandle; match history belongs in durable storage. A fresh app launch opens Home. Backgrounding cancels AI work and pauses the coordinator immediately; saving happens per accepted action rather than depending on `onStop` or process-exit callbacks. Returning to an existing ongoing game requires Resume. Rotation alone preserves selection and pause status. [Android state-saving guidance](https://developer.android.com/topic/libraries/architecture/saving-states).

Persist an interrupted/paused marker when possible. An inability to save that marker cannot restart play: all freshly restored ongoing matches begin paused. A durable completed result can be shown on restoration of the existing result screen. An unconfirmed Nearby result enters reconciliation, not playable Continue or a fabricated settled result.

Exclude saves, identity secrets and pending journals from cloud backup and device transfer: use app-private no-backup storage plus explicit pre-31 backup and 31+ extraction exclusions, including database sidecars. Preferences may also be excluded for V1 simplicity. Uninstall/data clearing can destroy local matches; no cloud recovery is promised. Android backup rules differ across versions and transfer paths, so validate the merged configuration and device behaviour. [Android backup guidance](https://developer.android.com/identity/data/autobackup).

Unknown future schema/ruleset versions are read-only blocked states. A migration must preserve the original evidence, execute transactionally, replay-validate its result, and keep failure recoverable. V1 has no historical production format to migrate; do not invent a migration from the disposable diagnostic app.

## Required implementation checks

Test initial save, every action, terminal save, both Solo roles and all difficulties; kill the process before intent, after intent, during candidate write, and after commit before UI publication; inject storage errors; repeat each retry; restore during AI turn; reject stale AI after Undo/new match; verify both slots survive independent replacement/cancellation. Test corrupt counts, unknown versions, truncated/contradictory events, cache mismatch, terminal history extension, migration rollback and backup exclusions. Run storage tests on Android, not only a desktop fake. These are planned checks, not evidence of completion.
