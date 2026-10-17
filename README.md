# Lamb and Tiger Game

An Android strategy game for children, families, and casual adults, based on the board labelled **Game type -2** in the [Lambs and tigers reference](https://en.wikipedia.org/wiki/Lambs_and_tigers#Gallery).

## Project status

Planning foundation only. There is no Android application, Gradle setup, computer opponent, or nearby connectivity implementation yet.

The precise rules and board topology remain to be specified and approved. The reference's gallery label is a board choice, not a complete rules specification.

## Product direction

- **Play Solo:** a computer opponent with multiple difficulty levels and playful names.
- **Play Nearby:** two phones playing locally without app accounts; technical feasibility must be verified on physical devices.
- Clean modern default board, with themes selectable in settings.
- A focused home screen with Solo and Nearby as the two primary actions.
- Pass & Play and internet multiplayer are deferred to V2 or later.

## Planning documents

- [V1 brief](docs/product/brief.md)
- [Decisions and proposals](docs/decisions.md)
- [Open questions](docs/open-questions.md)
- [Development instructions](AGENTS.md)

## Proposed technical direction

Three modules are proposed: `app` for the Android experience and match coordination, `game` for pure Kotlin rules and computer opponents, and `nearby` for local discovery and transport. These modules have not been created. Kotlin, Compose, and Google Nearby Connections remain implementation proposals to validate.

## Development

Build and test commands are not available yet. Add verified commands when the runnable scaffold is introduced. No GitHub remote or CI has been configured.

Setup proceeds one step at a time: perform the current authorized action, report its result, and wait for **NEXT** before starting another setup step. A commit and remote publication are separate steps.
