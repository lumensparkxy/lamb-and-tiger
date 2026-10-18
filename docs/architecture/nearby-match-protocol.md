# Approved Nearby match protocol contract

Status: approved by the user in D041, 2026-10-17. This is a transport-independent design, not adoption of Google Nearby Connections, a proven distributed protocol, or permission to resume the deferred experiment. D047 now authorizes local production implementation; the two-physical-phone gate still applies. See [implementation evidence and limits](../implementation/nearby-evidence.md). [D038](game-state-and-actions.md) owns legality; [storage](saved-state-and-restoration.md) owns durable evidence.

## Authority and identity

Choose the original host as a fixed match coordinator, regardless of chosen side. Both phones independently validate every board transition using `game`. A coordinator cannot submit a move for the other player or bypass that validation. There is no host migration, election, timeout result, or offline continuation in V1. Loss of the original coordinator blocks the saved Nearby match until it returns. This deliberately prefers preserving agreed play over availability during a partition.

Google Nearby Connections remains the candidate transport behind `nearby`. Use point-to-point topology. For each connection, display the SDK-provided authentication token on both devices and require each person to confirm the match before accepting. Do not replace this token with a random UI code or trust a discovered display name. If the candidate cannot support the approved confirmation flow, return to the transport decision. [Official connection authentication flow](https://developers.google.com/nearby/connections/android/manage-connections).

After an authenticated initial connection, exchange random installation player IDs and establish a random 256-bit per-pair resume secret over that channel. Store it only in private no-backup storage, never logs/discovery names. Bind the match to the two IDs, original coordinator, roles and ruleset. On reconnect require the matching-token flow again and an HMAC-SHA256 challenge/response over protocol version, match ID, both player IDs and both fresh 256-bit nonces, with distinct sender-role tags. Reject reused/nonmatching challenges. A matching name or endpoint ID is insufficient. A lost secret or changed installation cannot silently impersonate the saved opponent; the saved match stays blocked. This is possession continuity, not a user account or a claim of protection against a compromised phone.

## Wire contract and admission

Version 1 accepts only an exact protocol-major and ruleset match. Use the explicit canonical binary encoding described in the storage contract, with separate message tags. Unknown message kinds, inconsistent fields, oversized lengths or unsupported versions cause a paused incompatibility/error state, not best-effort decoding. Transport handles bytes and connection events; decoding and match coordination belong to `app`.

Every post-handshake message carries protocol version, session nonce pair, match ID, sender ID, unique request ID, coordinator sequence, expected base event count and digest, message tag and payload. Validate the authenticated endpoint mapping before decoding gameplay intent. A new connection has new nonces; reject packets from an earlier connection even if match ID is the same. Retries preserve logical request IDs but use the current authenticated session envelope.

| Message family | Payload / admission |
| --- | --- |
| Hello / resume challenge | Version/ruleset support, IDs and nonces; no gameplay before authentication. |
| Setup proposal / role / ready | New match ID, proposed roles, setup generation and replacement confirmations. A changed proposal clears both readiness flags. |
| Action request | One exact D038 action plus expected event count/digest. Sender must own side to move and be playable/ready. Coordinator's local actions use the same path. |
| Prepare / prepared / reject | Transaction ID, next sequence, base and candidate canonical state/history, operation and validated durable receipt. A reject is never an implicit draw or pass. |
| Commit / committed / settled | Coordinator's durable commit decision, peer durable commit receipt, then settlement notification with both commit identities. |
| Draw offer / accept / decline / withdraw | Offer ID, offerer, bound event count/digest, session/control generation. Acceptance explicitly names the active offer. |
| Pause / ready / resume | Reason and control generation, bound to exact settled state/history. Pause blocks locally immediately; Resume requires both readiness confirmations for that generation. |
| Sync summary / history chunk | Last settled and pending sequence, decision/receipt identities, versions and canonical history information. Chunks never authorize moves. |
| Rematch proposal / cancel | Previous settled result, fresh match ID, roles, proposal generation and mutual readiness/replacement agreement. |
| Heartbeat / error | Connection liveness and bounded error code; no game transition. |

Messages are capped at 32 KiB including envelope. History is transferred as bounded chunks with total event count, chunk indices and full-history digest; verify completeness and replay before adoption. Read/replay incrementally so long legal games do not acquire an invented move limit. Backpressure or insufficient storage blocks recovery and preserves evidence; it never declares a draw. SDK delivery success is not a durable application acknowledgement. [Nearby payload APIs](https://developers.google.com/nearby/connections/android/exchange-data).

## Ordering and durable game transactions

The coordinator serializes incoming control and gameplay requests on one queue. One durable gameplay transaction is in flight; event count and transaction sequence are separate because match start and control events do not add repetition. A request ID reused with different contents is a protocol error. Store outcome/receipt by request ID and return it for exact duplicates. Reject stale bases; a missing sequence triggers sync instead of speculative application. Local and remote taps are disabled while settlement is pending.

1. For a legal request against the agreed base, the coordinator stores a pending transaction and candidate, then sends Prepare. No confirmed board or result is published.
2. The peer validates actor, base, full candidate history and outcome independently. It persists Prepared before replying Prepared. It must retain this record through process death; it cannot unilaterally discard it after a timeout.
3. After the matching Prepared receipt, the coordinator transactionally records an irrevocable Commit decision and local committed state, then sends Commit. A lost connection after this point cannot turn the operation into an abort.
4. The peer validates that decision against its Prepared record, transactionally commits exactly once, then returns Committed. Duplicate Commit returns the same receipt.
5. The coordinator durably stores the peer receipt and marks Settled, then sends Settled. The peer persists that notification. A UI may show a settled result only with evidence of both commits; a phone missing settlement remains paused awaiting reconciliation. Simultaneous UI knowledge is not promised.

No new gameplay request is accepted until the coordinator has both commit receipts and the peer has acknowledged settlement delivery. In an uninterrupted playable session this ends the pending-action state and enables the next turn; it does not require tapping Resume after every normal move. Any pause/disconnect invalidates readiness and does require explicit mutual Resume.

Before Commit, cancellation requires a durable coordinator Abort decision sent to the peer and acknowledged; no peer may abort alone. After Commit, cancellation is too late and the exact operation must finish reconciliation. Prepared without a known decision remains blocked until the original coordinator provides its durable decision. On recovery, a coordinator with a pending undecided operation may record Abort only after confirming there is no prior Commit and retaining a durable abort tombstone. Never infer Abort from a missing message or timeout. If durable evidence is corrupt or contradictory, both remain blocked.

For initial match start/rematch, Prepare includes the full opening, roles and both explicit start confirmations; each phone stages the replacement without deleting its current mode slot. Commit atomically promotes the staged match and slot. A split commit blocks play until reconciliation; the committed side never silently restores its old slot. A declined/cancelled proposal before commitment leaves the previous game/result intact.

## Draw, pause and interruption races

An own-turn draw offer is a coordinator-ordered control event, bound to the current settled position and control generation. It pauses play without an engine event. Accept/withdraw/decline compete in that same queue: first valid event received by the coordinator wins; later events receive the recorded result. Device clocks do not break ties. Once acceptance wins, persist the exact acceptance evidence and run the game transaction for `completeAgreedDraw`; no move may race through the paused offer. Withdrawal after accepted commitment cannot undo a result.

On disconnect, unanswered offers are cancelled by a durable coordinator control decision once state can be reconciled. An acceptance that may already be admitted is uncertain, not automatically cancelled: compare durable request/decision records before deciding. A reconnected session never revives an unanswered offer. An acceptance in an old session envelope cannot be newly admitted after reconnection.

Pause locally as soon as lifecycle loss, a leave dialog, recovery fault or lost connection is observed; cancel AI/selection as applicable and send Pause if connected. Proposed liveness setting: heartbeat every 2 seconds while foreground/connected, suspend after 6 seconds without one, with immediate SDK-disconnect handling. These are detection targets to measure, never move clocks or forfeit rules. No background service or continuous background radio session is required. An already committed action remains final; reconcile an in-flight transaction instead of rolling it back.

Ready messages name the settled digest, event count and current control generation. Changing roles/setup, entering another pause, connection loss or an unresolved transaction clears readiness. The coordinator sends Resume only after both are ready for the same generation; a later Pause invalidates any older Resume. A phone also checks its current foreground state before enabling input.

## Reconnect and saved-match agreement

Authenticate the same opponent, establish a fresh connection session, then exchange durable summaries while paused. Compare match/roles/coordinator/ruleset and **full retained event history**, position, repetition and outcome, not only the board/hash. Exchange missing evidence in chunks and replay it. Valid coordinator Commit evidence plus matching prepared candidate permits a behind peer to finish that exact transaction. A remembered duplicate is acknowledged without applying again.

Never select a state by latest timestamp, longer history or host privilege. Divergent committed branches, absent required consent, unknown versions, missing coordinator decisions, a different opponent or unreadable local records block recovery under D032. A valid earlier prefix cannot be presented as recovered play. When an exact ongoing settled state is established on both phones, require both explicit readiness confirmations. When a committed terminal result is established, settle and show it; do not Resume it.

## Required implementation verification

Use a deterministic fake transport and crashable storage to enumerate loss/duplication/reordering at every Prepare/Prepared/Commit/Committed/Settled boundary, both actors and both outcomes. Include a process restart of either/both phones at each durable write, reconnect with a new endpoint, wrong identity/secret/version, changed request payload, stale session, incomplete chunks, corrupted history, draw accept/withdraw/disconnect races, replacement/rematch cancellation, and pause versus delayed Resume. Assert no double action, silent rollback, one-sided playable resume or contradictory settled result.

These tests establish implementation invariants under modeled faults only. Issue #3 must still establish transport feasibility on two physical phones; issue #8 must demonstrate complete matches and recovery with the actual transport. No claim of distributed consensus, simultaneous knowledge, or radio reliability is made by this proposal.
