# Illustrated play screen — local verification, 2026-10-18

Scope: D044, implemented on the current Solo-first checkout. No game-engine, storage, protocol or Nearby changes. No commit, push, merge or release publication was performed for this UI task.

## Implemented

- Compact play header, human/AI portraits, turn instruction and reserve/capture counts.
- Uniformly scaled 23-point board, nearly full portrait width and full available landscape height. Landscape moves navigation/status beside the board. There is no fixed 300 dp play-board height.
- Bundled storybook woodland art, distinct tiger/lamb portraits inside shape-distinguished pieces, clear legal destinations. Modern and Midnight Prowl remain light/dark preferences.
- Controls hidden initially. Reveal by upward swipe over the play area or the explicit Controls/Show controls action. Dismiss with Hide controls, Back, or downward drag on the panel header. Reveal/hide does not play a move or pause the coordinator.
- Hint/Undo, Rules/Settings, saved departure and the accessible point selector remain available in the scrollable panel. Rules and Settings still pause play and require Resume on return.
- The panel remains open during accessible point play. Completed results open it automatically with the existing rematch/home actions; it can be hidden to inspect the final board.
- Large-text status content scrolls within a bounded region so the board remains available. Tamil strings were added as drafts and still require fluent review.

## Automated checks

Reused the README doctor and build commands with Android Studio JBR 25 and local SDK 37. The initial full command included `:game:test`, `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`, and the nearby unit/lint tasks. The final app build, unit, lint and connected suite passed after the final layout fixes.

- 14 pure-game tests: 0 failures/errors.
- 5 app JVM tests: 0 failures/errors.
- 16 API 36 emulator instrumentation tests: 0 failures/errors/skips.
- The nearby module has no production unit cases; its successful task is not radio evidence.
- New tests exercise swipe disclosure without accidental moves, hide/reopen, accessible placement, and Canvas placement at the actual resized B coordinate. Existing complete-games tests now open the panel before using point controls.
- `git diff --check` passed.

Local, ignored logs: `work/ui-design/doctor.log`, `build.log`, `verified-final.log`. Build outputs remain in the checkout-specific Gradle user directory.

## Rendered checks

See [design-qa.md](../../design-qa.md) for the source/implementation comparison and fixed visual findings. Evidence is local under `work/ui-design/`:

- `portrait-final.png`, `portrait-content.png`, `comparison.png`: opening position, light, controls hidden, approximately 390 dp-wide comparison viewport.
- `landscape-final.png`: original 1080 × 2424 device rotated, full-height board and side status.
- `controls-final.png`: primary actions followed by all point buttons at normal text size.
- `dark-final.png`: Midnight Prowl board/labels and decorative art.
- `small-font200-final.png`, `small-controls-font200-final.png`, `small-points-font200-final.png`: 320 dp-wide window at 200% text, status scrolling and accessible point list.
- `swipe-header-before.txt` / `swipe-header-after.txt`: manual downward-header dismissal.

The unsupported-window guard also paused correctly when the comparison viewport became shorter than the approved 320 dp landscape minimum. Restored the original emulator size, font scale 1.0, user rotation 0 and automatic rotation enabled after checks. Latest debug app is installed for review.

## Remaining gates

Emulator UI checks and semantic-button tests do not establish spoken TalkBack quality, physical phone behavior, Tamil translation approval, Nearby connectivity, or release readiness. Those pre-existing gates remain open. The next review step is to inspect the installed build; physical accessibility/device qualification remains separate.

## D045 follow-up

Applied Claude's non-overlapping control layout and narrower landscape rail while preserving the bundled woodland artwork. Removed the persistent play footer. Controls remain hidden by default, disclosed by the header action or upward board swipe. Portrait controls sit below the board; landscape controls sit beside it. Compact windows may shrink the board to preserve visible, scrollable controls. Reduced piece radius prevents the opening D/E tigers from touching. No gameplay or persistence changes.

Build/unit/lint and 16 emulator tests passed (`work/ui-design/claude-layout-build.log`, `claude-layout-tests.log`). The disclosure test now asserts the controls do not overlap the board. Rendered portrait, landscape, revealed states, dark theme and 320 dp / 200% text checks are recorded in the D045 section of `design-qa.md` and `refined-*.png` local evidence. Emulator size and font scale restored; debug build installed. No commit/push/merge/publication in this follow-up.

## Dedicated branch review — 2026-10-18

Reviewed the D044/D045 source, strings, bundled assets and interaction tests on `codex/illustrated-play-ui`, based on merged Solo PR #15 (`3fa2313`). No blocking review findings or additional production changes were required. The user authorized this review, verification, commit/push and PR creation, explicitly stopping before merge.

Fresh verification used the README doctor and full build/unit/lint command, followed by `:app:connectedDebugAndroidTest` on Pixel9a API 36 (`emulator-5554`). Doctor/build/lint passed; JVM reports contain 14 game and 5 app tests with zero failures/errors/skips (unchanged JVM tests were up-to-date). The newly executed Android suite contains 16 tests with zero failures/errors/skips, including resized Canvas hit testing, swipe disclosure without a move, panel/board non-overlap, accessible placement, pause/recovery and complete Solo games. Nearby task success remains no evidence of physical connectivity.

Reinstalled the debug APK after instrumentation cleanup and manually opened Home → Play AI → Start Game → Controls. Fresh `portrait.png` and `controls.png` captures confirm the complete board and separate control area. Also reviewed the existing D045 portrait, landscape-controls, dark and 320 dp/200% text captures. These latter captures were inspected again, not recaptured in this review. No device settings were changed.

Ignored evidence: `work/ui-pr-review/doctor.log`, `build.log`, `instrumentation.log`, UI XML and PNG captures, and `source-manifest.json`. The 30-file source/resource manifest SHA-256 is `480427c4e0bced867e17a4955708d1e569960eb298fe22e14a5f0455f9244c10`. The debug APK is 36,374,275 bytes, SHA-256 `6ee1c5957beca8e5635c1f9339e83a92c04fd34612c51c7a66601dda735d1a8b`. Build outputs and raw device evidence remain outside version control.

Full spoken TalkBack, fluent Tamil review, physical feedback/latency qualification, Nearby and release gates remain open. Remote CI for this branch is separate from these local results. Merge and publication are not included.
