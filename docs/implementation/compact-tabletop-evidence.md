# Compact portrait tabletop

D056 implements the user-approved [Option A mockup without point letters](../../design/tabletop/compact-portrait-approved.png) in native Compose. The generated image is a design reference only; the existing engine remains the source of every board point, edge and legal action.

## Changes

- Preserve the upright 10:8 board geometry and existing tap-coordinate mapping. Hide A–W on Solo only, leaving learning and Nearby labels intact.
- Use compact player/difficulty identity, a pale teal turn banner, inline counts and visible Hint/Undo. Remove duplicated placement instructions and the separate phase line; phase remains in status semantics and the rules reference.
- Keep hints in the same banner footprint. Placement highlights the destination; movement/capture hints show a directional arrow on the board and use instructions that do not depend on letter labels. Internal point IDs, existing rule examples and saved-game format are unchanged.
- Keep result/error dialogs and confirmed-action animations. The banner reserves two text lines, converting each line height separately for Android's nonlinear enlarged-font scaling.
- Keep the existing small-window/large-text scrolling fallback and landscape support. Hint/Undo stack at enlarged text sizes to avoid splitting button words. The selected visual target is portrait, not an orientation lock.
- Reuse existing animal art and localized difficulty title/level resources. Add English and draft Tamil compact labels/hint text; fluent Tamil review remains outstanding. `ic_back.xml` uses the standard Material chevron-left path (Apache 2.0). No new dependency.

## Verification

- Doctor passed with the configured JDK 25 and SDK 37 toolchain.
- README build/unit/lint command passed: 14 game, 25 app and 8 Nearby JVM tests, zero failures. Final app build/unit/lint passed again after the enlarged-text refinements. App lint: zero errors, 27 warnings (including the now-unused combined difficulty strings); this is not a warning-free or release-qualification claim.
- On `emulator-5554` (API 36), SoloFlowTest passed all 12 tests, including board hit testing, complete games as both sides, Hint/Undo, pause and restoration. FeedbackLayoutTest, ResultLayoutTest and BoardAnimationTest passed another seven tests, covering stationary board/control bounds through hints/errors/results and confirmed-action animation behavior.
- Captured and inspected the normal portrait opening, a ringed placement hint, and Midnight Prowl. Compared the rendered structure with the approved reference: same identity/banner/board/count/action hierarchy and original engine geometry, with system bars respected.
- A 320 dp-wide / 200% text check exposed a clipped second banner line. Corrected the per-line height conversion and visually confirmed both lines in `large-text-top-fixed.png`. Hint/Undo now stack at enlarged font sizes; `large-text-bottom-final.png` confirms both complete labels and reachable buttons after scrolling. The normal-size geometry is unchanged.
- Re-ran both FeedbackLayoutTest cases after the banner-height fix: two passed. The final button-stack refinement passed build/unit/lint and the above enlarged-text render check.
- Restored the emulator to 1080 × 2424, 420 dpi, font scale 1.0 and Modern theme. Used Undo on the regression match to restore the opening and left the final portrait game visible (`final-portrait.png`). `git diff --check` passed.

Local screenshots and logs live in ignored `work/compact-tabletop/`. Existing uncommitted setup-screen/artwork work was preserved; `pre-existing.patch` records the tracked baseline before this refinement. No game-engine or protocol changes, physical-phone checks, spoken TalkBack session, commit/push, merge or publication were performed.

## Lamb visibility refinement (D057)

The shared piece renderer now applies a cached slate-blue colour matrix to the lamb only. It retains the original sprite's alpha and detail and uses the same tint for static pieces, action animations and `AnimalIcon`. Tiger pixels and the separate setup/branding artwork are unchanged.

`:app:assembleDebug :app:lintDebug` passed. Installed the debug build on `emulator-5554` and inspected the existing Tiger-side match (one placed lamb, 14 in reserve) in Modern and Midnight Prowl. The lamb is visible at board-piece and header-icon sizes in both themes. Local evidence: `work/lamb-colour/light.png`, `dark.png`, and `build.log`. Restored Modern and the emulator's original free-rotation setting after the portrait screenshots; no game moves or save replacement were needed. Gameplay suites were not rerun for this rendering-only change.
