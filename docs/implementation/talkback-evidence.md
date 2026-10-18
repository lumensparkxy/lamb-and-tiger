# Focused TalkBack pass — partial evidence

2026-10-18; baseline `112b4ac`; issues #11/#12. Evidence collected before the review commit.

## Findings and changes

- Opening Solo controls in landscape replaced the status rail, dropping turn/pause status, Hint text and error feedback from the rendered view. The controls panel now retains that feedback; portrait continues to use its existing status area.
- Hint/error text in compact status lacked live-region semantics. Both now use polite live regions.
- The status accessibility description now includes localized reserve/capture counts and the latest move, rather than announcing only the short turn title. This provides capture/move context; actual speech timing and interruption behavior still require listener verification.

## Pixel 9a attempt

- TalkBack 17.0 was installed. Initial enabled accessibility services were null, accessibility_enabled was 0, and the phone was unlocked.
- Enabled TalkBack with the user's readiness confirmation; verified service binding and touch exploration through dumpsys.
- User initially reported clear Home labels and opening How to Play. A later UI snapshot showed the Accessibility Suite notification permission prompt, so this report does not establish successful tutorial traversal.
- The user then reported being blocked and unable to disable accessibility. A system power menu was observed and dismissed. TalkBack was turned off promptly on request; verified enabled services empty, accessibility_enabled=0, and touchExplorationEnabled=false. It was not re-enabled.
- Updated debug APK installed preserving app data. Phone left on Home with normal single-tap interaction. No phone game moves were made in this pass.

## Focused verification

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`: passed; 22 app unit tests.
- Direct instrumentation on emulator-5554: two tests passed:
  - `SoloFlowTest#landscapeControlsRetainSpokenFeedback`: status/count description remains present with controls open; illegal-action and Hint feedback expose polite live regions.
  - `SoloFlowTest#tutorialCaptureExposesSelectionAndAnnouncesSuccess`: selected tiger semantics, capture destination action, lesson success live region and unchanged saved match.
- Rendered emulator landscape review: turn, counts and Hint text visible above controls, board separate from scrolling panel. Rotation restored to portrait with automatic rotation enabled.
- `git diff --check` passed. Local logs and screenshots remain ignored under `work/talkback/` and `work/board-a11y/`.

## Acceptance disposition

**Accepted by user decision on 2026-10-18; no further TalkBack checks required for the current scope.** The user explicitly said, “no more talkback checks required.. make it pass”. This supersedes the earlier decision to defer a future spoken pass. See D049.

Automated verification passed as recorded above. The spoken session was partial: complete tutorial/game traversal, announcement timing, focus retention, both-theme/large-text spoken coverage and Tamil pronunciation were not verified. User acceptance waives those remaining TalkBack checks for this delivery; it is not evidence that the unperformed tests passed or a claim of complete AC47 conformance. Separate non-TalkBack accessibility and release criteria are unchanged.

Keep TalkBack off on the Pixel. No full regression run or Nearby acceptance was performed during this pass. Issues #11/#12 remain open for their other criteria. Two-phone Nearby acceptance remains deferred.
