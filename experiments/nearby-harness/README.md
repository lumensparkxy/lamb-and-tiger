# Nearby diagnostic scaffold

An isolated issue #3 experiment, not the production game. The current source adds Host/Join, selectable discovered peers, explicit SDK matching-code confirmation on both devices, byte samples with unique IDs and peer ACKs, and Stop/Disconnect. It retains the scrollable local diagnostic screen and Refresh control. It reports Android/device details, Google Play services availability and package versions, reported Bluetooth/Wi-Fi features, and permission status for the current API level.

Host/Join requests missing permissions. Discovery/confirmation has a 60-second observation window; unacknowledged samples remain uncertain after 10 seconds and are never automatically replayed. Backgrounding or locking disconnects; reconnect explicitly and compare new codes on both devices. The harness does not save matches or implement game rules. Diagnostic text is English-only; production English/Tamil acceptance is not claimed.

## Build prerequisites and commands

Use the installed Android SDK with platform 36 and Build Tools 36.0.0. Set `ANDROID_HOME` to that SDK and `JAVA_HOME` to the Android Studio JBR locally. Do not commit these machine-specific paths. The verified host runtime was JBR 25.0.3. SDK auto-download is disabled; Gradle may download Maven dependencies.

From the repository root, with those environment variables set, the verified command is:

```sh
./experiments/nearby-harness/gradlew -p experiments/nearby-harness :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

Reuse this command if CI is introduced later. There is no CI workflow yet.

Build outputs are under the local Gradle user directory, outside cloud-synced Documents. Run `./experiments/nearby-harness/gradlew -p experiments/nearby-harness printBuildLocations` to locate them; the app APK is at `outputs/apk/debug/app-debug.apk` beneath the reported app directory. Historical in-workspace build outputs remain ignored and preserved. The wrapper uses Gradle 9.6.0 with the official distribution checksum; the generated wrapper JAR was also checked against Gradle's published checksum.

## Verified build result — 2026-10-17

- Debug assembly succeeded using AGP 9.4.1, Gradle 9.6.0, Kotlin 2.4.20, JBR 25.0.3, API 36, and Build Tools 36.0.0.
- Four permission-policy unit tests passed with zero failures/errors/skips. These test API boundaries, not Android permission dialogs or connectivity.
- Lint completed with **0 errors and 11 warnings**. Four warnings concern inlined permission names behind an explicit API argument (the caller passes `Build.VERSION.SDK_INT`); two concern flags ignored on old Android versions. Four are version advisories for the deliberately selected target/compile SDK, Gradle, and Play services base. One recommends explicit Android 12+ backup/transfer rules; the scaffold has `allowBackup=false` and implements no persistence. Address backup/transfer policy before adding saved data. No warnings were hidden or baselined.
- APK package metadata confirms the experiment ID, min SDK 29, target/compile SDK 36, and the intended permission caps. `apksigner verify` passed; this is a local debug signature, not release signing.
- APK SHA-256: `16e001c211ec10e7031fe2ed138f9140271fc70d8ec1bf3654b5052577813c67`.
- No APK installation, launch, rendered UI check, Android instrumentation tests, physical-phone tests, or release checks were performed.

The initial build failed because Nearby 19.5.1 contains Kotlin 2.4 metadata while AGP defaults to KGP 2.2.10. The root build file now uses Android's [documented KGP classpath override](https://developer.android.com/build/releases/agp-9-0-0-release-notes#upgrade-to-a-higher-kgp-version) to pin 2.4.20 while retaining built-in Kotlin. A status-reporting API call was also corrected before the successful build. The [Kotlin compatibility table](https://kotlinlang.org/docs/gradle-configure-project.html) lists full AGP support through 9.3.1 for KGP 2.4.20; this newer AGP combination has local build/test evidence, not a broader compatibility guarantee.

## Historical dependency and manifest inspection — 2026-10-17

Direct runtime dependencies are Nearby 19.5.1 and Play services base 18.5.0; JUnit 4.13.2 is test-only. Gradle resolved Kotlin stdlib 2.4.20, Play services basement 18.9.0/tasks 18.2.0, coroutines 1.10.2, and AndroidX transitively. ARCore appears as an optional entry in Nearby's POM but is absent from this resolved debug runtime graph. Dependency resolution completed without a minimum-SDK conflict; device compatibility remains untested.

The merged manifest preserves location/Wi-Fi caps at API 32 and legacy Bluetooth caps at API 30. Nearby contributes `ACCESS_NETWORK_STATE` and a library `WakeUpService` protected by `com.google.android.gms.nearby.exposurenotification.EXPOSURE_CALLBACK`. Play services adds its non-exported `GoogleApiActivity` and version metadata; AndroidX adds its component factory. There are no storage, camera, microphone, background-location, foreground-service, or INTERNET permissions in this APK. This inspection does not establish that SDK internals never communicate through Play services.

See [permission resolution](PERMISSIONS.md) for the API 32/33 correction and later runtime checks.

## Local evidence

Historical build/test/lint reports remain under ignored `app/build/`; current reports use the Gradle-user-directory path above. initial and corrected build logs and the dependency report remain under the repository's ignored `work/` directory. This sanitised summary records the meaningful results without committing device logs, machine paths, signing material, or build outputs. The original scaffold was subsequently merged in PR #15. Identify the current source revision/patch and regenerate the APK hash for each experiment run.

## Emulator smoke check — 2026-10-17

The existing APK was installed and inspected on Pixel9a. The diagnostic screen and Refresh passed the scoped functional checks; a temporary ADB permission grant proved Refresh updates status, and the grant was revoked afterward. Play services reports SUCCESS. No app crash, ANR, or security exception was found in the captured logs. Launches took 12.193 s and 4.885 s with rendering/skipped-frame messages, so startup performance remains unresolved. See the [smoke-check record](SMOKE-TEST.md) for exact scope, artifact identity, and evidence. No connectivity was tested.

## Current scope — D046

The user resumed the isolated experiment on 2026-10-18 and authorized a Pixel 9a plus emulator attempt. Two-physical-phone acceptance remains deferred. Production `nearby` integration, saved-game protocol behavior and issue closure are outside this task. See [current attempt evidence](LINK-TEST.md).

After a successful discovery, select the discovered host, compare the SDK code on both screens, and press **Codes match — Accept** on each device. **Send sample** stays disabled until both accept. Send in both directions and distinguish `SDK accepted send`, `RECEIVED`, and `ACK received`. Reject, Stop, timeout, background or disconnect disables sending. A new session has fresh sample bookkeeping; there is no persistence or uncertain-message replay.
