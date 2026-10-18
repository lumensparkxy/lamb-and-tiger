# Illustrated play screen QA — 2026-10-18

final result: passed

## Comparison target and evidence

- Source visual: `/Users/admin/.codex/generated_images/01a14d5b-a7be-7c33-9842-793a58656c7b/exec-aea2476e-5f87-46de-ab03-80cd2acb8dcc.png` (853 × 1844). User subsequently requested less scenery and the largest undistorted board, then authorized native implementation.
- Native implementation: `work/ui-design/portrait-final.png` (1024 × 2421); Android API 36 emulator at 420 dpi. App content crop excludes system bars, y=137..2358, approximately 390 × 846 dp. Both images normalized to 390 × 844 for comparison, with the small height normalization acknowledged.
- Combined full-view evidence: `work/ui-design/comparison.png`. Actual app-content preview: `work/ui-design/portrait-content.png`.
- Same state: light theme, human Lambs, Easy computer, opening position with tigers A/D/E, 15 reserve, zero captured, controls hidden.
- Additional evidence: `landscape-final.png`, `controls-final.png`, `dark-final.png`, `small-font200-final.png`, `small-controls-font200-final.png`, `small-points-font200-final.png` in the same local evidence directory. Full views were sufficient to inspect the header, board, labels, controls and illustration; no separate magnified crop was needed.

## Findings and comparison history

Initial `portrait-first.png` had P2 blank bands separating the artwork from the play area, bulky destination rings and excess status rows. These were fixed with a continuous illustration, smaller destination markers, stronger board lines and a two-row turn/count summary. The debug suffix was removed from the in-game title without changing the debug package identity.

A subsequent landscape pass revealed that the bottom reveal button reduced board height. Navigation and controls now sit in the side column, leaving the entire available height for the board. Extra vertical drawing inset keeps the base labels within bounds. `landscape-final.png` verifies the correction.

The 320 dp / 200% text pass exposed an excessively wrapping title and repeated panel header. The title now occupies one line (may ellipsize at that extreme), the controls label stays stable, and the panel has one explicit Hide controls action. Primary actions precede the scrollable point list. The bounded, scrollable status region preserves board space. The final small-screen captures verify actions and point selection remain reachable.

No actionable P0/P1/P2 findings remain in the checked states.

## Fidelity surfaces

- Typography: native Android sans-serif, clear bold role/turn hierarchy, compact title. English body text wraps at 200%; the nonessential app title may ellipsize on narrow windows. Full approved difficulty names replace abbreviated generated mock text.
- Layout: portrait board is nearly edge-to-edge; landscape uses all available play height with a separate information column. Geometry scales uniformly. A tall portrait necessarily retains space above/below the width-limited board; it is not stretched, cropped, or given invented edges to fill it.
- Color: ivory/forest light palette and dark forest/sage palette. Decorative artwork is subdued in dark mode; labels, shape distinctions and selected/legal-point indicators stay visible.
- Assets: generated storybook forest, tiger and lamb portraits are real bundled PNGs. Functional edges and tokens are rendered from the original board geometry, with circular lambs and triangular tigers. No image-generated geometry is used for gameplay. Trees remain at the perimeter rather than covering central nodes.
- Copy: real app name, approved role/difficulty names and counters; no invented scores/timers. Explicit Back, Controls and Show controls text replaces the mock's small icon-only affordances. Tamil strings are draft additions, not linguistically approved translations.

## Interaction verification

16 Android tests pass, including full games using accessible point controls, reveal/hide without moves, upward swipe without accidental placement, and a direct Canvas tap at the rendered B coordinate. Manual checks cover panel disclosure, scrollable large-text point list, theme change with explicit Resume, portrait/landscape and the minimum-window pause guard. Controls are not a modal pause; Rules/Settings retain coordinator pause semantics. Downward header drag and the Hide controls button dismiss the panel. Reduced-motion behavior is immediate; no decorative animations were introduced.

## Follow-up polish and limits

- P3: tiny tiger-face insets are subordinate to the triangle silhouette at tightly spaced upper nodes; a future icon refinement could improve them without enlarging overlapping targets.
- Spoken TalkBack, fluent Tamil review, physical hardware, Nearby radios and release acceptance remain separate gates. The rendered check is an emulator check, not those qualifications.
- Changes are local; no merge or release publication was performed.

## D045 refinement — Claude layout with existing woodland art

final result: passed

Implemented the approved hybrid layout. The persistent footer is removed; Controls remains in the compact header, with upward swipe disclosure. The landscape status rail is 160 dp (was 200), replaced by a 300 dp control column when revealed. Portrait disclosure collapses the identity row and places controls below a fully visible board. The width-limited board retains its natural size on the normal portrait viewport; short windows share space without overlap. Piece radius is bounded to 0.29 board units to separate the crowded opening tigers. Original PNG artwork and board topology are unchanged.

Inspected `refined-portrait.png`, `refined-controls.png`, `refined-landscape.png`, `refined-landscape-controls.png`, `refined-small.png` (320 dp width, 200% text) and `refined-dark.png` under `work/ui-design/`. Normal portrait and landscape show the entire board in both disclosure states. Large-text controls scroll within their allocated space; the board remains visible, with the existing accessible point buttons available. No additional P0/P1/P2 visual defects found. Remaining P3 artwork detail and physical/TalkBack/Tamil gates above still apply.

Build, lint and 19 JVM tests passed; all 16 API 36 instrumentation tests passed, now including a board/panel non-overlap assertion. Logs: `claude-layout-build.log`, `claude-layout-tests.log`. No claim that a width-limited portrait board grew: its unchanged proportions set the maximum size.
