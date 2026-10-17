# Open questions

## Before rules implementation

The [board and consolidated game-rules specification](product/game-rules.md) is approved as the baseline for issue #1 in D017. R01–R10 record the resolved core rules, with detailed approvals in D010–D016. The mapping includes 23 nodes, 38 edges, and 56 directed straight capture paths; reviewed examples include the concrete threefold-repetition sequence.

No core board/rules decision remains open for issue #1. Specification approval is complete; see [issue #1](https://github.com/lumensparkxy/lamb-and-tiger/issues/1) for publication and completion status. Implementation has not started.

## Before committing Nearby to release

- Which two physical Android phones are available for testing?
- Which concrete Android versions/devices will be used for the D031 test matrix, and what Google Play services availability/capability checks are required if Google Nearby Connections is adopted? Android 10+ and the initial API 36 target are approved; dependencies and transport remain to be validated.
- Which confirmation-code and opponent-identification mechanisms will implement the matching-code confirmation and readiness flow approved in D027?
- How will the protocol implement D028 draw-offer delivery, match/position validity, and ordering of acceptance, withdrawal, and disconnection, including recovery when acceptance is uncertain?
- How will both phones agree on a completed result and coordinate rematch proposals, cancellations, and readiness under D022?
- Which device coordinates authoritative match state?
- How are duplicate, late, out-of-order, or incompatible messages handled?
- How will interruption/connection-loss detection, pause notification, and app-termination recovery implement D021, including an unconfirmed in-flight move or save failure?
- How will the Nearby protocol establish the same opponent/match and agreement on game state and repetition history required by D020? How will mismatched saves be recovered while keeping play paused?

Deliverable: bounded two-phone experiment and recorded findings; no production reliability claim from a single happy-path run.

## Home navigation and setup (issue #2)

The [consolidated screen flows and acceptance criteria](product/screen-flows-and-acceptance.md), derived from D018–D032, are approved as the issue #2 baseline in D033. Baseline approval is complete. The user subsequently authorized commit/push and GitHub updates/closure; see [issue #2](https://github.com/lumensparkxy/lamb-and-tiger/issues/2) for completion evidence. This authorization stops before issue #3 and does not start implementation. The technical, translation, evaluation, and release questions below remain unresolved.

The private repository is established. Verified milestone links: [M1 — Specification baseline](https://github.com/lumensparkxy/lamb-and-tiger/milestone/1); [M2 — Technical foundation](https://github.com/lumensparkxy/lamb-and-tiger/milestone/2); [M3 — Playable Nearby](https://github.com/lumensparkxy/lamb-and-tiger/milestone/3); [M4 — Playable Solo](https://github.com/lumensparkxy/lamb-and-tiger/milestone/4); [M5 — V1 polish and release readiness](https://github.com/lumensparkxy/lamb-and-tiger/milestone/5).

D018 approves the exact labels/subtitles: **Play AI** / **Challenge the computer** and **Play Friend** / **Play nearby on two phones**. These retain the Solo/Nearby scope.

D019 approves the six [Home navigation and setup rules](product/brief.md#approved-home-navigation-and-setup-d019), including secondary Settings, initial side selection/agreement, explicit start/readiness, pre-match Back/cancellation, and permission/unavailable-opponent handling.

D027 now settles Nearby opponent confirmation, side selection, and readiness interactions; the underlying protocol remains open. D025 settles Settings behaviour and defaults.

D020 approves the seven [Continue and saved-game rules](product/brief.md#approved-continue-and-saved-game-behaviour-d020), including one save per mode, automatic saving, preserved repetition history, offline AI continuation, mutually confirmed Nearby resume with matching state/history, protected replacement, and unusable-save handling. Detailed synchronization/recovery remains open.

D021 approves the seven [active-game departure and interruption rules](product/brief.md#approved-active-game-departure-and-interruptions-d021): leaving pauses and preserves the match, returning requires explicit resume, uncertain state blocks further play, and departures create no result or penalty. Detailed connection detection and recovery mechanisms remain open.

D022 approves the seven [result-screen and rematch rules](product/brief.md#approved-result-screen-and-rematches-d022), including explicit outcomes/reasons, the final board, Play Again/Switch Sides/Home, mutually confirmed Nearby rematches, fresh-match resets, and result-screen return behaviour. Match-history archives, cross-game scores, and statistics are outside this approval; Nearby result agreement and rematch-message handling remain open.

D023 approves the seven [tutorial and rules-access rules](product/brief.md#approved-tutorial-and-rules-access-d023), including secondary How to Play, offline Quick Rules/Guided Tutorial, optional independent practice with no saved tutorial progress, and paused in-game Rules access with explicit resume. Tutorial wording and examples require verification against the approved rules during implementation; D024 now settles hints and undo.

D024 approves the seven [hints and undo rules](product/brief.md#approved-hints-and-undo-d024): legal highlights in both modes, offline hints and full-turn Undo in Play AI with persisted remaining move/repetition history, availability restrictions, and no strategic hints or takebacks in Play Friend. Explicit Undo does not permit silent rollback during recovery.

D025 approves the seven [Settings rules and defaults](product/brief.md#approved-settings-and-defaults-d025), including paused in-game access, Modern as the default light theme, Midnight Prowl as the dark theme, readable shape/colour distinctions, sound and vibration Off by default, Reduced Motion, and immediate locally persisted preferences. Exact visual designs and sound assets remain open; D030 now approves broader accessibility and screen-layout requirements.

D026 approves the five [AI difficulty levels and six behaviour rules](product/brief.md#approved-ai-difficulty-levels-and-behaviour-d026), including Baa-sic Instinct — Easy as the first-game default, fixed per-match difficulty, equal rules, offline play, and unchanged D024 assistance. Level descriptions are design targets; strength/performance must be evaluated rather than assumed.

D027 approves the seven [Nearby opponent-confirmation and side-selection rules](product/brief.md#approved-nearby-opponent-confirmation-and-side-selection-d027), including session selection, mutual matching-code confirmation, opposite-side choice, readiness reset on changes/disconnection, saved-game protection, and separate resume/rematch flows. Confirmation-code mechanisms, saved-opponent identification, message ordering, and technical match authority remain open.

D028 approves the seven [Nearby draw-offer interaction rules](product/brief.md#approved-nearby-draw-offer-interaction-d028): own-turn offers with explicit send confirmation, one paused pending offer, explicit acceptance, decline/withdraw with mutual readiness, cancellation of unanswered offers on interruption, reconciliation of uncertain acceptance, and match/position validity without new repetition occurrences. Concurrent event ordering and recovery mechanisms remain protocol work.

D029 approves the seven [game-screen interaction and status rules](product/brief.md#approved-game-screen-interaction-and-status-d029), including mode/side/turn information, phase and counts, tap-based placement/movement, input gating, latest-action feedback, and mode-specific controls. D030 now approves adaptive layout, touch-target, text-scaling, and screen-reader requirements; exact rendered designs remain open, while D031 now settles minimum supported window dimensions.

D030 approves the seven [accessibility and screen-layout rules](product/brief.md#approved-accessibility-and-screen-layouts-d030), including adaptive portrait/landscape phone/tablet layouts, safe rotation/resizing, 48 × 48 dp controls and alternative point selection, 200% text scaling, 4.5:1 text contrast, complete TalkBack play, persistent important messages, and release verification coverage. D031 now settles minimum supported window dimensions and Android version scope.

D031 approves [device support and English/Tamil languages](product/brief.md#approved-device-support-and-languages-d031): Android 10+, initial API 36 target with release recheck, minimum content windows of 320 × 480 dp portrait or 480 × 320 dp landscape, conditional Nearby capability checks, system-language selection with English fallback, and extensible language resources without an in-app selector. Concrete test devices, Tamil translations/difficulty names, and technical compatibility validation remain pending.

D032 approves the seven [invalid-game-state recovery rules](product/brief.md#approved-invalid-game-state-recovery-d032): stop play without a pass or result, explain the failure and recovery choices, preserve saves/history, retry without rewriting completed play, require mutual Nearby agreement/readiness, offer explicit Resume or an established completed result, and protect save replacement when starting over. Validation, reconstruction, and Nearby reconciliation mechanisms remain architecture work.

## Before feature completion

- How will validation and reconstruction establish the latest confirmed state and complete move/repetition history under D032, including Nearby state/history/result reconciliation? The recovery experience is approved; technical mechanisms remain to be designed and verified.
- Which AI evaluation fixtures and quantitative strength/response-time thresholds will verify the progression approved in D026? The five levels, names, default, and behaviour are settled; implementation and measured results are pending.
- Exact Modern/Midnight Prowl visual designs, sound assets, and rendered game-screen layout? D025/D029/D030 approve behaviour and accessibility requirements; implementation still needs verification.
- Which concrete devices/window configurations and intermediate Android permission behaviours will be covered by the D031 verification matrix?
- What are the reviewed Tamil translations, terminology, and difficulty names? Verify Tamil wrapping and TalkBack behaviour, regional Tamil selection, and English fallback under D031.

## Before publication

- Final app name and application ID? The existing repository is private under lumensparkxy; any future ownership/visibility change requires a separate decision.
- Monetization and analytics choices?
- Applicable store audience, privacy, and SDK disclosure requirements based on the actual implementation?
- Signing custody, distribution track, and release acceptance criteria?

These questions should be resolved at the relevant milestone; they are not all prerequisites for the initial scaffold.
