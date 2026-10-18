# Nearby development implementation — 2026-10-18

D047 authorizes local implementation following the D046 diagnostic experiment. The work is uncommitted on `codex/nearby-diagnostic-link`, based on `1cf8f821f47a0b141897b56465c88183822e9920`. Existing harness changes are preserved. No commit, push, PR, merge, issue closure or release was performed.

## Implemented

- `nearby` contains the Google Nearby Connections point-to-point adapter, permission policy, selectable discovery, explicit matching-code acceptance on both endpoints, byte transport and cancellation. It has no game dependency. The SDK candidate is now integrated for development; physical qualification has not been waived.
- Play Friend opens production Compose setup, requests permissions only on Host/Join, shows discovered sessions and the SDK confirmation code, allows independent side selection, blocks conflicts, and requires both Ready confirmations. Existing board rendering and accessible point selection use the unchanged `game` engine. Hint/Undo remain Solo-only.
- `app/friend` owns versioned binary messages, actor/turn/base validation, per-connection random nonces, installation IDs, private 256-bit pair secrets and HMAC-SHA256 saved-opponent challenges. Display names and endpoint IDs never establish saved-peer identity.
- Original-host coordination records Prepare, Prepared, Commit, Committed, Settled and settlement acknowledgement before opening the next turn. Both actors persist their exact move intent; both engines validate candidate histories. Snapshot/results are not presented as settled before durable peer receipts. Pending evidence survives reconnect; recovery never chooses by timestamp or longest history.
- The separate private `nearby.sqlite` database uses atomic journal/slot transactions. It never modifies Solo's `matches.sqlite`. Staged starts retain the previous Nearby slot until commit. Secrets, saves and journals stay in no-backup storage.
- Pause, backgrounding, connection loss and the leave confirmation block input. Resume requires fresh authentication, saved-state comparison and both readiness confirmations. No background radio service, timeout loss or host migration exists.
- Draw offers pause play; explicit accept runs the durable result transaction. Decline/withdraw requires mutual resume. Play Again/Switch Sides proposes a fresh match ID, clears readiness and preserves the previous result until the new start commits.
- Rules/settings navigation pauses the connection; move feedback respects the existing sound/vibration preferences. New English/Tamil strings are supplied; Tamil remains a draft pending fluent review.

## Automated evidence

Command (with the README's local JDK/SDK environment):

```sh
./gradlew :game:test :app:testDebugUnitTest :nearby:testDebugUnitTest :app:assembleDebug :app:lintDebug :nearby:lintDebug --console=plain
```

Passed: **40 JVM unit tests** (game 14, app 22, nearby 4), zero failures/errors/skips; debug assembly; scoped lint. App lint has 0 errors/6 warnings; nearby lint has 0 errors/9 warnings. Warnings concern selected dependency/SDK versions, permission API constants and manifest flags, catalog/style suggestions and unused resources. None was suppressed or baselined for this change. Routine CI remains unit-only. No full emulator regression matrix was run.

New protocol tests exercise:

- Independent side selection, conflicts, readiness invalidation and both actors' legal moves.
- Four complete simulated games with identical canonical boards/histories/outcomes after every settled move.
- Restart at each receipt boundary for starts, moves and agreed draws, including a remote actor whose intent was persisted before sending.
- Duplicate transaction messages without double application; wrong saved secret and stale session rejection; a failed peer Prepare write; delayed Resume invalidated by Pause.
- Draw acceptance/withdrawal and a rematch that preserves its previous result until mutual readiness.
- Chunk retransmission/duplicate handling, strict wire version/trailing-data rejection, corrupted replay rejection and an independently constructed opening digest vector.

These are deterministic app-coordinator tests with crashable in-memory persistence. They do not substitute for Android SQLite failure injection, every possible queue/interruption race, full long-history streaming qualification or real-radio testing.

## Initial rendered/device attempt (before timeout investigation)

Installed the production debug app on the physical Pixel 9a (Android 17/API 37) and API 36 `sdk_gphone64_arm64` emulator. Production Home, Play Friend, runtime permission requests, Host/Join, discovery and timeout/retry screens were exercised. Both permission dialogs were granted; Bluetooth and Wi-Fi enabled flags read 1 on both devices. Screenshots/UI trees and app-scoped logs remain local and ignored.

Phone-host attempts timed out without a discovered peer. With roles reversed, the Pixel listed the emulator session; selecting it timed out before the SDK showed confirmation digits. Another phone-host retry also timed out. Thus **no production connection, live game, on-device durable recovery or result agreement is claimed**. This is consistent with the earlier harness's uneven repeatability, but the cause has not been established. The harness's earlier successful sample exchange is distinct evidence, not a passed production flow.

Initial debug APK SHA-256: `4ad0f9afc226be6d9d7e7c3dceb702ba1ba1ee5746efb2383c867f94abbfe94f` (installed on both devices). This initial build is superseded by the follow-up below. Evidence/logs: ignored `work/nearby-production/` and `work/nearby-link/`; generated reports use `./gradlew printBuildLocations`. Raw device IDs, session codes, secrets, logs and APKs are not versioned.

The untracked root `gradle/gradle-daemon-jvm.properties` is unchanged; SHA-256 `6c13aa5ba36690ccfbbb25a8807d01d98c15833e446d7ae82ddc726472cb8ef7` was checked before and after.

## Limits and next qualification

This is a development implementation, not completed issue #8 or a release-ready Nearby mode. #3/#4/#8 remain open. Required next evidence is a second physical phone and the bounded two-phone discovery/confirmation/match/recovery cases, followed by Android persistence-failure checks.

Specific remaining hardening/verification boundaries:

- Recovery re-drives the exact retained transaction and fails closed on incompatible/uncertain evidence. It does not expose a coordinator Abort workflow for a prepared operation; disconnect preserves it for reconciliation instead of discarding it.
- A staged start that lost contact before both readiness records were persisted may require a fresh pairing rather than Continue. The old committed slot remains retained; no committed game is silently rolled back.
- Framing bounds each SDK payload below 32 KiB and acknowledges each chunk. Logical history/record reassembly currently uses memory with a resource guard, rather than a disk-backed incremental replay pipeline. Oversized recovery blocks and retains evidence; no move-limit draw is invented. This remains a gap against the protocol's incremental long-history requirement.
- Strict stale/contradictory control traffic blocks the session. Exhaustive draw accept/withdraw/disconnect interleavings and old-message receipt replay still need broader fault qualification.
- The focused follow-up below exercised production confirmation, gameplay and recovery on the mixed device pair. TalkBack, large-font/Tamil review and full two-phone match usability remain unverified.

## Codec and dependency notes

`FriendMatch` V1 writes big-endian integers and length-prefixed strict UTF-8/blobs: version, ruleset, match ID, original host ID, guest ID, host side, then a Nearby-state blob. The Nearby-state version is 2 and preserves the existing explicit Solo field layout while adding event tag 4 for admitted draw agreement. Solo bytes/version remain unchanged. `FriendRecord` V1 adds the private pair secret, sequence, started flag, transaction stage/candidate and request evidence map. Packet tags use explicit names, not enum ordinals. Unknown versions/tags and trailing bytes fail closed.

The opening golden vector uses match/host/guest UUIDs ending in `001`, `002`, `003`, host as Lamb and the D017 opening. SHA-256: `77b0cf40d18b29f0faf88aee3acc283829582e51e61d5c9c80ab9e31ddc9fba5`.

Production resolves Nearby 19.5.1/base 18.5.0 on the existing AGP/Kotlin stack; locks and SHA-256 dependency metadata were regenerated and inspected. Permission boundaries reuse the observed Wi-Fi-state correction, keep location capped at API 32, and retain target SDK 36. No Internet/storage permission or analytics SDK was added. SDK internals and eventual privacy disclosure still need release review. References: [SDK confirmation](https://developers.google.com/nearby/connections/android/manage-connections), [SDK permissions](https://developers.google.com/nearby/connections/android/get-started), [harness permission evidence](../../experiments/nearby-harness/PERMISSIONS.md).

## Pairing-timeout investigation and focused live follow-up — 2026-10-18

User authorization: investigate/fix the Pixel 9a/emulator pairing timeout and verify a live match/reconnect, without a full regression run. Existing work and the untracked Gradle file were preserved. No commit, push, merge or issue closure.

### Findings and changes

- In reverse roles, the Pixel discovered the emulator advertisement at virtual address `10.0.2.16:52661`. Google Play services attempted Wi-Fi LAN sockets three times, each failing after 10 seconds. This occurred before SDK confirmation, not in the app handshake. The emulator-host direction remains unsuitable in this setup; no network bridge or substitute transport was added.
- With Pixel hosting, Nearby reported successful advertising and emulator discovery startup, but no endpoint reached the app. The Mac could resolve the phone advertisement. Cycling **only the emulator Wi-Fi while the phone advertised** restored endpoint discovery and a real Nearby SDK connection. Reconnect needed the same workaround. This identifies a repeatable recovery action, not a proven underlying emulator-stack fix.
- The app had a separate timeout defect: one 60-second deadline covered search, socket negotiation and human confirmation. A late peer selection or code prompt inherited the nearly expired search budget. `PairingTimeout` now gives each stage its own 60-second budget, invalidates obsolete callbacks and cancels deadlines on stop/connection success. Four deterministic unit tests cover late transitions, cancellation, retry and correct failure classification.
- Sanitized `NearbyLink` stage/timing logs contain no codes, payloads, peer IDs or network addresses. UI distinguishes searching, connecting, saved-game sync and the three timeout stages. English/Tamil timeout strings were added; Tamil remains draft.

### Live evidence

Pixel host / emulator join, real Nearby SDK transport; both devices unlocked, Wi-Fi and Bluetooth enabled:

1. Compared identical SDK codes and explicitly confirmed both endpoints. Both reached side selection. Selected Pixel Lamb / emulator Tiger and confirmed Ready on both.
2. Played Lamb at B and Tiger A → C. Both private SQLite stores had sequence 3 and identical canonical match digest `fce7d3913fce5506f06c52ba7a3a07333a6c84d4920f46022e76c14e017a27cd`.
3. Backgrounded Pixel: emulator disconnected and paused. Installed the updated debug build on both, retaining app data. Continue Nearby game, discovery workaround, fresh matching-code confirmation and saved-peer authentication recovered that exact digest and sequence. Both stayed paused; Pixel-only Resume left emulator paused with “Your friend is ready.” Both Resume reopened play.
4. On the updated build, played Lamb at G and Tiger C → I. Both stores reached sequence 5 with identical digest `fc7d7487c860e84c5a2648ea3c7ca1d64f0180adfc59d1c6d47b8aa3cb37100e`.
5. Pixel offered a draw, emulator accepted. Both rendered “Draw / By agreement” and persisted sequence 6 with identical digest `13c17a34c6c41eb27f29f33dc6baf479918a04674d5d5b17e59bbf3841f57ecb`.

This is a short live match completed by agreement, not a full capture/trap playthrough. It proves this bounded mixed-device flow with an emulator network workaround, not reliable arbitrary discovery, reverse-role connectivity, two-phone acceptance, offline operation or release readiness. SQLite match digests were extracted read-only in memory; secrets were not printed or versioned. Screenshots/UI trees and raw logs remain ignored under `work/nearby-pairing/` and `work/nearby-link/`.

### Verification and setup

- `:nearby:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug`: passed, **30 tests** (Nearby 8, app 22), zero failures/errors/skips. Game sources were unchanged; the game suite and full emulator regression matrix were not rerun.
- `:app:lintDebug :nearby:lintDebug`: passed; 0 errors, existing 6 app / 9 Nearby warnings.
- Updated APK installed on both: SHA-256 `549684ad0489aacb1376cb801398781788b4b206b0b499a6e0b7627dfd0f915e`.
- Root `gradle/gradle-daemon-jvm.properties`: original SHA-256 rechecked, unchanged.

For this mixed-device development setup, use Pixel **Host** and emulator **Join** (Continue chooses the saved original roles). If the emulator does not discover the advertising phone, cycle the emulator Wi-Fi and retry within the host search window:

```sh
"$ANDROID_HOME/platform-tools/adb" -s emulator-5554 shell svc wifi disable
"$ANDROID_HOME/platform-tools/adb" -s emulator-5554 shell svc wifi enable
```

Confirm matching codes on both, then Resume on both for a saved match. Do not cycle Wi-Fi during gameplay unless deliberately testing a disconnect. The next acceptance setup remains a second physical phone. Android documents the emulator's [virtual address space](https://developer.android.com/studio/run/emulator-networking-address) and [shared emulator network](https://developer.android.com/studio/run/emulator-networking-interconnect); neither replaces two-phone qualification.

## PR review verification — 2026-10-18

D048 authorizes delivery on dedicated branch `codex/nearby-gameplay-review`, based on main `1cf8f821f47a0b141897b56465c88183822e9920`. The earlier sections retain their original local-work status and chronology; this section records the subsequent review/delivery step.

Reviewed the production transport and permission policy, per-stage timeout guards, app protocol/journal/codecs, Compose integration and lifecycle handling, diagnostic harness, dependency locks/checksums, and documentation. The known recovery, long-history and device-qualification gaps above remain explicit; this PR does not close issues #3/#4/#8 or claim complete V1 acceptance.

Re-executed the documented production verification command: **44 JVM tests** (game 14, app 22, Nearby 8), all passing, plus debug assembly and app/Nearby lint. Gradle reused valid up-to-date outputs where sources were unchanged. Also ran the isolated harness debug assembly, unit tests and lint successfully. No source behavior changed during this delivery review, so the final APK and live evidence above still identify the reviewed implementation. No full regression matrix or new radio session was run for PR preparation. Routine PR CI remains unit-only; emulator/build/lint regression stays manually dispatchable.

Raw logs, device identifiers, SDK paths and build outputs remain local/ignored. The root untracked Gradle file retains SHA-256 `6c13aa5ba36690ccfbbb25a8807d01d98c15833e446d7ae82ddc726472cb8ef7` and is excluded from the commit. Merge is not authorized in this step.
