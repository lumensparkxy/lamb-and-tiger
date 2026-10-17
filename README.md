# Lamb and Tiger Game

An Android strategy game for children, families, and casual adults, using the **23-point Aadu Puli Aatam board** selected on 2026-10-17. See the [approved board and rules specification](docs/product/game-rules.md).

## Project status

Planning foundation only. There is no Android application, Gradle setup, computer opponent, or nearby connectivity implementation yet.

The 23-point board choice is approved. Setup, turns, movement, captures, wins, draws, and invalid-state handling are approved in D011–D016. D017 approves the labelled graph and consolidated specification as the baseline for issue #1; the earlier 19-point Type 2 choice is superseded by D010.

## Product direction

- **Play Solo:** a computer opponent with multiple difficulty levels and playful names.
- **Play Nearby:** two phones playing locally without app accounts; technical feasibility must be verified on physical devices.
- Clean modern default board, with themes selectable in settings.
- A focused home screen with Solo and Nearby as the two primary actions.
- Pass & Play and internet multiplayer are deferred to V2 or later.

## Planning documents

- [V1 brief](docs/product/brief.md)
- [23-point board and rules — approved baseline](docs/product/game-rules.md)
- [Decisions and proposals](docs/decisions.md)
- [Open questions](docs/open-questions.md)
- [Development instructions](AGENTS.md)

## Proposed technical direction

Three modules are proposed: `app` for the Android experience and match coordination, `game` for pure Kotlin rules and computer opponents, and `nearby` for local discovery and transport. These modules have not been created. Kotlin, Compose, and Google Nearby Connections remain implementation proposals to validate.

## Development

Build and test commands are not available yet. Add verified commands when the runnable scaffold is introduced. The private [GitHub repository](https://github.com/lumensparkxy/lamb-and-tiger) is configured as `origin`; the initial foundation is published. No CI workflow is tracked yet. The approved specification is tracked in this repository; see [issue #1](https://github.com/lumensparkxy/lamb-and-tiger/issues/1) for publication and completion status.

Setup proceeds one step at a time: perform the current authorized action, report its result, and wait for **NEXT** before starting another setup step. A commit and remote publication are separate steps.
