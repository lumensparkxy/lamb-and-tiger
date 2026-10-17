# Decisions and proposals

Recorded 2026-10-17 from the project discussion.

## Agreed decisions

| ID | Decision |
| --- | --- |
| D001 | Build a new Android app with the working title Lamb and Tiger Game. |
| D002 | Use the reference board labelled Game type -2. Exact rules remain open. |
| D003 | Design for children, families, casual adults, and broader strategy audiences. |
| D004 | Offer computer difficulty levels with playful names. |
| D005 | Use a clean modern default board with themes selectable in settings. |
| D006 | Target nearby two-phone play without app accounts, subject to feasibility testing. |
| D007 | Defer Pass & Play to V2 or later; omit it from the V1 home screen. |
| D008 | Defer internet multiplayer to V2 or later. |
| D009 | Begin locally under `/Users/admin/Documents/ChatGPT/lamb-and-tiger`; proceed one setup step per NEXT. |

## Proposals awaiting validation or approval

- Three modules: `app`, pure Kotlin `game`, and Android `nearby`.
- Kotlin and Jetpack Compose for Android implementation.
- Google Nearby Connections as the local transport; confirm device support and required permissions in the experiment.
- Modern and Midnight Prowl as the initial two themes.
- Difficulty names: Sleepy Whiskers, Baa-sic Instinct, Sneaky Paws, Woolly Schemer, Grandmaster Growl.
- Interactive tutorial, contextual Continue, and mode-specific hints/undo.
- A private GitHub repository with issues and milestones; no remote exists yet.
- No advertising, purchases, or custom analytics initially; business decisions are still open.

## Decision maintenance

When a proposal is accepted, record the decision and rationale here. Introduce separate architecture decision records when a substantial technical tradeoff is resolved. Preserve superseded decisions with their replacements rather than silently rewriting history.
