# Lamb and Tiger Game

An Android strategy game for children, families, and casual adults, using the **23-point Aadu Puli Aatam board** selected on 2026-10-17. See the [approved board and rules specification](docs/product/game-rules.md).

## Project status

The Solo-first implementation is locally playable and verified under **D041** on `codex/solo-first`. The production app now contains the pure rules engine, offline computer play, SQLite saving/recovery, Hint/Undo, themes/settings, rules and interactive lessons, and English/Tamil resources. The user-confirmed identity is **Lamb and Tiger**, **`com.maswadkar.lambandtiger`**; debug installs use `.debug`.

The [implementation evidence](docs/implementation/solo-first-evidence.md) separates completed checks from remaining acceptance gates. Tamil text is a draft pending fluent review. The [approved storage](docs/architecture/saved-state-and-restoration.md) and [protocol](docs/architecture/nearby-match-protocol.md) contracts remain authoritative. Nearby is visibly unavailable in this development build: its module is inert, its experiment remains deferred, and there is no physical-phone connectivity claim. Issues #3/#4/#8 remain incomplete. D042 authorizes delivery of the Solo-first branch and revision-bound issue updates; merging and release publication are separate gates.

The approved board and rules are D017; product flows are D018–D033; module/UI and immutable state contracts are D037/D038. D041 explicitly permits the independent implementation sequence while preserving the two-phone gate.

## Product direction

- **Play AI** — subtitle: **Challenge the computer**. Solo mode has [five approved difficulty levels](docs/product/brief.md#approved-ai-difficulty-levels-and-behaviour-d026), with **Baa-sic Instinct — Easy** as the first-game default. All adjacent levels met the selected aggregate head-to-head progression targets; the 1,600-game results and limits are in the implementation evidence.
- **Play Friend** — subtitle: **Play nearby on two phones**. Nearby mode targets local play without app accounts; technical feasibility must be verified on physical devices.
- **Modern** default light theme and **Midnight Prowl** dark theme, with sound/vibration Off by default and Reduced Motion support; [Settings rules and defaults (D025)](docs/product/brief.md#approved-settings-and-defaults-d025).
- A focused home screen with Play AI and Play Friend as the two primary actions; exact labels and subtitles approved in D018. Home navigation, setup, and secondary Settings are approved in [D019](docs/product/brief.md#approved-home-navigation-and-setup-d019).
- Contextual Continue and one independent saved game per mode, with protected replacement and mutually confirmed Nearby resume, are approved in [D020](docs/product/brief.md#approved-continue-and-saved-game-behaviour-d020).
- Leaving an active game and interruptions pause play, with explicit resume and no departure penalties, as approved in [D021](docs/product/brief.md#approved-active-game-departure-and-interruptions-d021).
- [Approved device/language scope (D031)](docs/product/brief.md#approved-device-support-and-languages-d031): Android 10+ phones/tablets; English and Tamil selected from Android system language, with English fallback. English/Tamil resources and adaptive layouts are implemented; Tamil review and the broader platform/device matrix remain pending.
- Pass & Play and internet multiplayer are deferred to V2 or later.

## Planning documents

- [V1 brief](docs/product/brief.md)
- [Screen flows and acceptance criteria — approved issue #2 baseline](docs/product/screen-flows-and-acceptance.md)
- [23-point board and rules — approved baseline](docs/product/game-rules.md)
- [Decisions and proposals](docs/decisions.md)
- [Open questions](docs/open-questions.md)
- [Nearby experiment — issue #3 draft scope and checks](docs/product/nearby-experiment.md)
- [Nearby harness setup — experiment selection and verified tool inventory](docs/product/nearby-harness-setup.md)
- [Issue #4 — approved module boundaries and UI stack](docs/architecture/module-boundaries.md)
- [Issue #4 — approved immutable game-state and action contract](docs/architecture/game-state-and-actions.md)
- [Consolidated prerequisites and implementation sequence](docs/architecture/implementation-readiness.md)
- [Approved saved-state and restoration contract](docs/architecture/saved-state-and-restoration.md)
- [Approved Nearby protocol contract](docs/architecture/nearby-match-protocol.md)
- [Build choices and verification matrix](docs/architecture/build-and-verification.md)
- [AI, UI, tutorial and language implementation details](docs/architecture/implementation-details.md)
- [Development instructions](AGENTS.md)

D033 approves the consolidated screen flows and acceptance criteria as the issue #2 baseline, incorporating product decisions D018–D032. The user subsequently authorized publication and GitHub closure; see [issue #2](https://github.com/lumensparkxy/lamb-and-tiger/issues/2) for completion evidence. Issue #3 now has a minimal diagnostic scaffold, a successful debug build, four passing permission-policy unit tests, and a bounded emulator diagnostic smoke check. No connectivity checks have run; the two physical test phones remain unidentified.

Verified private repository milestones: [M1 — Specification baseline](https://github.com/lumensparkxy/lamb-and-tiger/milestone/1); [M2 — Technical foundation](https://github.com/lumensparkxy/lamb-and-tiger/milestone/2); [M3 — Playable Nearby](https://github.com/lumensparkxy/lamb-and-tiger/milestone/3); [M4 — Playable Solo](https://github.com/lumensparkxy/lamb-and-tiger/milestone/4); [M5 — V1 polish and release readiness](https://github.com/lumensparkxy/lamb-and-tiger/milestone/5).

## Technical direction

Three modules: `app` owns Compose UI, coordination, codecs and SQLite persistence; `game` is pure Kotlin/JVM with deterministic rules and separate computer policies; `nearby` is an inert Android transport boundary until the deferred experiment succeeds. No game rules or simulated connection success live in `nearby`.

## Development

Use JDK 25 (local checks use Android Studio JBR 25.0.3), SDK platform 37, and Build Tools 36.0.0. Set `JAVA_HOME` and `ANDROID_HOME` locally. Minimum SDK is 29; the approved initial target remains 36. Do not commit SDK paths or signing keys.

Verified local build/unit/lint command:

```sh
./scripts/doctor.sh
./gradlew :game:test :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :nearby:testDebugUnitTest :nearby:lintDebug --console=plain
```

Android storage, lifecycle, UI and game-flow checks on the selected running emulator:

```sh
./gradlew :app:connectedDebugAndroidTest --console=plain
```

Generated build output is under the local Gradle user directory, outside cloud-synced Documents. Discover the exact checkout-specific paths with `./gradlew printBuildLocations`; the app output contains `outputs/apk/debug/app-debug.apk`. This avoids duplicate generated class files observed in the workspace output directory. The older diagnostic project and its evidence remain separate.

AI comparison (200 paired seeds per adjacent level; results include censored games):

```sh
mkdir -p work
./gradlew :game:evaluateAi -Pseeds=200 -Pworkers=3 --console=plain > work/ai-evaluation.log
python3 scripts/summarize_ai.py work/ai-evaluation.log
```

Dependencies are pinned and locked; `gradle/verification-metadata.xml` records SHA-256 checksums for the resolved artifacts. Review dependency changes before deliberately regenerating locks/metadata. CI configuration uses the same local checks and separate API 29/34/36/37 emulator jobs; remote results are tracked in [PR #15](https://github.com/lumensparkxy/lamb-and-tiger/pull/15). No release signing/publishing is configured.

CI invokes `bash scripts/ci-connected-tests.sh` around the connected-test command to print instrumentation counts, failure reports and device crash diagnostics without changing the test exit status. Espresso 3.7.0 is explicitly pinned for API 37 input-service compatibility.

The isolated [Nearby harness](experiments/nearby-harness/README.md) has its own verified commands. Do not run its connectivity investigation as part of the Solo sequence. D041 authorizes continuous Solo-first implementation and routine fixes. D042 authorizes review, commit/push and issue evidence updates for this branch; merging and release publication remain separate scope boundaries.
