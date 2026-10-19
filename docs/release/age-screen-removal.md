# Age-screen removal verification — 2026-10-19

D065 removes the age-group prompt, Change player control and local age-group storage. The app opens directly to Home; startup deletes the previous `audience` preference without clearing game saves or other settings. CHILD/G/non-personalized advertising and UMP under-age treatment remain unchanged for every player.

## Candidate and checks

Built in an isolated checkout of `8aa395f` plus the scoped D065 patch. Staged app sources were compared byte-for-byte with that tested checkout. Pre-existing Nearby layout/recovery and accessibility changes were excluded and preserved in the original workspace.

- Debug assembly, release Kotlin compilation, debug/release lint and 32 app JVM tests passed. Release lint: 0 errors, 31 warnings.
- Focused emulator startup/navigation test: 1 passed; tutorial/save isolation and entry to Play Friend remain functional without selecting an age group.
- Focused Google test-ad SDK test: 1 passed; a test interstitial loaded, displayed and continued exactly once after dismissal. This does not qualify every live creative.
- Emulator Home was visually inspected with Play AI, Play Friend, How to Play and Settings visible, without an age prompt.

The same ad-disabled debug APK was installed with `adb install -r` on Pixel 9a and `emulator-5554`. SHA-256 of the installed `base.apk` matched the local candidate on both:

```
885dc545c0b68eb0466dd0aee99cd3acdd179b64ba914748928b57f64aef947f
```

Package: `com.maswadkar.lambandtiger.debug`; version code 1 / `0.1-solo`. The Pixel's original installation timestamp was retained; no uninstall or data-clear command was used on the phone. The old `audience.xml` preference was absent after launch on both devices. After the owner unlocked the Pixel, its Home screen was visually verified with Continue, Play AI, Play Friend, How to Play and Settings visible and no age prompt. Continue remains available for the existing saved game. MainActivity was confirmed as the resumed activity. This completes the pending physical-device visual check; it does not verify Bluetooth connectivity.

## Release boundaries

No new signed release bundle, Play upload or submission was performed. Existing signed bundles predate D065. The mixed-audience age-screening issue remains recorded in [submission status](submission-status.md). Actual CDM/Bluetooth connectivity and two-phone acceptance remain unverified by this pass.

Local build logs, test results, screenshots, installation checks and the exact scoped patch are retained under ignored `artifacts/local/remove-age-20261019/`. They are not release artifacts.
