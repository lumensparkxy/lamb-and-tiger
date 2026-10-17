# Issue #4, step 1 — approved module and UI architecture

Status: approved by the user on 2026-10-17, recorded in D037. Originally proposed under D036, this document records the module responsibilities, dependency direction, and Kotlin/Jetpack Compose choice for the transport-independent portion of [issue #4](https://github.com/lumensparkxy/lamb-and-tiger/issues/4). Approval does not satisfy issue #4's dependency on the unfinished issue #3 or authorize implementation. Nearby investigation remains deferred.

## Approved choice

Use Kotlin with three production modules. Use Jetpack Compose for the production UI. Keep the disposable Android Views harness separate.

| Module | Owns | Boundary |
| --- | --- | --- |
| `game` | Approved board topology, immutable game-state model, legal actions, deterministic transitions, outcome/repetition logic, and pure computer-choice policies | Plain Kotlin/JVM. No Android, rendering, storage, connection SDK, or device lifecycle dependency. Computer policies consume the engine's legal actions; the engine does not depend on a policy. Randomness for computer choices must be explicit input, never hidden in rule transitions. |
| `nearby` | Device discovery, connection lifecycle, and transport of bytes/events through an app-facing interface | Android library, with no game-rule implementation or independent decision that a move is legal. Transport/library choice remains provisional. No production implementation while the experiment is deferred. |
| `app` | Compose screens, input, match coordination, AI task scheduling/cancellation, persistence, preferences, themes, and Android lifecycle handling | Calls the rules engine for state changes. Converts UI actions and, later, agreed protocol messages into engine actions. Storage and wire representations stay outside the engine. |

Dependency direction: `app → game` and, when Nearby integration is authorized, `app → nearby`. Neither lower module depends on `app`; `game` and `nearby` do not depend on each other. Host status does not determine match authority.

The later match coordinator/protocol belongs to `app` in this approved three-module architecture, separated internally from rendering and persistence. That placement does not decide authority, ordering, acknowledgement, reconnect, or saved-state agreement. Those contracts must be resolved before protocol code.

## Rationale

- The rule engine can be tested against D017 examples without an emulator or radio.
- UI themes and connectivity changes cannot redefine legal play.
- The uncertain transport stays behind a module boundary while rules and Solo design can progress.
- Three modules establish the approved separation without introducing a separate module for every screen or concern.

Compose is Android's recommended native UI toolkit and supports state-driven UI, theming, and adaptive layouts. These capabilities fit the approved screen/theme requirements; they do not prove our board, TalkBack, or 200% text behaviour. Those still need implementation and rendered checks. [Official Compose overview](https://developer.android.com/compose), reviewed 2026-10-17.

## Alternatives considered

| Alternative | Tradeoff |
| --- | --- |
| One Android application module | Less initial build configuration, but no build-enforced separation between pure rules and Android/transport dependencies. |
| Additional modules for AI, storage, and every feature | More boundaries, but more configuration before the interfaces and workload justify them. Keep these concerns separate within the three modules first. |
| Android Views for production UI | Viable and already used by the diagnostic harness. Compose is the approved production choice for state-driven screens and themes; the harness retains Android Views. |

## Still open and outside this step

D041 approves the storage/versioning and protocol/authority contracts and Solo-first sequence in the [prerequisite package](implementation-readiness.md). D040 settles the app name and application ID. Production Solo modules now exist; see the [implementation evidence](../implementation/solo-first-evidence.md). Transport adoption and physical evidence remain pending. Android 10+, the initial API 36 target, English/Tamil scope, and all approved rules/flows remain unchanged.

The harness's build versions and launch timings are experiment evidence only. Do not copy its dependencies into the production app without checking the selected production stack. Nearby remains a V1 target with its two-physical-phone acceptance gate. Issue #3 is deferred and incomplete; issue #4 cannot be closed from this module/UI approval alone.

## Next bounded step

The module/UI choice is recorded in D037. The [immutable game-state and action contract](game-state-and-actions.md) is approved in D038. D041 completes that prerequisite review and authorizes continuous Solo-first implementation. The three production modules exist; `nearby` is an inert boundary, with live protocol integration and testing deferred.
