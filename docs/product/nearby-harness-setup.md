# Nearby diagnostic harness setup

Selected 2026-10-17 for the issue #3 experiment, under the user's instruction to choose the minimal setup and inspect available tools. D034 records the initial selection; D035 records the subsequently authorized diagnostic scaffold, permission resolution, and successful build. A later authorized [Pixel9a smoke check](../../experiments/nearby-harness/SMOKE-TEST.md) verified the screen and Refresh but identified slow launch timings. See the [verified build record](../../experiments/nearby-harness/README.md). Connectivity remains untested.

## Selected setup

| Item | Selection and rationale |
| --- | --- |
| Location | Independent Gradle project at `experiments/nearby-harness/`, containing one Android application module, `app`. Keep the disposable experiment separate from the proposed production modules. |
| Identity | Diagnostic label `Nearby Harness`; experiment-only application ID `com.lumensparkxy.lambandtiger.nearbyharness`. This does not settle the final game's name or application ID. |
| Language and UI | Kotlin with AGP's built-in Kotlin support; one Activity with standard Android Views and string resources. Use simple status text, buttons, a peer list, and a scrollable message view. Compose, navigation frameworks, dependency injection, and persistence are unnecessary for this bounded harness. Production UI technology remains open. |
| Android scope | `minSdk = 29`, `compileSdk = 36`, `targetSdk = 36`, following D031. Java/Kotlin bytecode target 17, independently of the host JDK used to run Gradle. |
| Build | Android Gradle Plugin **9.4.1**, Gradle wrapper **9.6.0**, SDK Build Tools **36.0.0**. Use the existing Android Studio JBR **25.0.3** for the initial build attempt. Pin the wrapper distribution checksum when creating it. |
| Kotlin | Use AGP's built-in support without applying `org.jetbrains.kotlin.android`, with the documented KGP classpath override to **2.4.20**. The first build showed that AGP's default **2.2.10** cannot read Nearby 19.5.1's Kotlin 2.4 metadata. Gradle's embedded Kotlin **2.3.21** is its build-script runtime, not the app compiler. |
| Direct app dependencies | `com.google.android.gms:play-services-nearby:19.5.1` and `com.google.android.gms:play-services-base:18.5.0`. The second explicitly provides the Play services availability check and matches Nearby's declared base dependency. Use `google()` and `mavenCentral()` with fixed versions. JUnit **4.13.2** is test-only for the permission-policy boundary checks. |
| Connection topology | Evaluate `Strategy.P2P_POINT_TO_POINT` on both peers, matching the experiment's single-pair scope. Host advertises, Join discovers, and both explicitly confirm authentication digits. This is an experiment choice, not production transport approval or match authority. |
| Sample transport | Small UTF-8 JSON byte payloads using platform JSON support. Diagnostic IDs and receipt acknowledgements only; no game engine, legal-move enforcement, storage, or production multiplayer protocol. |
| Build/install boundary | Start with a debug APK. No release signing configuration, publishing, accounts, Firebase setup, or production app scaffold in this experiment. Install and execute only in a later authorized step. |

Keep local SDK/JDK paths in local configuration or the execution environment, never shared build files. Use the existing repository exclusions for build output and local evidence.

## Verified local inventory

These are historical observations from the setup-selection step, before the build. They are not app compatibility results. The subsequent build resolved missing dependencies as recorded below.

| Component | Observed availability |
| --- | --- |
| Android Studio | Installed, version 2026.2 |
| Java | Studio JBR 25.0.3 and system OpenJDK 25.0.2; Studio JBR selected for consistency |
| Gradle | Cached distributions 9.6.0, 9.7.0, 9.7.1; 9.6.0 `--version` ran successfully using Studio JBR |
| Android Gradle Plugin | Cached 9.4.1 POM and all its direct dependency JAR entries found; this does not establish complete transitive resolution |
| SDK platforms | API 34, 36, 36.1, and 37.0 directories present; API 36 source metadata reports revision 2 |
| Build Tools | 35.0.0, 36.0.0, and 37.0.0 directories present; selected 36.0.0 and successfully ran its `aapt2 version` |
| SDK manager | Available through Homebrew; `sdkmanager --version` returned 22.0 with a deprecation notice directing future use to Android CLI. It is outside the SDK's usual `cmdline-tools` directory. No package installation was performed. |
| ADB | 1.0.41, platform-tools 37.0.1 |
| Emulator | Pixel9a remains connected as an emulator; Android reports boot completed. Earlier setup verified Android 16/API 36 and Play services 26.34.36; refresh package versions for each test run. |
| Nearby client library | 19.5.1 is listed by Google's setup guide and its POM is reachable from Google Maven. No `play-services-nearby` entry exists in the inspected Gradle module cache. |

AGP 9.4 documents Gradle 9.6.0 and Build Tools 36.0.0 as its minimum/default versions, with JDK 17 minimum. Gradle documents Java 25 support from 9.1.0. These checks support trying the available stack; they do not substitute for a successful Android build.

The initial setup step read POM metadata only. During the subsequently authorized build, dependencies were resolved and downloaded as needed. The debug runtime graph contains AndroidX, Play services base/basement/tasks, Kotlin stdlib, and coroutines. ARCore's optional POM entry did not enter this runtime graph. The [build record](../../experiments/nearby-harness/README.md) documents the inspected merged manifest and APK; device compatibility remains untested.

## Permission and capability implementation checklist

Before enabling Host/Join, check Google Play services availability, relevant device capabilities, and runtime permission results. Request permissions only after Host/Join, show denial/retry/cancel states, and report SDK failures without claiming a connection.

- API 29–30: legacy Bluetooth/Wi-Fi declarations plus coarse/fine foreground location.
- API 31–32: runtime Bluetooth scan/connect/advertise and coarse/fine location; retain legacy Wi-Fi declarations through API 32.
- API 33–36: runtime Bluetooth and nearby Wi-Fi; the harness does not derive physical location and declares `neverForLocation` for scan/nearby Wi-Fi.
- Byte payloads need no storage permission. No background location or foreground-service functionality is planned.
- **Resolved boundary:** platform definitions and Android's Wi-Fi guidance place `NEARBY_WIFI_DEVICES` at API 33. The scaffold follows that boundary and retains location through API 32, correcting the gap in the Nearby example. See [permission policy and sources](../../experiments/nearby-harness/PERMISSIONS.md).

The scaffold lists permission status only and makes no runtime requests. Four unit tests cover the policy boundaries, and APK inspection confirms manifest caps survive merging. Actual permission dialogs, denial/retry, SDK behaviour, and device compatibility remain untested. APIs above the chosen target also require their own verification before any expanded support claim.

## Next bounded step

The diagnostic scaffold, debug build, and Pixel9a functional smoke check are complete. The user deferred further Nearby investigation under D036, including the launch-delay follow-up. Preserve the findings and leave discovery, confirmation, messaging, and lifecycle/reconnect verification pending. Issue #4's [module/UI architecture](../architecture/module-boundaries.md) is approved in D037; its state/action contract is approved in D038. D039 now batches the remaining architecture prerequisites for one final review; the physical-phone experiment remains deferred. NEXT does not automatically resume this experiment.

The verified build/test/lint command is documented in the [harness README](../../experiments/nearby-harness/README.md) and linked from the root README. Emulator installation/execution, physical tests, commit/push, and issue updates remain separate steps.

## Official references checked on 2026-10-17

- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- [AGP built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [AGP 9.4.1 published POM](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/9.4.1/gradle-9.4.1.pom)
- [Play services setup and availability checks](https://developers.google.com/android/guides/setup)
- [Nearby 19.5.1 published POM](https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-nearby/19.5.1/play-services-nearby-19.5.1.pom)
- [Nearby connection strategies](https://developers.google.com/nearby/connections/strategies)
- [Nearby permission example](https://developers.google.com/nearby/connections/android/get-started)
- [Android Wi-Fi permissions by version](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)
