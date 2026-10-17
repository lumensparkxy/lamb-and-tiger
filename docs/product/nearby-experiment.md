# Minimal Nearby feasibility experiment — issue #3

Status: experiment-check plan, 2026-10-17. D034 selected the harness setup; D035 records the subsequently authorized diagnostic scaffold and successful build. A later [Pixel9a smoke check](../../experiments/nearby-harness/SMOKE-TEST.md) exercised the diagnostic portion of E01; Host/Join and E02–E10 remain unrun. Production transport, architecture, and protocol remain undecided.

Authority: [issue #3](https://github.com/lumensparkxy/lamb-and-tiger/issues/3), [product brief](brief.md), [decisions](../decisions.md), and [open questions](../open-questions.md). D019, D021, D027, and D031 constrain the relevant permissions, interruption, confirmation, and device behaviour.

## Question and scope

Can two physical Android phones discover each other, explicitly confirm the intended peer, exchange small sample messages without app accounts or internet access, and recover a usable connection after interruptions?

Propose a disposable diagnostic harness with:

- Host and Join controls, a recognisable host label, and a selectable discovery list.
- Visible permission/capability status, connection status, errors, and retry/cancel actions.
- Matching-code comparison with explicit confirmation on each device and a rejection action. Evaluate the Nearby-provided authentication digits as the experiment mechanism; do not automatically accept connections.
- A Send Sample control on each device and a received-message view. Samples represent move-shaped data only: receiving one never changes a game board or establishes that a move is legal.
- Diagnostic message IDs and receipt acknowledgements so sent, received, and acknowledged samples can be compared. An API send success alone does not prove receipt; an acknowledgement does not prove a saved or committed game move.
- Disconnect and explicit reconnect controls. Disable sample sending while unconfirmed, paused, or disconnected. Foreground return does not automatically resume sending; require both participants to explicitly resume. After connection loss, repeat peer confirmation.

Google Nearby Connections is the candidate being evaluated. The [harness setup selection](nearby-harness-setup.md) records experiment-only dependencies, strategy, and tool availability under D034; D035 resolves the permission boundary and records the diagnostic build. This harness does not define production match authority, message ordering, persistent opponent identity, saved-match reconciliation, draw agreement, or rematches. Those decisions remain open before multiplayer protocol implementation.

## Evidence available before implementation

The preceding setup step booted the existing Pixel9a AVD and verified Android 16/API 36, Google Play services 26.34.36 installed/enabled/running, and Play Store 53.0.27-34 installed/enabled. This is an environment observation, not a Nearby API or radio test. Recheck versions when running the harness.

Two physical phones have not yet been identified. One emulator, or one emulator plus one phone, cannot satisfy issue #3's physical-phone requirement.

## Proposed checks

E01 is **partially checked**: diagnostic launch, rendered status, Refresh, and no automatic permission prompt were verified on Pixel9a; Host/Join remains unimplemented, and startup performance is unresolved. E02–E10 are **not run**. Counts and observation limits are proposed experiment bounds, not approved product performance targets.

| ID | Check | Expected observation / evidence |
| --- | --- | --- |
| E01 | Launch on Pixel9a; inspect initial controls and diagnostics | No automatic discovery or permission prompt before Host/Join. Record rendered UI, crashes, and available capability checks. Passing does not establish radio support. |
| E02 | Deny required permissions, cancel a request, and retry | Clear blocked status; no false connected state or crash. Cancel returns to idle. Granting permissions allows a new attempt. Repeat for applicable permission groups on each physical OS. |
| E03 | Host on phone A, join on phone B, then reverse roles | Host appears with the expected label; selecting it initiates confirmation. Both phones show matching codes; sample sending remains unavailable until both confirm. Record discovery and connection durations. |
| E04 | Reject confirmation on either phone; cancel discovery or a pending connection | Both devices settle into an understandable recoverable state, with no sample transfer. A fresh attempt succeeds. Exercise the code-mismatch rejection path through a clearly labelled test simulation if a natural mismatch cannot be produced. |
| E05 | Send 10 uniquely identified samples in each direction after confirmation | Compare content, IDs, received events, and acknowledgements on both phones. Record any loss, duplicates, unexpected order, or uncertain delivery; do not silently retry or treat unacknowledged samples as received. |
| E06 | Repeat discovery, confirmation, and E05 without internet access or app registration | Disable mobile data and remove internet-providing Wi-Fi connections while keeping required local radios available. Record the network/radio setup and independently verify lack of internet access on both phones. SDK downloads/install preparation may use internet beforehand. |
| E07 | Background and screen-lock each phone, including during sample transfer | Local send controls pause; record what the peer observes and the detection delay. Returning requires explicit mutual resume. Preserve uncertainty for any in-flight sample. Record gaps needing later protocol work. |
| E08 | Force connection loss, including during transfer, then reconnect | Record the loss method and detection delay. No false delivery claim or automatic replay of uncertain samples. Reconnect explicitly, compare codes again, and exchange fresh samples. Reconnection alone does not prove saved-match recovery. |
| E09 | Terminate/reopen one harness process, then reconnect | No claim that an old connection or game was restored. Explicitly reconnect and confirm before exchanging fresh samples. Persistent game/history recovery is outside this harness. |
| E10 | Repeat complete sessions and failure recovery | Run five full connect/exchange/disconnect cycles with A hosting and five with B hosting. Repeat each denial, rejection/cancellation, background, lock, loss, and process-restart scenario three times per affected phone. Retain all attempts, including failures. |

Use a proposed 60-second observation window for discovery, connection, and reconnect attempts; record timeout and actual elapsed time rather than waiting indefinitely. This is a test cutoff, not a gameplay timeout or forfeiture rule. Report individual durations and counts; this small sample cannot establish production reliability.

Emulator work covers UI, permissions where exercisable, and diagnostic behaviour. Mark unavailable emulator checks blocked or not applicable with a reason. E03–E10 require two physical phones for acceptance. Fault simulations must be labelled separately from real transport events.

## Run record

For each revision tested, record:

- Commit plus any working-tree patch identifier and APK SHA-256; harness build/dependency versions, SDK, JDK, Gradle, and ADB versions.
- Phone aliases A/B, model, Android/API/security patch, Play services version/status, granted/denied permissions, and relevant radio/network settings. Avoid personal device names and serial numbers in shared evidence.
- Check ID, run number, host/join roles, interruption method, expected and actual results, elapsed times, and pass/fail/blocked status.
- Sample IDs and counts sent/received/acknowledged, uncertainty, errors, and evidence references. Keep raw device logs and local paths out of version control; share a sanitised findings summary.

## Decision after the experiment

- **Go to further design:** both physical phones pass the repeated discovery, confirmation, offline exchange, and explicit reconnection checks, with no unexplained delivery claims or unresolved critical failures. Document observed limitations and required production design work. This permits a recommendation, not a release-readiness claim or automatic transport approval.
- **No-go for this candidate as tested:** reproducible failures prevent the required flow. Document failures and options for a separate decision; do not silently remove Nearby from V1.
- **Inconclusive:** phones, evidence, or reproducible conditions are missing, or results remain unexplained. Emulator success alone leaves physical feasibility inconclusive.

## Sources and next step

Official references reviewed on 2026-10-17:

- [Nearby setup and permissions](https://developers.google.com/nearby/connections/android/get-started): required permissions depend on platform/target conditions; denial prevents advertising/discovery. Derive the exact harness permission matrix during implementation planning.
- [Connection management and authentication](https://developers.google.com/nearby/connections/android/manage-connections): both peers accept, and the provided authentication digits support manual code comparison.
- [Connection strategies](https://developers.google.com/nearby/connections/strategies): reference for choosing the experiment's connection topology in the next step.

Setup selection, permission resolution, the diagnostic build, and the subsequent smoke check are linked from [Nearby harness setup](nearby-harness-setup.md). Further Nearby investigation is deferred at the user's request under D036; issue #3 remains incomplete. Issue #4's [module/UI architecture](../architecture/module-boundaries.md) is approved in D037; its state/action contract is approved in D038. D039 now batches the remaining architecture prerequisites for one final review; the physical-phone experiment remains deferred. Execution, implementation, commits, publication, and GitHub updates proceed separately under AGENTS.md.
