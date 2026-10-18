# Focused board usability and accessibility evidence

2026-10-18, issues #7 / #11. Baseline `e74f26b`; evidence collected before the review commit.

## Changes

- Point buttons now expose selection only for the selected piece. Legal destinations retain a contrasting background and their explicit available/capture description without claiming to be selected.
- Board previews use a neutral 23-point board label in English and Tamil, removing directions to nonexistent point buttons on Home and Settings. The live board retains its Controls instruction.
- The focused controls instrumentation test now checks that a legal destination is not selected and has a minimum 48 dp height before activating it.

## Device evidence

- Pixel 9a: installed debug build preserving app data. Reviewed Modern and Midnight Prowl boards with lambs, tigers, last-move and capture markers. Opened Controls, placed at A through the point list, observed reserve decrease from 11 to 10 and computer reply, then Undo restored 11 to place and 2 captured.
- Pixel: 200% font size in portrait and landscape. Board and controls remain separate; controls and point list scroll, including the final W point. Restored original 1.15 font scale, automatic rotation, portrait rotation value and Modern theme. Saved match left paused on Home.
- Pixel9a AVD (Android 16): reviewed Modern portrait and 200% landscape, and Midnight portrait. Restored 1.0 font scale, automatic rotation and Modern theme. No emulator wipe.
- Final APK installed on both targets; UI trees verify the corrected preview label. Screenshots, UI dumps and logs remain ignored under `work/board-a11y/`.

## Focused automated checks

- `:app:assembleDebug :app:testDebugUnitTest`: successful; 22 app unit tests, zero failures.
- Direct AndroidJUnitRunner execution on emulator-5554: `SoloFlowTest#hiddenControlsRevealWithoutChangingMatch` passed (1 test), including accessible point action, minimum height and selection semantics.
- Direct AndroidJUnitRunner execution: `SoloFlowTest#resizedBoardTapUsesRenderedCoordinates` passed (1 test).
- Initial Gradle-connected attempts failed: missing available-point node after the device-setting checks, then startup wait timeout. The same test subsequently passed through direct instrumentation without loosening assertions. These initial failures are retained in local logs; their precise cause was not established. Emulator crash buffer contains an Android Bluetooth service crash during this interval, not an app crash.
- No app crash found in either captured crash buffer. `git diff --check` passed.

Direct focused test invocation (after installing debug and androidTest APKs):

```sh
adb -s emulator-5554 shell am instrument -w \
  -e class 'com.maswadkar.lambandtiger.SoloFlowTest#hiddenControlsRevealWithoutChangingMatch' \
  com.maswadkar.lambandtiger.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Repeat with `#resizedBoardTapUsesRenderedCoordinates` for the board tap check.

## Limits

This is focused visual and accessibility-semantics evidence, not full accessibility acceptance. Spoken TalkBack traversal/announcements, fluent Tamil review, formal contrast measurement and the complete minimum-window/reduced-motion matrix remain unverified in this pass. No full regression run or Nearby radio tests were performed. Two-phone Nearby acceptance remains deferred. Issues #7 and #11 remain open; this evidence does not establish release readiness.
