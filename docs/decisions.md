# Decisions and proposals

Recorded 2026-10-17 from the project discussion.

## Agreed decisions

| ID | Decision |
| --- | --- |
| D001 | Build a new Android app with the working title Lamb and Tiger Game. |
| D002 | **Superseded by D010.** Originally selected the reference board labelled Game type -2; exact rules remained open. |
| D003 | Design for children, families, casual adults, and broader strategy audiences. |
| D004 | Offer computer difficulty levels with playful names. |
| D005 | Use a clean modern default board with themes selectable in settings. |
| D006 | Target nearby two-phone play without app accounts, subject to feasibility testing. |
| D007 | Defer Pass & Play to V2 or later; omit it from the V1 home screen. |
| D008 | Defer internet multiplayer to V2 or later. |
| D009 | Begin locally under `/Users/admin/Documents/ChatGPT/lamb-and-tiger`; proceed one setup step per NEXT. |
| D010 | On 2026-10-17, switch to the 23-point Aadu Puli Aatam board shown in the user-provided screenshot, replacing the 19-point Type 2 board in D002. This aligns the board with the Aadu Puli Aatam references consulted for traditional setup. The board selection does not settle all rules or automatically approve the labelled transcription. |
| D011 | On 2026-10-17, approve the initial setup on the D010 board: 3 tigers at A, D, E; 15 lambs off-board; lambs play first. The user explicitly confirmed this setup. Other turn, movement, capture, and outcome rules remain unresolved. |
| D012 | On 2026-10-17, approve the six turn and placement rules recorded in the [rules specification](product/game-rules.md#approved-turns-and-placement-d012): alternate lamb/tiger turns; place one reserve lamb per lamb turn with no movement of placed lambs; act with one tiger per tiger turn, including movement or capture during placement; permanently remove captured lambs without replenishing reserve; after the 15th placement, tiger turn follows and later lamb turns move one survivor; placement ends at zero reserve regardless of captures. Exact movement/capture mechanics and endings remain unresolved. |
| D013 | On 2026-10-17, approve the six [movement and capture rules](product/game-rules.md#approved-movement-and-captures-d013): ordinary moves follow one drawn edge to an adjacent empty point; lambs move only after all 15 placements and never jump/capture; tigers capture one adjacent lamb by a straight jump to the empty point immediately beyond; no turning, skipped points, or jumping tigers; capture is optional; exactly one jump per capture turn, with no chains. |
| D014 | On 2026-10-17, approve the four [win conditions](product/game-rules.md#approved-win-conditions-d014): tigers win on five total lamb captures across all three tigers; lambs win when all three tigers simultaneously lack any legal ordinary move or capture; either win may occur during or after placement; check after every completed action and end immediately, allowing no further moves. Five is the chosen game target, not a claim that trapping becomes mathematically impossible. |
| D015 | On 2026-10-17, approve the five [draw rules](product/game-rules.md#approved-draw-rules-d015): automatic draw on the third occurrence of the same position, not necessarily consecutive; compare species occupancy, side to move, phase, reserve and captured counts, ignoring individual same-species identities; Nearby draw offers require explicit opponent acceptance and may be declined; Solo uses repetition draws with no negotiated draw in V1, and leaving is not a draw; check wins before repetition after every completed action, and never convert a completed win into an agreed draw. |
| D016 | On 2026-10-17, approve [lamb-no-action handling](product/game-rules.md#approved-lamb-no-action-handling-d016): no automatic pass; a lamb turn with no legal action is an invalid game state, not a win or draw; stop further moves and report the problem. Recovery behaviour remains for implementation planning. |
| D017 | On 2026-10-17, approve the [23-point board mapping and consolidated game-rules specification](product/game-rules.md) as the baseline for issue #1, including the A–W labels, 38 undirected edges, 56 directed straight capture paths, D011–D016 rules, and reviewed examples. This records specification approval; it does not authorize implementation, commit/push, or GitHub issue closure. |

The [19-point rules draft](product/archive/type-2-game-rules.md) and its diagram are retained as superseded reference material. The [approved baseline](product/game-rules.md) uses the new board. The starting setup is approved in D011, turns/placement in D012, and movement/captures in D013. The earlier entries record the scope of their approvals; D013 resolves the movement/capture questions left open by D011/D012. D014 approves win conditions and immediate checks after completed actions. D015 approves repetition and agreed-draw policies, including win priority. D016 resolves lamb-no-action handling as an invalid-state error, not a game outcome. D017 approves the graph transcription and consolidated baseline following the final consistency review.

The private [GitHub repository](https://github.com/lumensparkxy/lamb-and-tiger) exists and `origin` is configured, superseding the initial planning note that no remote existed. After D017, the user separately authorized committing and pushing the approved baseline, updating GitHub wording to the 23-point board, and closing issue #1. That authorization explicitly stops before issue #2; it does not approve implementation.

## Proposals awaiting validation or approval

- Three modules: `app`, pure Kotlin `game`, and Android `nearby`.
- Kotlin and Jetpack Compose for Android implementation.
- Google Nearby Connections as the local transport; confirm device support and required permissions in the experiment.
- Modern and Midnight Prowl as the initial two themes.
- Difficulty names: Sleepy Whiskers, Baa-sic Instinct, Sneaky Paws, Woolly Schemer, Grandmaster Growl.
- Interactive tutorial, contextual Continue, and mode-specific hints/undo.
- Role selection and rematches: Solo role choice with same-role replay or Switch Sides; Nearby opposite-role agreement and mutually confirmed swaps. Discussed but not approved by the D012 rules approval.
- No advertising, purchases, or custom analytics initially; business decisions are still open.

## Decision maintenance

When a proposal is accepted, record the decision and rationale here. Introduce separate architecture decision records when a substantial technical tradeoff is resolved. Preserve superseded decisions with their replacements rather than silently rewriting history.
