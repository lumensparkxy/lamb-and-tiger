# Project instructions

## Current stage and scope

Production Solo-first implementation is authorized under D041; an isolated diagnostic scaffold exists under `experiments/nearby-harness/`. Read `docs/product/brief.md`, `docs/decisions.md`, and `docs/open-questions.md` before making product or architecture changes.

D041 approves the storage/protocol contracts and authorizes continuous Solo-first implementation without per-step NEXT pauses. Follow the approved implementation sequence and verify each stage. Nearby testing and live transport integration remain deferred. D042 authorizes review, commit/push and relevant issue evidence updates for the Solo-first branch. Preserve existing work; merging and release publication remain separate scope boundaries.

## Product authority

- Preserve the distinction between agreed decisions, proposals, and unresolved questions.
- Use the 23-point Aadu Puli Aatam board selected in D010. Do not invent rules, board edges, starting positions, capture thresholds, or draw conditions.
- V1 targets Solo and Nearby. Pass & Play and internet multiplayer are deferred.
- Theme choices must preserve gameplay and board readability.
- Do not turn proposed difficulty names or architecture into approved requirements without recording the decision.

## Approved implementation boundaries

The user approved the three-module architecture and Kotlin + Jetpack Compose production UI in D037; see `docs/architecture/module-boundaries.md`. The immutable game-state/action contract is approved in D038; preserve its semantics in `docs/architecture/game-state-and-actions.md` during later authorized implementation.

- `game`: deterministic rules and state transitions, with no Android, UI, storage, or connectivity dependency; computer strategy uses legal moves from this engine.
- `nearby`: discovery, connections, and transport; it must not independently define legal game moves.
- `app`: screens, match coordination, persistence, preferences, and theme rendering.

Dependencies run from `app` to `game` and, when integration is authorized, from `app` to `nearby`. Neither lower module depends on `app`, and `game` and `nearby` do not depend on each other. Keep the diagnostic Android Views harness separate. Architecture approval does not itself authorize implementation.

Resolve match authority, message ordering, reconnect, and saved-match agreement before implementing the multiplayer protocol.

## Verification and reporting

Production doctor/build/unit/lint and emulator commands are documented in README; reuse them in CI. Build outputs are under the local Gradle user directory; `./gradlew printBuildLocations` reports them. Experiment commands remain in `experiments/nearby-harness/README.md`. Keep JVM, rendered emulator and physical Nearby evidence distinct.

Test game behaviour against approved rule examples. Check rendered UI for screen changes. Nearby acceptance requires two physical phones; emulator or unit-test success alone is not proof of radio connectivity.

Report what changed, what was checked, what remains unresolved, and the next setup step. Keep implementation, automated tests, emulator evidence, physical-device evidence, and release readiness distinct.

## Repository hygiene

Preserve unrelated changes. Keep credentials, signing keys, local SDK paths, generated build outputs, and device logs out of version control. Commit only deliberate shared project configuration. Do not duplicate global Codex credentials or session state into `.codex/`.
