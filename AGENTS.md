# Project instructions

## Current stage and scope

This repository is at the planning stage. Read `docs/product/brief.md`, `docs/decisions.md`, and `docs/open-questions.md` before making product or architecture changes.

The user has requested step-by-step setup. Complete only the currently authorized step, then stop and wait for **NEXT**. Do not interpret NEXT as permission to complete the entire roadmap. Do not commit, create a remote repository, push, install tooling, or publish as part of an unrelated step.

## Product authority

- Preserve the distinction between agreed decisions, proposals, and unresolved questions.
- Do not invent Type 2 rules, board edges, starting positions, capture thresholds, or draw conditions.
- V1 targets Solo and Nearby. Pass & Play and internet multiplayer are deferred.
- Theme choices must preserve gameplay and board readability.
- Do not turn proposed difficulty names or architecture into approved requirements without recording the decision.

## Proposed implementation boundaries

If the three-module proposal is adopted:

- `game`: deterministic rules and state transitions, with no Android, UI, storage, or connectivity dependency; computer strategy uses legal moves from this engine.
- `nearby`: discovery, connections, and transport; it must not independently define legal game moves.
- `app`: screens, match coordination, persistence, preferences, and theme rendering.

Resolve match authority, message ordering, reconnect, and saved-match agreement before implementing the multiplayer protocol.

## Verification and reporting

No app build or test commands exist yet. Do not claim they ran. Once the Android scaffold exists, document verified commands in README and reuse them in CI.

Test game behaviour against approved rule examples. Check rendered UI for screen changes. Nearby acceptance requires two physical phones; emulator or unit-test success alone is not proof of radio connectivity.

Report what changed, what was checked, what remains unresolved, and the next setup step. Keep implementation, automated tests, emulator evidence, physical-device evidence, and release readiness distinct.

## Repository hygiene

Preserve unrelated changes. Keep credentials, signing keys, local SDK paths, generated build outputs, and device logs out of version control. Commit only deliberate shared project configuration. Do not duplicate global Codex credentials or session state into `.codex/`.
