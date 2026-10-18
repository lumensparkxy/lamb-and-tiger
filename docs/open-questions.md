# Open questions and remaining gates

## Authorized and settled

D041 approves the storage/protocol contracts and Solo-first sequence and authorizes continuous implementation without NEXT pauses. D040 confirms **Lamb and Tiger**, **`com.maswadkar.lambandtiger`**. Current results belong in the [implementation evidence](implementation/solo-first-evidence.md), not in unqualified completion claims.

- D017 settles the [23-point rules](product/game-rules.md): 23 nodes, 38 edges, 56 directed capture paths, opening, legal actions and outcomes. No core rules decision remains open.
- D018–D033 settle the [product flows](product/screen-flows-and-acceptance.md), including exact Home labels, Solo/Nearby scope, fixed five-level difficulty, Continue, pause/recovery, rematches, Hint/Undo, themes, learning, accessibility and English/Tamil. See the [brief](product/brief.md) for the full decisions.
- D037/D038 settle the [three modules and Compose UI](architecture/module-boundaries.md) and [immutable engine/action contract](architecture/game-state-and-actions.md).
- D041 settles [storage/restoration](architecture/saved-state-and-restoration.md) and [Nearby authority, ordering and recovery](architecture/nearby-match-protocol.md). They are approved contracts; physical transport feasibility and live protocol correctness are not established by approval.
- D039 delegates routine technical/visual choices. Pinned builds, original board rendering, SQLite storage, bounded computer policies and their [engineering targets](architecture/implementation-details.md) are selected and implemented for Solo.

The published specification milestones for [issue #1](https://github.com/lumensparkxy/lamb-and-tiger/issues/1) and [issue #2](https://github.com/lumensparkxy/lamb-and-tiger/issues/2) remain separate from implementation acceptance. D042 authorizes revision-bound implementation evidence updates on issues #3–#14; the issues remain open pending their remaining acceptance and integration gates.

## Nearby implementation and remaining acceptance

D046 resumed the isolated diagnostic harness; D047 authorizes production Nearby implementation. The local development implementation and current limits are recorded in [Nearby evidence](implementation/nearby-evidence.md). Two-phone qualification remains deferred. Preserve the diagnostic scaffold, smoke evidence and unresolved launch-delay finding. Issues #3/#4/#8 remain incomplete; a Solo implementation does not satisfy their transport criteria.

- Which two physical Android phones will be the reference pair, and what Android/Google Play services versions and radio capabilities do they have?
- Does the bounded two-phone experiment establish the selected transport, permission behaviour, matching-code confirmation and saved-peer continuity?
- Does the adapter implement the approved original-host authority, durable message ordering, duplicate/stale rejection, explicit mutual readiness and saved-state agreement?
- Do fault tests and real-phone interruptions establish pause/reconnect, in-flight move recovery, uncertain draw acceptance, completed-result agreement and rematches without silent rollback or automatic penalties?

These are implementation/verification questions under the approved protocol, not requests to reopen settled architecture. No simulator or fake-transport test substitutes for physical radio evidence. Return to this lane only when the user resumes it.

## Remaining Solo/V1 qualification

- Host head-to-head progression met the [AI targets](architecture/implementation-details.md); complete the device qualification. The measured results do not establish perfect play or physical-device strength/latency.
- Nominate a physical AI/Hint latency reference. Pixel9a emulator timing is separate evidence.
- Review the broader API 29/34/37 runtime results in [PR #15](https://github.com/lumensparkxy/lamb-and-tiger/pull/15); the local rendered evidence is API 36. Remote emulator checks do not replace the full rendered/language/accessibility or physical-device matrix.
- Complete a real TalkBack navigation/pronunciation pass, including both themes, large text, captures, errors and completed games. Semantic-button automation is useful but does not prove the spoken experience.
- Obtain fluent review of draft Tamil terminology, difficulty names, instructions and pronunciation. Layout checks do not approve translation quality.
- Validate sound/haptic behaviour on physical hardware. The UI respects availability and platform controls; emulator settings tests are not a physical feedback check.

## Before publication

- Store account/ownership, signing custody, distribution track and release acceptance criteria?
- Monetization and analytics choices? No ads, purchases, custom analytics or internet permission are included in this Solo build.
- Actual audience, privacy and SDK disclosures for the eventual release, with a current target-SDK recheck?
- Explicit authorization for merging and any release publication? D042 covers this Solo-first branch commit/push and issue evidence updates.

The repository remains private under lumensparkxy. No ownership/visibility change, release signing or publication is part of D041. These gates do not prevent the authorized local Solo implementation.
