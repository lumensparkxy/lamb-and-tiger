# Quiet Tabletop implementation evidence

Date: 2026-10-18. Authority: D050. Local debug implementation; no merge or release publication.

## Implemented

- Native Compose Solo layout with ivory/teal styling, visible Hint/Undo, labelled Menu. D051 subsequently removes the Solo Choose a point footer, point-list panel and swipe disclosure; interaction is directly on the board. Landscape uses a side rail; compact and large-text layouts scroll.
- Approved transparent lamb and tiger head artwork in `drawable-nodpi/animal_pieces.png`, rendered directly from the approved sprite sheet across boards, player identities and point selection. Board geometry and legal actions remain engine-owned.
- Placement: 260 ms fade/settle and quiet ring. Movement: 240 ms eased travel. Capture: 360 ms hop with a gentle lamb fade. Only the next confirmed legal action animates; restoration, undo, history replacement, pause and Reduced Motion settle immediately.
- Board interaction is gated during motion. Computer replies wait 420 ms so the longest animation can settle. Persistence and protocol semantics are unchanged.
- A recording exposed a one-frame destination flash. Preparing the initial animation position before the first draw fixes it; a pixel regression test verifies the tiger remains at its origin on the first capture frame.

## Verification

On the API 36 Pixel 9a emulator (`emulator-5554`):

- Doctor, debug assembly and app/Nearby lint passed.
- 47 JVM tests passed: game 14, app 25, Nearby 8.
- Full connected suite: 20 tests passed before the final animation initialization fix. After that fix, the focused animation suite passed all 3 tests, including the new first-frame pixel regression. The final resource-copy adjustment also passed assembly and app lint.
- Native recordings show a lamb placement, computer tiger movement and a guided legal capture. Reduced Motion, pause, restoration and undo animation behavior have focused instrumented coverage.
- Rendered checks include light/dark play, large text, compact portrait and landscape. The initial point-list check is historical; D051 removes this Solo panel.

Reproduce the standard checks with the commands in README. Focused animation command:

```sh
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.maswadkar.lambandtiger.BoardAnimationTest' \
  --console=plain
```

Ignored local evidence is under `work/tabletop-implementation-20261018/`: `final-checks.log`, `motion-regression.log`, `final-resources.log`, `passing-full-connected-results/`, `passing-motion-results/`, `final-game.png`, and `android-animation-demo.mp4`. Earlier recordings are diagnostic evidence; the combined demo uses the corrected implementation. Instrumentation removes the target APK afterward, so the debug APK was reinstalled for the demonstration and the pre-test saved match restored before interactive play.

## Boundaries

No further spoken TalkBack checks were performed, per D049. Tamil strings remain subject to the existing fluent-language review. The shared Nearby board receives the artwork and motion, but this session makes no physical-connectivity claim. Two-phone acceptance and release qualification remain separate. The next review step is to play the installed emulator build and assess the design and motion in context.

## D051 refinement

Removed the Solo Choose a point footer, its panel and board swipe disclosure. Updated the existing Solo flow tests to use rendered board coordinates. Debug assembly, app lint and all 12 Solo instrumented tests passed, including complete games as both sides, placement/computer reply/Hint/Undo, landscape feedback and recreation. Local evidence: `work/remove-point-list-20261018/checks.log` and `passing-connected-results/`. The debug build was reinstalled with the pre-test save restored.

## Stable feedback and D053 result popup

The initial fix gave Solo hints/errors a reserved, font-scaled scrollable area; D054 below supersedes that inline arrangement. The result now appears in a dismissible dialog after the final move settles, with the existing Play Again, Switch Sides and Home actions. View Board dismisses the popup; View Result reopens it. The completed screen retains the turn/phase and action-area dimensions so the board does not reflow when the result appears. Removed the obsolete inline Solo result/control renderer.

Debug assembly, app lint and all 25 app JVM tests passed. API 36 emulator verification passed both feedback-layout tests, both result-layout tests and complete games as both sides with Switch Sides triggered through the popup. The result-layout tests compare board bounds before/after the approved fifth-capture fixture and verify dismissal/reopening and Android Back. An initial portrait test incorrectly requested scrolling for an already-visible non-scrollable button; after correcting the test, both result-layout tests passed. Rendered portrait/landscape captures are under local `output/result-popup/files/`; these isolated layout fixtures use the default Material theme. The full Solo flow tests exercise the production theme and persistence/rematch behavior. No physical-device or release qualification is implied; new Tamil labels retain the existing fluent-review gate.

## D054 error popups

Solo gameplay errors now use an overlaid, theme-matched dialog with OK, outside-tap and Back dismissal. Long message text can scroll. Dismissal clears only the message, retaining the match and hint, and a repeated invalid tap can reopen the same error. The separate inline feedback area is removed; hints replace the normal instruction inside a stable, font-scaled two-line area. Win/loss/draw handling remains in the D053 result popup. Local rendered error-popup captures are in `output/error-popup/`.

Final debug/test assembly, app lint and 25 app JVM tests passed. Five focused API 36 emulator tests passed: both feedback layouts (including identical board/button bounds while the popup is open, repeated errors, OK/Back dismissal and unchanged match/hint), both result layouts and landscape live-region feedback. The first attempt hit Espresso's root-focus timing when rapidly reopening a dialog; the test now injects the actual Back key through instrumentation. The corrected focused run passed all five tests in 37.6 seconds. The final debug APK remains installed on the emulator.
