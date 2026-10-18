> **D041 approval:** The user approved both contracts and the Solo-first sequencing exception and authorized continuous implementation. Nearby remains deferred. The review text below records the package at approval; see the [implementation evidence](../implementation/solo-first-evidence.md) for current execution status.

# Consolidated prerequisite review

Prepared 2026-10-17 under D039. The user requested completion of prerequisites together before implementation, authorized routine technical/visual choices, and asked for one final review. This document is that review package. No production implementation is authorized or started by preparing it.

## Decisions already settled

- D017: complete 23-point rules/graph and examples.
- D018–D033: Solo/Nearby product flows, five difficulty names, hints/Undo, saving, themes, accessibility, English/Tamil and recovery behaviour.
- D036: further Nearby investigation is deferred, with physical-device feasibility still required.
- D037/D038: three modules, Kotlin/Compose and immutable engine/action contract.
- D039: batch prerequisite work; choose routine implementation details and present material architecture in one final review.
- D040: **Lamb and Tiger**, application ID **`com.maswadkar.lambandtiger`**.

## Package for one final review

| Area | Concrete recommendation | Status |
| --- | --- | --- |
| [Storage/restoration](saved-state-and-restoration.md) | Framework SQLite; two independent mode slots; versioned event history; durable pending operations; replay validation; no destructive migration/automatic rollback; private local saves excluded from backup/transfer. | New material contract, proposed. |
| [Nearby protocol](nearby-match-protocol.md) | Fixed original-host coordinator; both engines validate; authenticated pairing and saved-peer continuity; ordered durable prepare/commit/settlement; blocked uncertain recovery; explicit mutual resume. | New material contract, proposed; physical feasibility unproven. |
| [Build and QA](build-and-verification.md) | Pinned Kotlin/Compose stack; min 29 / compile 37 / target 36; test matrix and future CI commands; debug identity separated from final app ID. | Routine selections, metadata checked; production build pending. |
| [AI/UI/tutorial details](implementation-details.md) | Legal-action-only bounded AI, evaluation targets, original readable board/pieces, adaptive layouts and language review path. | Routine choices/engineering targets; runtime and review evidence pending. |

The new contracts preserve existing game rules. They deliberately separate engine legality, durable saving and peer settlement. Alternatives considered: Room adds compiler integration for a very small store; atomic whole-file saves make coordinated journals/slot updates harder; peer elections/host migration add recovery ambiguity; optimistic play during disconnect conflicts with approved mutual agreement. These alternatives are not needed for V1.

## Actual blockers and what can proceed

Live GitHub checked on 2026-10-17: #1/#2 are closed; #3–#14 are open; no open PRs; working branch is `main` with existing uncommitted planning/harness work. Issue #4 depends on #3; #5 depends on #4; #8 also depends directly on #3. No issue has been closed or dependency silently removed.

**Full V1 prerequisites are not all complete:** two physical phones and issue #3's feasibility/go-no-go evidence are still missing. A protocol document cannot substitute for that evidence. Storage/protocol approval is also awaiting this final review.

Recommended execution choice for final approval: allow a **transport-independent implementation sequence** after accepting this package, while retaining #3/#4/#8 as incomplete until their full criteria are met. That is an explicit sequencing exception to #5's whole-issue dependency on #4, not an assertion that #4 is complete. The alternative is to finish the two-phone experiment first and then begin the whole implementation. The user has not yet selected either path; the previous Nearby deferral remains effective.

| Gate | Blocks starting transport-independent implementation? | Blocks full Nearby/release completion? |
| --- | --- | --- |
| Accept storage contract and the proposed sequencing exception | Yes | Yes |
| Accept protocol design | Required before protocol implementation; may be approved in this same review | Yes |
| Two phones + #3 transport results | Under current dependencies yes; the proposed exception would permit the independent sequence only | Yes; never waived by the exception |
| First production dependency resolution/build | First implementation task, cannot be proven before a scaffold | Yes if it fails |
| Reviewed Tamil text and rendered localization | No; resource architecture is defined | Yes for complete English/Tamil V1 |
| AI strength/performance, UI/storage/protocol tests | No; designed checks require implementation | Yes for the relevant acceptance criteria |
| Release owner/account, signing, track, audience, monetization/analytics policy | No; no release configuration/SDK additions included | Yes before publication/release preparation |

## Planned uninterrupted implementation sequence

After explicit implementation authorization and the sequencing choice:

1. **#5 foundation:** preserve/commit only separately authorized work, create the three modules with an inert Nearby boundary, pinned catalog, launchable Compose screen, doctor and local build/test/lint/CI configuration. Resolve ordinary build issues; prove launch before progressing. No live Nearby adapter under the exception.
2. **#6 engine:** implement topology, D038 state/actions, validation/outcomes/repetition/replay and fixtures; satisfy the pure JVM tests.
3. **#7 board:** implement rendered board and interaction, point-list accessibility, screen-state coordination and result/recovery presentation. Development-only test control is not a public Pass & Play mode.
4. **#9/#10 Solo:** AI scheduling and evaluation, storage/Continue/Undo, failure injection, process recreation and two save-slot isolation. Implement storage when first needed, while retaining issue-specific acceptance tracking.
5. **#11/#12 polish/learning:** themes/settings, tutorial/hints, adaptive/TalkBack/large text, English/Tamil resources and measured UI checks. Translation review can be batched separately; no claim of reviewed Tamil until it occurs.
6. **#3/#4/#8 Nearby lane:** only after an explicit return to the experiment and successful feasibility decision, finalize any transport-driven protocol changes, test coordinator with fake faults, implement the actual adapter, and prove integrated two-phone matches/recovery. Deferred work is never reported as finished.
7. **#13/#14 release lane:** obtain missing ownership/signing/audience/distribution decisions and assemble revision-bound evidence. No release upload/publish is included in a development authorization.

Builds, dependency resolution, normal fixes and local emulator verification are part of the future implementation request, not executed now. Commit/push/PR/merge or remote issue mutations need the scope authorized at that time. The one-final-review approach does not promise never to stop for missing hardware, failed acceptance, a material scope change or credentials that the agent cannot supply.

## Backlog reconciliation prepared, not sent

When GitHub edits are separately authorized: link these contracts on #4; update #5's dependency wording only if the sequencing exception is accepted; replace #9's already-settled difficulty-name question with D026 evaluation; replace #11's theme-selection question with D025 implementation; mark #12's conditional Nearby Undo as excluded by D024; record D040 on #13. Keep #3/#8 physical evidence requirements and all unperformed checks unchecked. Do not close #4 based on the independent work alone.

## Verification performed for this package

Read the approved product/rules/architecture records and live issue bodies; checked branch/working tree/open PRs; inspected installed SDK/JDK/AVD/device inventory; fetched selected official Maven POMs and AAR metadata and reviewed relevant Android/Nearby documentation. Final documentation checks passed: 20 Markdown files, 162 local link targets, balanced code fences, whitespace, and unique sequential decision IDs D001–D040. These are documentation checks, not application tests. No production build, game tests, emulator UI checks or physical connectivity tests were run in this planning step.

Final review needs two material choices together: accept the storage/protocol recommendations, and choose the proposed independent implementation sequence or completion of the two-phone experiment first. Physical phone details, Tamil review and release choices remain explicitly owned future gates, not hidden prerequisites claimed complete.
