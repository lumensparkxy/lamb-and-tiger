# V1 product brief

Status: initial draft for review, 2026-10-17. This records the discussion; it is not an approved detailed specification.

## Purpose and audience

Create an approachable Android Lamb and Tiger strategy game for children, families, casual adults, and players seeking more challenge.

## Agreed direction

- Use the **23-point Aadu Puli Aatam board** shown in the user-provided screenshot on 2026-10-17 (D010, superseding D002). See the [annotated transcription](game-rules.md).
- Offer Solo play with computer difficulty levels and playful names.
- Target Nearby play between friends on separate phones without app account creation.
- Start with a clean modern board and allow theme changes through settings.
- Keep Solo and Nearby as the primary home actions.
- Defer Pass & Play and internet multiplayer to V2 or later.

Nearby is a V1 target subject to a successful two-physical-phone experiment. A failed experiment requires an explicit scope decision, not silent removal or an unverified promise.

## Proposed supporting features

These were discussed but still need detailed acceptance criteria:

- A short interactive tutorial and concise rules reference.
- Automatic saving and a contextual Continue action.
- Learning assistance such as hints and undo, with mode-specific behaviour.
- A second dark theme, with additional themes later.
- Accessible piece shapes, readable board lines, large touch targets, and reduced-motion options.

## Proposed release success criteria

- Players can complete legal games with correct outcomes on the selected 23-point board with an approved graph and rules.
- Solo works offline and the difficulty settings show meaningful differences in evaluation.
- Two physical phones can discover, confirm, and complete a Nearby match.
- Disconnect, reconnect, interruption, and saved-state behaviour match the approved specification.
- Theme changes preserve the match state and board readability.
- No app registration is required for Solo or Nearby.

## Not yet decided

Android support range, final theme set, difficulty names, monetization, analytics, languages, application ID, and release distribution. The board and game-rules baseline for issue #1 is approved in D017, incorporating D011–D016; the current GitHub repository is private. See [open questions](../open-questions.md).

## First planning milestone

An approved rules/board specification and rough user flows, followed by a bounded Nearby feasibility experiment. App scaffold and harness checks should support these milestones without assuming unresolved product rules.
