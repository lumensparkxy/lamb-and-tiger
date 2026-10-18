> D041 authorized implementation. The current verified commands, build output location and remaining evidence gates are in [README](../../README.md) and the [implementation record](../implementation/solo-first-evidence.md). The inventory and selection details below preserve the planning baseline.

# Production build choices and verification plan

Status: routine technical selections under D039, prepared 2026-10-17 for the final package review. Dependency metadata is inspected; a production build has not been attempted. Changes required by real compilation may be resolved within these boundaries and recorded, rather than prompting over routine fixes.

## Identity and build layout

User-confirmed name: **Lamb and Tiger**. User-confirmed production application ID: **`com.maswadkar.lambandtiger`** (D040). Use the same namespace root with `.game` and `.nearby` package subtrees. Debug builds append `.debug` and a visible debug label; no release signing configuration yet. The diagnostic harness keeps its distinct identity.

Root Gradle project contains `:app`, `:game`, `:nearby`. `game` uses Kotlin/JVM and JUnit only at first; app owns UI, coordinator, codecs and persistence; nearby exposes transport contracts and later the proven SDK adapter. Do not move match legality or protocol coordination into nearby. Manual constructor injection is sufficient; no DI framework, backend, account SDK, ads, purchases or custom analytics dependencies are needed for the implementation baseline. The last three exclusions are a development scope choice, not a final business decision.

Use fixed versions in `gradle/libs.versions.toml`, Google Maven and Maven Central, wrapper checksum verification, JVM bytecode target 17, and explicit SDK paths through local environment only. `app` applies Compose compiler; Android modules use AGP built-in Kotlin with the documented KGP override; `game` applies the matching Kotlin JVM plugin. The compiler plugin version matches Kotlin. [Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [AGP built-in Kotlin override](https://developer.android.com/build/releases/agp-9-0-0-release-notes#upgrade-to-a-higher-kgp-version).

| Component | Selected version / setting | Evidence and limit |
| --- | --- | --- |
| AGP / Gradle | 9.4.1 / 9.6.0 | Existing harness build record; AGP POM retrieved live. Official AGP 9.4 requires Gradle 9.6.0 and supports API 37. |
| Kotlin JVM, KGP override, Compose compiler | 2.4.20 | Maven Central POMs retrieved for KGP/compiler. Harness previously needed this version for Nearby 19.5.1 metadata. Production Compose integration unbuilt. |
| Host JDK | Existing Android Studio JBR 25.0.3; CI JDK 25 with vendor/build recorded | Local version rechecked. JVM output remains 17; no new local JDK install needed. CI compatibility must be demonstrated. |
| Android | minSdk 29, compileSdk 37, targetSdk 36, Build Tools 36.0.0 | Preserves D031 minimum and initial target. SDK platform 37 and tools 36.0.0 are installed. Compile SDK 37 is a build requirement, not a higher minimum or a target policy change. |
| Compose BOM | 2026.09.00 | Google Maven POM retrieved: UI/foundation/test 1.12.1 and Material3 1.4.0. UI 1.12.1 AAR metadata requires compileSdk 37 and AGP >=9.1.0. |
| Activity Compose | 1.13.0 | POM and AAR metadata inspected; minCompileSdk 36. |
| Lifecycle ViewModel/Runtime Compose | 2.11.0 | POMs retrieved; runtime Android AAR minCompileSdk 34. |
| Coroutines Android/test | 1.11.0 | Android POM retrieved; test coordinate must resolve during scaffold dependency verification. Keep coroutines out of deterministic rule transitions. |
| Storage | Framework SQLite + explicit app codecs | No Room/KSP dependency; see proposed storage contract. |
| JUnit / Android test runner / test ext JUnit | 4.13.2 / 1.7.0 / 1.3.0 | Harness JUnit already used; runner/ext POMs retrieved. Compose UI tests use BOM versions. |
| Nearby SDK / Play services base | Candidate 19.5.1 / 18.5.0 | Harness evidence only; production adapter blocked on experiment decision. |

The current API/Gradle compatibility is documented in [AGP 9.4 release notes](https://developer.android.com/build/releases/agp-9-4-0-release-notes). Artifact metadata exists at [Google Maven](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom) and [Maven Central](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/2.4.20/kotlin-gradle-plugin-2.4.20.pom). Existence and AAR metadata checks are not complete dependency resolution, a merged-manifest check, or runtime compatibility proof. Verify all transitives and minSdk constraints at the first build. Never suppress a metadata mismatch to force success.

## Local inventory rechecked

ADB currently reports one connected device, `emulator-5554`; only Pixel9a appears in the AVD inventory. No physical phones are connected. Installed platforms include 34, 36, 36.1 and 37; Build Tools include 35.0.0, 36.0.0 and 37.0.0. JBR reports 25.0.3. These read-only checks did not boot/install/change a device or resume Nearby testing. The earlier Pixel9a diagnostic evidence remains in its separate smoke record.

## Implementation verification sequence

The following production commands are **planned and unverified**, since no root scaffold exists. Confirm task names after creating it; document the commands that actually pass in README and reuse exactly those in CI.

```sh
./gradlew :game:test :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :nearby:testDebugUnitTest :nearby:lintDebug --console=plain
./gradlew :app:connectedDebugAndroidTest --console=plain
```

Add a read-only doctor script for JDK/SDK/device/version reporting without installing missing tools. CI runs on pull requests with read-only contents permission, no signing secrets, and pinned reviewed action revisions. Separate JVM/build/lint from emulator jobs. Lock resolved dependencies and record verification metadata after reviewing origins. Do not upload saves, peer secrets, signing material, device IDs or raw device logs as public artifacts. Commit sanitized evidence summaries and retain raw QA under ignored local paths.

| Area | Required evidence |
| --- | --- |
| Engine | D017 E01–E21, D038 checks, all 38 edges/56 captures, immutable input snapshots, complete replay, invalid-state distinction, legal AI outputs. Fixtures must carry valid counts/history context. |
| Coordinator/storage | Crash/failure cases from storage contract; lifecycle and stale AI cancellation; both save slots; terminal/Continue behaviour; repeated Undo; preference isolation. |
| Compose | Rendered Home, setups, board, result, settings, rules/tutorial and blocked/recovery states; taps and alternative point list; exact approved English labels. |
| Protocol | Fake-transport fault schedule from protocol proposal before radio integration. Unit results cannot close #3 or #8. |
| Nearby candidate | Existing issue #3 experiment on two physical phones, both host/join roles, repeated setup and interruption, confirmation, denial, service-unavailable cases and go/no-go. |
| Integrated Nearby | Complete games, all ending paths, draw races, rematches, save/reconnect/process-death scenarios on both phones under #8. |
| Performance | Record build/device/runtime, cold/warm launch, UI frame behaviour and AI latency. Investigate the known harness launch delay if it reproduces; do not call production performance verified from the diagnostic. |

## Required device and layout matrix

| Target | Purpose | Availability |
| --- | --- | --- |
| API 29 emulator/device | Minimum Android; permission flow; storage/lifecycle | Not currently an installed AVD; later provisioning needed. |
| API 30, 31, 32, 33 permission checks | Bluetooth 30/31 and nearby Wi-Fi 32/33 boundaries | Pure policy tests for all; actual denial/grant UI on both sides of each boundary before Nearby acceptance. Targets not provisioned. |
| API 34 | Intermediate Android, lifecycle and UI | Platform installed; AVD not provisioned. |
| API 36 Pixel9a | Initial main rendered/debug target | Existing AVD and current emulator. |
| API 37 | Current-platform compatibility separate from target 36 | Platform installed; AVD not provisioned. |
| Physical phones A and B | Offline radio, services, OEM lifecycle and complete matches | Models/OS/Play services/availability still required from user. |

Exercise 320×480 dp portrait and 480×320 dp landscape usable windows, a 600 dp-wide tablet layout, 100%/200% font scale, both themes, English/ta/ta-IN/ta-LK and an unsupported-language fallback. Test TalkBack completion of a legal game, reduced motion, rotation, resize below the minimum, screen lock, background, and process recreation. Temporary emulator display overrides must be restored. An emulator can cover layout/accessibility behaviour; it does not establish physical-radio or OEM reliability.

Full matrix execution is feature/release verification, not something that can be completed before implementation. Missing emulator images can be provisioned during an authorized implementation run; downloads are not performed in this planning step.

## CI package identifiers verified during implementation

The installed platform metadata and Google's [SDK repository catalog](https://dl.google.com/android/repository/repository2-3.xml) identify the selected platform as `platforms;android-37.0` (Gradle compile SDK 37). The [Google APIs system-image catalog](https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml) lists `system-images;android-37.0;google_apis;x86_64`; there is no corresponding API 37 default image in the AOSP catalog checked. CI therefore uses string API values 29/34/36/37.0, Google APIs targets, and command-line tools build 16111833 from the catalog. The emulator-runner v2 annotated tag was resolved to its commit `a421e43855164a8197daf9d8d40fe71c6996bb0d`; the workflow pins the commit. These are configuration/metadata checks, not a remote CI run.

Linux CI also resolves AAPT2's `linux.jar` classifier. Its SHA-256 was added to dependency verification after downloading the pinned 9.4.1-15978811 artifact from Google Maven and cross-checking its published SHA-1. The local macOS build had resolved only the `osx.jar` classifier. No verification bypass or broadly trusted artifact pattern was added.
