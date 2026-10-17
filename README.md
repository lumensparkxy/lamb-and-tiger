# Lamb and Tiger Game

An Android strategy game for children, families, and casual adults, using the **23-point Aadu Puli Aatam board** selected on 2026-10-17. See the [approved board and rules specification](docs/product/game-rules.md).

## Project status

Planning foundation only. There is no Android application, Gradle setup, computer opponent, or nearby connectivity implementation yet.

The 23-point board choice is approved. Setup, turns, movement, captures, wins, draws, and invalid-state handling are approved in D011–D016. D017 approves the labelled graph and consolidated specification as the baseline for issue #1; the earlier 19-point Type 2 choice is superseded by D010.

## Product direction

- **Play AI** — subtitle: **Challenge the computer**. Solo mode has [five approved difficulty levels](docs/product/brief.md#approved-ai-difficulty-levels-and-behaviour-d026), with **Baa-sic Instinct — Easy** as the first-game default. Their relative strength remains to be implemented and evaluated.
- **Play Friend** — subtitle: **Play nearby on two phones**. Nearby mode targets local play without app accounts; technical feasibility must be verified on physical devices.
- **Modern** default light theme and **Midnight Prowl** dark theme, with sound/vibration Off by default and Reduced Motion support; [Settings rules and defaults (D025)](docs/product/brief.md#approved-settings-and-defaults-d025).
- A focused home screen with Play AI and Play Friend as the two primary actions; exact labels and subtitles approved in D018. Home navigation, setup, and secondary Settings are approved in [D019](docs/product/brief.md#approved-home-navigation-and-setup-d019).
- Contextual Continue and one independent saved game per mode, with protected replacement and mutually confirmed Nearby resume, are approved in [D020](docs/product/brief.md#approved-continue-and-saved-game-behaviour-d020).
- Leaving an active game and interruptions pause play, with explicit resume and no departure penalties, as approved in [D021](docs/product/brief.md#approved-active-game-departure-and-interruptions-d021).
- [Approved device/language scope (D031)](docs/product/brief.md#approved-device-support-and-languages-d031): Android 10+ phones/tablets; English and Tamil selected from Android system language, with English fallback. Implementation and compatibility verification are pending.
- Pass & Play and internet multiplayer are deferred to V2 or later.

## Planning documents

- [V1 brief](docs/product/brief.md)
- [Screen flows and acceptance criteria — approved issue #2 baseline](docs/product/screen-flows-and-acceptance.md)
- [23-point board and rules — approved baseline](docs/product/game-rules.md)
- [Decisions and proposals](docs/decisions.md)
- [Open questions](docs/open-questions.md)
- [Development instructions](AGENTS.md)

D033 approves the consolidated screen flows and acceptance criteria as the issue #2 baseline, incorporating product decisions D018–D032. The user subsequently authorized publication and GitHub closure; see [issue #2](https://github.com/lumensparkxy/lamb-and-tiger/issues/2) for completion evidence. No implementation or executed acceptance checks are claimed. Work stops before issue #3.

Verified private repository milestones: [M1 — Specification baseline](https://github.com/lumensparkxy/lamb-and-tiger/milestone/1); [M2 — Technical foundation](https://github.com/lumensparkxy/lamb-and-tiger/milestone/2); [M3 — Playable Nearby](https://github.com/lumensparkxy/lamb-and-tiger/milestone/3); [M4 — Playable Solo](https://github.com/lumensparkxy/lamb-and-tiger/milestone/4); [M5 — V1 polish and release readiness](https://github.com/lumensparkxy/lamb-and-tiger/milestone/5).

## Proposed technical direction

Three modules are proposed: `app` for the Android experience and match coordination, `game` for pure Kotlin rules and computer opponents, and `nearby` for local discovery and transport. These modules have not been created. Kotlin, Compose, and Google Nearby Connections remain implementation proposals to validate.

## Development

Build and test commands are not available yet. Add verified commands when the runnable scaffold is introduced. The private [GitHub repository](https://github.com/lumensparkxy/lamb-and-tiger) is configured as `origin`; the initial foundation is published. No CI workflow is tracked yet. The approved specification is tracked in this repository; see [issue #1](https://github.com/lumensparkxy/lamb-and-tiger/issues/1) for publication and completion status.

Setup proceeds one step at a time: perform the current authorized action, report its result, and wait for **NEXT** before starting another setup step. A commit and remote publication are separate steps.
