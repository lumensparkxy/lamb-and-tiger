# Open questions

## Before rules implementation

The [board and consolidated game-rules specification](product/game-rules.md) is approved as the baseline for issue #1 in D017. R01–R10 record the resolved core rules, with detailed approvals in D010–D016. The mapping includes 23 nodes, 38 edges, and 56 directed straight capture paths; reviewed examples include the concrete threefold-repetition sequence.

No core board/rules decision remains open for issue #1. Specification approval is complete; see [issue #1](https://github.com/lumensparkxy/lamb-and-tiger/issues/1) for publication and completion status. Implementation has not started.

## Before committing Nearby to release

- Which two physical Android phones are available for testing?
- What Android versions and Google Play services availability must we support?
- What are the host/join and connection-confirmation flows?
- How are approved Nearby draw offers delivered, expired, and recovered across interruptions?
- Which device coordinates authoritative match state?
- How are duplicate, late, out-of-order, or incompatible messages handled?
- What happens on screen lock, app backgrounding, process death, or a lost connection?
- Under what conditions can both players resume a saved match?

Deliverable: bounded two-phone experiment and recorded findings; no production reliability claim from a single happy-path run.

## Before feature completion

- What recovery flow follows a reported invalid game state? D016 requires moves to stop and the problem to be reported; recovery details are not yet specified.
- How are roles chosen before games, and retained or swapped for rematches? The discussed Solo/Nearby proposal remains unapproved.
- Final number/names of difficulty levels and evaluation criteria?
- Hints and undo rules for Solo and Nearby?
- Tutorial scope, initial themes, sound, vibration, and accessibility settings?
- Supported languages, orientations, phone sizes, and tablet expectations?

## Before publication

- Final app name, application ID, repository ownership and visibility?
- Monetization and analytics choices?
- Applicable store audience, privacy, and SDK disclosure requirements based on the actual implementation?
- Signing custody, distribution track, and release acceptance criteria?

These questions should be resolved at the relevant milestone; they are not all prerequisites for the initial scaffold.
