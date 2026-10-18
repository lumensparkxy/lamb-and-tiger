# Illustrated Solo setup

D055 implements the selected child-friendly mockup in native Compose. The screen uses 52 dp difficulty medallions, 40 dp side icons, full-row single-choice selection, selected tint/border and a checkmark. Text wraps; the whole page scrolls when needed. Existing English and draft Tamil difficulty strings are split into separate name/level resources without changing their wording. D056 also uses these separate strings during gameplay; the original combined resources are retained. Decorative images are excluded from semantics; each choice exposes its selected state and radio-button role.

## Artwork

The six `challenge_*.png` resources were generated from the user-approved mockup using the built-in image generator. The 1024 x 1536 source sheet has two columns and three rows; measured 408 px circular medallions were cropped, resized to 256 px and displayed with a circular clip. The smiling lamb asset is reused for side selection. Text is rendered by Compose, never baked into the illustrations. `ic_check.xml` uses the standard Material Design check path (Apache 2.0).

## Verification

- Doctor passed with JDK 25 and SDK 37. `:app:assembleDebug`, `:app:testDebugUnitTest` (25 tests, zero failures) and `:app:lintDebug` passed. Lint reports warnings, no errors; this is not a warning-free or release qualification claim.
- Installed the debug APK on `emulator-5554` (API 36, 1080 x 2424, 420 dpi). Normal portrait shows all five levels and Start Game without scrolling.
- UI-tree-driven taps verified all five difficulty choices and both sides. Exactly one difficulty and one side remain selected. The original Easy default was observed before making selections.
- Start Game opened a Lamb / Sleepy Whiskers match with the approved 15-lamb opening. Saved and returned Home using the existing confirmation flow. This leaves a QA match in the emulator only.
- Visually checked Modern and Midnight Prowl. At 320 dp width and 1.5x font scale, text wraps and scrolling reaches Expert and Start Game without horizontal truncation. Restored the original emulator size/font scale and Modern theme afterward.
- Left the running emulator on the illustrated setup screen. Local screenshots are under `output/setup-design-20261018/` (not shared source); `preview.png` is the final normal-size render.
- `git diff --check` passed. No new dependencies, game-engine changes, physical-phone tests, TalkBack session, commit/push or release publication. Broader device/localization acceptance remains separate.
