# Pixel 9a / emulator link attempt — 2026-10-18

## Scope and source identity

D046 authorizes this isolated diagnostic experiment. It does not implement production game transport or satisfy issue #3 two-physical-phone acceptance. No full regression run was performed.

Source: local branch `codex/nearby-diagnostic-link`, based on `1cf8f821f47a0b141897b56465c88183822e9920`, with the uncommitted harness/docs changes described here. No commit, push or issue closure was performed.

- Physical Pixel 9a: Android 17/API 37, Google Play services 26.40.32.
- Emulator: `sdk_gphone64_arm64`, Android 16/API 36, Google Play services 26.34.36; Android Emulator 37.2.12.
- Harness: `com.lumensparkxy.lambandtiger.nearbyharness`, min 29 / target 36; Nearby 19.5.1, base 18.5.0, point-to-point strategy.
- Exchange APK SHA-256: `7176483c1e65ee55dec39225aa5f98dd5fa6252081f14485fa9f1864f0e7a32e`.
- Final APK SHA-256: `e89bc9a1fe7e216dfe5821fd9cd7c6e5540c3c8affe962c8b4e9923c20900390`. Only the Play services diagnostic sentence changed after the first exchange: availability alone does not prove connectivity. Installed and launched on both targets.

## Observations

| Check | Result |
| --- | --- |
| Phone Host, emulator Join | Advertising and discovery started; emulator found phone 481 ms after Join start. |
| Explicit confirmation | Same SDK digits on both screens. Accept/Send were disabled before connection initiation; Send remained disabled after phone-only acceptance. Both accepted explicitly, then both reported connected. |
| Phone sample to emulator | Receiver logged the matching sample ID; phone received its ACK 83 ms after send. |
| Emulator sample to phone | Receiver logged the matching sample ID; emulator received its ACK 184 ms after send. |
| Background phone | Home navigation stopped the phone session; emulator reported disconnected. Both disabled sending. Returning to the phone did not automatically reconnect. |
| Final APK retry | Advertising/discovery started; no peer was observed and the emulator reached its 60-second timeout. During the observation the phone was found in system Settings, so this retry is inconclusive. The emulator was explicitly stopped; the harness stops on phone backgrounding. |
| Reverse roles | Emulator advertised; phone found it 1,760 ms after Join start. Selecting it remained at requesting connection, with no code shown. Both ended at the 60-second timeout with sending disabled. Cause is unresolved. |

The timings are single local observations, not benchmarks or guarantees. Session clocks are independent; only send-to-ACK deltas on the same device are used. Connection latency includes manual confirmation time. The underlying radio path was not instrumented; the observed pair must not be described as verified Bluetooth/Wi-Fi Direct transport or two-phone acceptance.

## Implementation and fixes

The harness adds Host/Join, selectable endpoints, SDK matching-code confirmation, Reject, Stop/Disconnect, strict versioned sample/ACK bytes, unique IDs, duplicate/unknown ACK handling, a ten-pending-sample limit, a bounded visible event log, and session callback guards. No samples are sent before connection success and local acceptance. Unacknowledged delivery is uncertain and never replayed. Backgrounding disconnects; reconnect requires fresh confirmation. There is no persisted peer identity, match, history, mutual-ready protocol or automatic recovery.

The initial phone Host attempt failed with SDK status 8032 after runtime grants. Removing obsolete API-32 caps from normal Wi-Fi state/change-state permissions fixed that start failure; see [permission evidence](PERMISSIONS.md). No Internet or additional location permission was introduced.

A later rebuild encountered duplicate generated `LinkHarness$start$task$1 2.dex` beside the original in the cloud-synced workspace output. Outputs now use the Gradle user directory, matching the production build approach. Previous ignored outputs were preserved. `printBuildLocations` reports the new location.

## Verification and evidence

The [README build command](README.md#build-prerequisites-and-commands) passed debug assembly, all **8 unit tests** (0 failures/errors/skips), and lint (**0 errors, 22 warnings**). Warnings: 11 diagnostic SetTextI18n, 4 InlinedApi, 2 UnusedAttribute, 2 dependency advisories, and one each target SDK, AGP version and backup-rules advisory. None was suppressed or baselined for this change. Unit tests cover permission boundaries through API 37, wire validation and sample bookkeeping; they do not prove Android callback ordering or radio reliability.

Rendered screens on both targets were inspected, including enabled/disabled controls and event logs. Raw UI trees, screenshots and build logs remain in ignored `work/nearby-link/`; reports are under the Gradle user build directory. No device serials, pairing codes, logs, APKs or signing material are committed.

The unrelated untracked root `gradle/gradle-daemon-jvm.properties` was preserved byte-for-byte (SHA-256 `6c13aa5ba36690ccfbbb25a8807d01d98c15833e446d7ae82ddc726472cb8ef7`).

## Remaining qualification

This proves one successful phone/emulator session with bidirectional acknowledged samples. It does not establish repeatability, reverse-role connection reliability, denial/rejection behavior, screen-lock recovery, process-death recovery, offline operation, production gameplay, or release readiness. Emulator cold startup remained slow (12.421 s and 7.046 s observed); no performance claim is made.

The next acceptance setup remains a second physical Android phone. Run the [planned experiment cases](../../docs/product/nearby-experiment.md), including both host roles, rejection/permissions, repeated connections and interruptions, before a two-phone go/no-go decision or production integration.
