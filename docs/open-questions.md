# Open questions

## Before rules implementation

- What are the exact nodes, edges, and allowed capture paths of the Type 2 board?
- How many pieces are used, where do tigers start, and who plays first?
- How does lamb placement work, and when can placed lambs move?
- Are captures mandatory? Are chained captures allowed?
- What constitutes tiger victory, lamb victory, and a draw?
- How are repetitions or positions with no legal move handled?

Deliverable: illustrated rules specification with concrete legal/illegal move and outcome examples.

## Before committing Nearby to release

- Which two physical Android phones are available for testing?
- What Android versions and Google Play services availability must we support?
- What are the host/join and connection-confirmation flows?
- Which device coordinates authoritative match state?
- How are duplicate, late, out-of-order, or incompatible messages handled?
- What happens on screen lock, app backgrounding, process death, or a lost connection?
- Under what conditions can both players resume a saved match?

Deliverable: bounded two-phone experiment and recorded findings; no production reliability claim from a single happy-path run.

## Before feature completion

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
