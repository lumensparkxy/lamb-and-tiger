# V1 product brief

Status: approved product baseline, 2026-10-17. D018–D032 below record approved product decisions. D033 approves the [consolidated screen flows and acceptance criteria](screen-flows-and-acceptance.md) as the issue #2 baseline; remaining proposals are not approved requirements. D041 authorizes the Solo-first implementation; see the [current implementation evidence](../implementation/solo-first-evidence.md). Historical approval statements below describe their original scope, not current test status.

## Purpose and audience

Create an approachable Android Lamb and Tiger strategy game for children, families, casual adults, and players seeking more challenge.

## Agreed direction

- Use the **23-point Aadu Puli Aatam board** shown in the user-provided screenshot on 2026-10-17 (D010, superseding D002). See the [annotated transcription](game-rules.md).
- Offer Solo play with computer difficulty levels and playful names.
- Target Nearby play between friends on separate phones without app account creation.
- Start with a clean modern board and allow theme changes through settings.
- Keep Solo and Nearby as the primary home actions, labelled **Play AI** and **Play Friend** respectively (D018).
- Defer Pass & Play and internet multiplayer to V2 or later.

Nearby is a V1 target subject to a successful two-physical-phone experiment. A failed experiment requires an explicit scope decision, not silent removal or an unverified promise.

The production module boundaries (`app`, pure Kotlin `game`, Android `nearby`) and Kotlin + Jetpack Compose UI choice are approved in D037; see the [architecture record](../architecture/module-boundaries.md). The [immutable game-state/action contract](../architecture/game-state-and-actions.md) is approved in D038. D041 approves the persistence and protocol contracts and the [Solo-first sequence](../architecture/implementation-readiness.md). D040 confirms **Lamb and Tiger** and **`com.maswadkar.lambandtiger`** as the app name and application ID. Production Solo modules now exist and are locally verified; D046 resumed the isolated harness; D047 authorizes production Nearby implementation. See [Nearby evidence](../implementation/nearby-evidence.md) for local implementation and verification limits. Two-phone acceptance remains open.

## Approved Home labels (D018)

| Action label | Subtitle | Mode |
| --- | --- | --- |
| Play AI | Challenge the computer | Solo |
| Play Friend | Play nearby on two phones | Nearby |

Use this exact wording for English; D031 requires reviewed Tamil translations. D018 approves labels and subtitles; D019 approves the Home navigation and setup behaviour below.

## Approved Home navigation and setup (D019)

The user approved these six rules for issue #2:

1. **Home:** show **Play AI** and **Play Friend** with their approved subtitles. Show **Settings** as a secondary action. Opening Home starts no match and requests no Nearby permissions.
2. **Play AI:** open setup to choose **Tiger or Lamb** and computer difficulty. Start only when the player taps **Start Game**. Lambs always play first—so choosing Tiger means the computer makes the opening placement.
3. **Play Friend:** open a choice to **Host Game** or **Join Game**. Players connect, confirm their opponent, and agree on opposite sides. The match starts only when both players confirm readiness.
4. **Settings:** open preferences, including theme selection, with a clear route back to Home. We will decide the individual settings separately.
5. **Back and cancellation:** before a match starts, Back returns to the previous screen. Leaving Nearby setup cancels searching, hosting, or the pending connection. Cancelling setup creates no game result.
6. **Nearby permissions and unavailable opponents:** request required permissions when the player chooses Host or Join. If permission is denied or no opponent is available, explain the situation and offer retry or return navigation. **Play AI remains usable.**

D020 now settles Continue and saved-game behaviour; D021 settles leaving an active game and handling interruptions. D022 settles result-screen and rematch behaviour. D023 settles tutorial and rules access; D025 settles Settings and their defaults. D026 settles the five AI difficulty levels, names, and behaviour rules. D027 details Nearby opponent confirmation and side selection. D019 approves the setup behaviour above, not the detailed Nearby protocol or implementation; Nearby feasibility still requires two physical phones.

## Approved Continue and saved-game behaviour (D020)

The user approved these seven rules for issue #2:

1. **Contextual Continue:** show **Continue** on Home only when an unfinished saved game exists. Opening the app still shows Home; it never resumes play automatically.
2. **One save per mode:** keep up to one **Play AI** game and one **Play Friend** game independently. If both exist, Continue lets the player choose which to resume.
3. **Automatic saving:** save the initial match and every completed legal action. Preserve the board, side to move, placement phase, reserve/captured counts, player roles, and repetition history. For Play AI, also preserve difficulty. Resuming must not add another repetition occurrence.
4. **Continue Play AI:** restore the saved position and settings. If it is the computer’s turn, the computer continues from there without duplicating an already completed move. No connection is required.
5. **Continue Play Friend:** reconnect with the **same opponent**. Both players must confirm resuming the same match, and their game state and repetition history must agree before moves resume. If the opponent is unavailable or the saves disagree, keep the game paused and offer retry or return Home. Never silently restart, roll back, or substitute an AI opponent.
6. **Protect existing saves:** starting a new game in a mode with an unfinished save requires explicit confirmation to replace it. Merely opening or cancelling setup preserves the save. Replacement happens only when the new match actually starts; the other mode’s save remains untouched.
7. **Completed or unusable saves:** wins and draws are no longer offered as playable Continue games. If a save is invalid, damaged, or incompatible, explain why it cannot resume; never silently reset it. Discarding it requires confirmation.

D021 now settles intentional departure from an active game and handling interruptions. The detailed Nearby synchronization/recovery protocol remains a separate decision. These are approved product requirements; implementation and two-phone verification come later.

## Approved active-game departure and interruptions (D021)

The user approved these seven rules for issue #2:

1. **Back during a game:** show **Keep Playing** and **Save & Return Home**. Pause local play and computer thinking while this choice is open.
2. **Leaving Play AI:** Save & Return Home preserves the latest completed position and returns Home. The computer makes no further moves while the game is paused.
3. **Leaving Play Friend:** either player may leave without needing the opponent’s permission. Preserve the match and notify the opponent when connected. The opponent sees that play is paused and can return Home too. Leaving does not award a win, loss, or draw.
4. **Interruptions:** switching apps, locking the screen, receiving an interruption that takes the game out of the foreground, or losing the Nearby connection pauses play. On the other phone, pause as soon as the interruption or connection loss is detected. App termination must recover from the saved match.
5. **Returning:** never resume moves automatically. If the game screen is still open, show **Resume**; after reopening the app, use **Continue** from Home. Play Friend requires both players to confirm readiness and agree on the same saved state/history, as approved in D020. Opening a Nearby leave dialog also pauses the session; cancelling it does not bypass this readiness check.
6. **Interrupted moves or save failures:** preserve completed actions and never duplicate a move or silently roll back. If the saved state or an in-flight Nearby move cannot be confirmed, block further play and explain the problem. Offer retry or return navigation with an explicit warning if the latest position could not be saved; never falsely claim “saved.”
7. **No departure penalties in V1:** no automatic timeout loss, disconnect forfeiture, or resignation action. A completed win or draw remains final even if an interruption follows. Discarding or replacing a save requires the confirmations already approved in D020 and does not create a game result.

Exact connection detection and recovery mechanisms remain for the later Nearby protocol work. These are approved product requirements, not evidence of implemented save/recovery behaviour or verified two-phone connectivity.

## Approved result screen and rematches (D022)

The user approved these seven rules for issue #2:

1. **Show the outcome and reason:** use **Tigers Win**, **Lambs Win**, or **Draw**, followed by the reason: five lambs captured, all tigers trapped, threefold repetition, or an accepted draw agreement. Identify the player’s side clearly. An interruption or invalid state is not a game result.
2. **Keep the final board visible:** players can inspect the finishing position and captured count. No further moves are allowed in that completed game.
3. **Play AI rematch:** offer **Play Again**, **Switch Sides**, and **Home**. Play Again keeps the player’s side and difficulty; Switch Sides changes the player’s side and keeps difficulty. To change difficulty, return through Home to setup.
4. **Play Friend rematch:** offer the same actions. Play Again proposes keeping both players’ sides; Switch Sides proposes swapping them. Show the proposed roles to both players and start only after both confirm the same choice and readiness.
5. **No automatic alternation or forced rematch:** sides change only through Switch Sides. Either Nearby player may decline or cancel a rematch request. A disconnect prevents the new match from starting until the connection and mutual readiness are restored. The previous result remains final.
6. **Every rematch is a fresh game:** reset to three tigers at A, D, E and 15 lambs off-board, with lambs first. Reset captured counts and repetition history, counting the new initial position once. Save the new match under D020; do not carry over moves or draw offers.
7. **Leaving the result screen:** Home or Back returns Home without an active-game exit warning. The completed game is not offered through Continue. If interrupted while still showing the result, restore that result when returning to the existing screen; a fresh app launch opens Home as already approved.

A match-history archive, scores across games, and statistics remain outside this approval. Nearby result agreement and rematch-message handling still need protocol specification. These are approved product requirements, not implemented or tested app behaviour.

## Approved tutorial and rules access (D023)

The user approved these seven rules for issue #2:

1. **Home access:** add **How to Play** as a secondary action alongside Settings. It opens two choices: **Quick Rules** and **Guided Tutorial**. Play AI and Play Friend remain the primary actions.
2. **Optional learning:** never force the tutorial before playing. Players can open it whenever they want, skip a lesson, or exit to How to Play.
3. **Quick Rules:** provide an illustrated, scrollable reference covering the approved board/setup, placement, movement, optional single captures, wins, and draws. Explain that lambs always start, captures cannot chain, and Nearby draw agreement requires both players.
4. **Guided Tutorial:** use short interactive lessons to practise placing a lamb, moving after placement, moving a tiger, capturing, and recognising wins and draws. Explain incorrect attempts and let the player retry. Use the approved board and rules throughout.
5. **Independent practice:** tutorial examples never modify a real match, saved game, difficulty setting, or repetition history. For V1, tutorial progress is not saved; reopening starts at the lesson list, where any lesson can be selected.
6. **Rules during a game:** provide **Rules** in the game menu to open Quick Rules. Pause the match and computer thinking while it is open. Returning requires explicit Resume; Nearby also requires both players’ readiness under D021. The interactive tutorial is accessed from Home.
7. **Offline and repeatable:** both learning options work without internet or an opponent. Finishing the tutorial returns to How to Play with a clear route Home; it never starts a match automatically.

D024 now settles hints and undo during actual games. Tutorial wording and example positions will be checked against the approved specification when implemented. These are approved product requirements, not implemented or tested app behaviour.

## Approved hints and undo (D024)

The user approved these seven rules for issue #2:

1. **Legal destinations in both modes:** selecting a playable piece highlights its legal destinations. During lamb placement, indicate available empty points. This explains permitted actions without recommending a strategy.
2. **Play AI hints:** offer **Hint** during the human player’s turn, at every difficulty, without a usage limit. Highlight one suggested legal placement, move, or capture. Never play it automatically or promise it is the best move. Hints work offline.
3. **Play AI undo:** offer **Undo** on the human player’s turn. Restore the position immediately before their most recent action, undoing both that action and the computer’s reply. Repeated Undo can step back through earlier human turns. The computer’s opening lamb placement cannot be undone by itself.
4. **Availability:** hints and undo are unavailable while the computer is acting, while the game is paused or invalid, and after a win or draw. Undo is also unavailable before the human has made an action. Explain unavailable actions clearly.
5. **Complete restoration:** Undo restores pieces, captured lambs, reserve counts, phase, side to move, and repetition history. Remove the undone actions and their repetition occurrences; restoring a position does not count as another occurrence. Save the restored state and remaining move history so Undo still works after Continue. V1 has no Redo.
6. **Play Friend:** provide no strategic hints, undo requests, or takebacks in V1. Both players retain legal-destination highlights and access to Rules. A completed legal action stands.
7. **Hints do not change the game:** requesting or dismissing a hint consumes no turn and changes no saved position or repetition count. Clear the suggestion whenever the position changes, including after Undo, and discard results calculated for an older position.

Undo is an explicit, player-requested exception to preserving completed moves; interruption recovery still never silently rolls back. D024 extends D020 saving with the remaining move history needed for Solo Undo. These are approved product requirements, not implemented or tested app behaviour.

## Approved Settings and defaults (D025)

The user approved these seven rules for issue #2:

1. **Access and return:** open Settings from Home or the game menu. During a match, opening Settings pauses play and computer thinking. Returning requires explicit Resume; Play Friend also requires both players’ readiness under D021.
2. **Two initial themes:** **Modern** is the default light theme; **Midnight Prowl** is the dark theme. Show a board preview when choosing. Additional themes remain outside V1.
3. **Appearance only:** changing themes preserves the board position, roles, turn, difficulty, and history. Both themes must keep board lines, pieces, selection, and legal destinations clear. Distinguish tigers and lambs by shape as well as colour.
4. **Sound Effects:** provide an On/Off toggle, **Off by default**. When enabled, use short cues for moves, captures, and results. V1 has no background music. Respect device audio controls; essential information must also appear visually.
5. **Vibration:** provide an independent On/Off toggle, **Off by default**. When enabled and supported, use brief feedback for the player’s completed actions. Respect device restrictions. If unavailable, disable the control with an explanation; gameplay remains fully usable.
6. **Reduced Motion:** provide a toggle that removes decorative movement and uses immediate or minimal board transitions. Enable it by default when the device requests reduced motion. Keep turns, captures, and results understandable without animation.
7. **Save preferences locally:** apply changes immediately and remember them across app restarts. Preferences belong to each phone, so Nearby players may use different themes, sound, vibration, and motion settings. Changing preferences never replaces a saved game. Role and difficulty remain in game setup.

Exact visual designs and sound assets remain for later review. D030 now approves broader accessibility and screen-layout requirements. These are approved product requirements, not implemented or tested app behaviour.

## Approved AI difficulty levels and behaviour (D026)

The user approved these five levels for issue #2:

| Level | Name | Label | Intended experience |
| --- | --- | --- | --- |
| 1 | **Sleepy Whiskers** | Very Easy | Gentle practice with frequent missed opportunities |
| 2 | **Baa-sic Instinct** | Easy | Simple challenges while learning |
| 3 | **Sneaky Paws** | Medium | Notices immediate threats and opportunities |
| 4 | **Woolly Schemer** | Hard | Plans ahead and makes fewer mistakes |
| 5 | **Grandmaster Growl** | Expert | The strongest V1 opponent |

These descriptions are design targets; **Expert does not mean unbeatable or proven optimal**.

The user approved these six behaviour rules:

1. **Default:** select **Baa-sic Instinct — Easy** for the first game. For later new games, preselect the difficulty used in the most recently started Play AI match.
2. **Clear selection:** show the playful name, plain difficulty label, and short description in Play AI setup. All five levels are available immediately, whether the player chooses Tiger or Lamb.
3. **Fixed per match:** difficulty cannot change during a match and never adjusts secretly. Continue restores the saved difficulty; Play Again and Switch Sides retain it. Changing difficulty requires a new game through setup.
4. **Same rules:** every level uses the approved legal moves, setup, capture target, and draw rules. Easier opponents make weaker choices; they receive no different rules or extra pieces. All levels work offline.
5. **Learning support:** Hint and Undo remain available at every level under D024. Using them does not change the selected difficulty or add a penalty.
6. **Verify the progression:** evaluate fixed scenarios and repeated games with each AI playing both sides. Measure legal play, relative strength, and response time. Build the easiest and strongest opponents first, then tune the intermediate levels. If adjacent levels are not meaningfully different, revisit their tuning or the five-level plan before release.

These are approved product requirements. No AI implementation or strength/performance evaluation has been performed; algorithms, evaluation fixtures, and quantitative acceptance thresholds remain implementation-planning work.

## Approved Nearby opponent confirmation and side selection (D027)

The user approved these seven rules for issue #2:

1. **Find the host:** Host Game shows a session label that the friend can recognise. Join Game lists available sessions and lets the player select one. Discovery or selection alone never starts a match.
2. **Confirm the opponent:** show the same short confirmation code on both phones. Players compare it and each tap **Confirm Opponent**. Either may reject or cancel; mismatched codes must not proceed. No account or real name is required.
3. **Choose sides:** after both confirm, each player selects **Tiger** or **Lamb**. Both phones show each player’s selection. If both choose the same side, explain the conflict and keep Ready unavailable until one changes. Hosting gives no automatic side preference.
4. **Confirm readiness:** once opposite sides are selected, each player taps **Ready**. Show both readiness states and remind players that **lambs play first**. A side change clears both readiness confirmations. A ready player can cancel readiness while waiting.
5. **Start together:** start only when both phones agree on the opponent, roles, and readiness, and any required saved-game replacement confirmation under D020 is complete. Show **You are Tigers** or **You are Lambs** clearly on each game screen. Host status does not determine the first turn.
6. **Handle cancellation and connection loss:** rejection or cancellation ends the pending pairing and returns to Host/Join. A connection loss blocks starting and clears readiness. After reconnecting, repeat opponent confirmation and review the roles before either player becomes ready again. Pre-match cancellation preserves existing saves.
7. **Keep resume and rematch distinct:** Continue reconnects the same saved match with its existing roles under D020; it does not reopen side selection. Rematches use Play Again or Switch Sides under D022, with both players confirming the proposed roles and readiness.

The confirmation-code mechanism, saved-opponent identification, message ordering, and authoritative match coordination remain for protocol design and two-phone testing. Choosing Host here does not settle technical match authority. These are approved product requirements, not implemented or verified connectivity behaviour.

## Approved Nearby draw-offer interaction (D028)

The user approved these seven rules for issue #2:

1. **Offer Draw:** either player may offer a draw **on their own turn** in an ongoing, connected Play Friend match. It is unavailable during setup, pauses, uncertain game states, or after completion. Play AI has no draw-offer action.
2. **Confirm before sending:** tapping **Offer Draw** asks, “Offer your friend a draw?” with **Send Offer** and **Cancel**. Cancelling sends nothing and changes no game state.
3. **Wait for a response:** after sending, pause play while the offer is pending. Show the sender **Waiting for your friend** with **Withdraw Offer**. Show the opponent **Accept Draw** and **Decline**. Allow only one pending offer; silence never means acceptance, and there is no automatic acceptance timeout.
4. **Accept:** explicit opponent acceptance ends the match as **Draw — By agreement**, provided the offer is still valid and the match has not already ended. Both phones must confirm the same result before presenting it as settled. A completed win can never be replaced by a draw.
5. **Decline or withdraw:** close the offer without a result. Keep the same position and side to move. Resume only after both players confirm readiness, following D021. Neither action consumes a turn.
6. **Interruption:** an unanswered offer is cancelled when either player leaves or the connection is lost; it is not automatically restored by Continue. If acceptance may already have occurred but the outcome cannot be confirmed, keep the match paused and reconcile it before allowing further moves. Never guess a draw or silently erase an accepted result.
7. **Keep offers tied to the match:** an offer applies only to the current match and position. Position changes or a completed result invalidate an outstanding offer; delayed responses cannot affect a later position or rematch. Offers, responses, and readiness confirmations never add repetition occurrences.

The exact handling of simultaneous acceptance, withdrawal, and connection loss remains protocol-design work under these requirements. These are approved product requirements, not implemented or verified two-phone behaviour.

## Approved game-screen interaction and status (D029)

The user approved these seven rules for issue #2:

1. **Board and player identity:** keep the approved 23-point board prominent. Show **Play AI** or **Play Friend**, the player’s side, and whose turn it is. In Play AI, also show the selected difficulty.
2. **Phase and counts:** show **Placement phase** or **Movement phase**, **Lambs to place: N**, and **Lambs captured: N / 5**. During placement, remind the lamb player that placed lambs cannot move yet.
3. **Place a lamb:** on the human lamb player’s placement turn, tapping an available empty point places one lamb immediately. No reserve-piece selection, dragging, or extra confirmation is required.
4. **Move or capture:** tap one of your pieces, then a highlighted legal destination to act. Tap the selected piece again to deselect, or another of your pieces to change selection. Distinguish capture destinations from ordinary moves using more than colour. An illegal tap changes no game state and gives a brief explanation.
5. **Accept input only when playable:** block board actions during the opponent’s turn, pauses, pending draw offers, uncertain connection/state, and completed games. After submitting an action, prevent duplicate taps or queued moves until it is resolved. Show clear text such as **Computer thinking**, **Waiting for your friend**, **Paused**, or **Confirming move**.
6. **Show what happened:** briefly mark the latest move’s origin and destination—or the placement point—and identify any captured lamb. Clear the old selection and update the turn, phase, and counts after the action is confirmed. Respect Reduced Motion; feedback must remain understandable without animation.
7. **Controls and menus:** show **Hint** and **Undo** for Play AI with D024 availability rules. The game menu contains **Rules**, **Settings**, and **Save & Return Home**, plus **Offer Draw** for Play Friend under D028. Opening the menu alone does not pause play; entering Rules, Settings, or the exit confirmation follows the approved pause behaviour. A completed game uses the D022 result actions instead.

D030 now approves adaptive layout, touch-target, text-scaling, and screen-reader requirements. Exact rendered designs remain to be settled; D031 now defines minimum supported window dimensions. These are approved product requirements, not implemented or tested game-screen behaviour.

## Approved accessibility and screen layouts (D030)

The user approved these seven rules for issue #2:

1. **Phones and tablets:** support portrait and landscape layouts in V1. Use the available space: controls below the board on narrow screens and beside it on wider screens. Preserve the approved board geometry.
2. **Safe rotation and resizing:** changing orientation or window size preserves the match, turn, selection, settings, and repetition history. It must not restart play, duplicate a move, or bypass an existing pause.
3. **Comfortable touch targets:** use at least **48 × 48 dp** for buttons and controls, following [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/apps). For closely spaced board points, provide an accessible **point-selection list** as an alternative to precise board taps. It uses the same placement and piece-then-destination actions; no dragging is required to play.
4. **Readable text and themes:** support system text enlargement through **200%** without hiding essential information or controls. Allow wrapping and scrolling instead of shrinking text. Target at least **4.5:1 text contrast** in both themes, and retain shape/text distinctions for pieces, selections, captures, and status. Android’s guidance supports this contrast target for ordinary text. [Source](https://developer.android.com/guide/topics/ui/accessibility/apps)
5. **Screen-reader play:** make the complete game usable with TalkBack. Announce each board point’s label, occupancy, selection, and available action—for example, “Point C, lamb, selected.” Use a predictable focus order and announce turn changes, captures, pauses, errors, and results without repeatedly interrupting navigation.
6. **Multiple forms of feedback:** never rely only on colour, sound, vibration, or animation. Respect the approved Reduced Motion setting. Keep important errors and connection messages available until resolved or dismissed; do not make understanding them depend on a brief animation or disappearing message.
7. **Verify before release:** check both themes, both orientations, small and large phones, a tablet layout, enlarged text, TalkBack, and Reduced Motion. Exercise a complete game through accessible controls. Layout/emulator checks do not replace the required two-physical-phone Nearby testing.

D031 now sets minimum supported window dimensions and Android version scope. D030 establishes layout and accessibility requirements; it does not claim rendered UI, TalkBack, emulator, physical-device, or release verification has been completed.

## Approved device support and languages (D031)

The user approved the device-support rules with English and Tamil for V1, following Android system language and falling back to English:

1. **Minimum Android version:** support **Android 10 and newer**. This is an approved product boundary, subject to dependency checks during scaffolding—not a claim that the app already runs on those versions.
2. **Target Android version:** initially target **Android 16 / API 36**, and recheck the requirement before release. This is separate from the minimum supported version. The [official target API guidance](https://developer.android.com/google/play/requirements/target-sdk) inspected during this decision required API 36 or higher for new phone/tablet submissions; release requirements must be verified again before publication.
3. **Devices and window sizes:** support Android phones and tablets in both orientations. Set the minimum usable content area to **320 × 480 dp in portrait**, or **480 × 320 dp in landscape**, excluding system bars. Smaller windows show a clear request to enlarge the window or rotate, while preserving the match and pausing active play.
4. **Nearby availability:** check the required connectivity capabilities and permissions before hosting or joining. If the proposed Google Nearby Connections transport is adopted, check its Google Play services requirements too. When Nearby is unavailable, explain why and keep Play AI, tutorials, and settings usable. Transport selection remains subject to the feasibility experiment. [Nearby setup guidance](https://developers.google.com/nearby/connections/android/get-started)
5. **V1 languages and selection:** support **English and Tamil**. Follow the **Android system language**: Tamil (`ta`, including regional variants) uses Tamil; English uses English; any unsupported language falls back to English. Localize all app text, including tutorials, rules, errors, status messages, and screen-reader labels. Approved English button names, difficulty names, and subtitles remain the English source wording.
6. **Extensible language resources:** keep English as the complete fallback. Store text in Android language resources, separate from code and images, so more languages can be added later. V1 has no in-app language selector. Review Tamil translations, terminology, text wrapping, and TalkBack behaviour before release; Tamil difficulty names need review. Additional languages require reviewed translations and layout/accessibility checks.
7. **Verification coverage:** test the minimum Android version, relevant intermediate permission behaviours, and the current stable Android release before shipping. Cover the minimum window sizes, larger phones, tablets, and D030 accessibility requirements. Nearby still requires two physical phones; record their models and Android versions when choosing the test pair.

This decision approves device/language scope, not implementation or verified compatibility. The concrete device/window test matrix, Tamil translations and difficulty names, and technical dependency/transport validation remain pending.

## Approved invalid-game-state recovery (D032)

The user approved these seven invalid-game-state recovery rules:

1. **Stop safely:** when an invalid state is detected—including an ongoing lamb turn with no legal action—block moves, computer actions, hints, undo, and draw offers. Do not pass the turn or award a result.
2. **Explain clearly:** show **“This game cannot continue safely.”** Explain the problem in plain language when known. Offer **Retry Recovery**, **Return Home**, and **Start New Game**. Provide English and reviewed Tamil text; do not expose technical error dumps.
3. **Preserve existing data:** keep the existing save and available move/repetition history. Never overwrite a usable save with a state known to be invalid, silently delete the match, or reset it to the opening position. Returning Home preserves the recovery option through Continue.
4. **Retry without rewriting play:** Retry Recovery may reload and validate saved data or reconstruct the match from confirmed history. It must preserve completed actions and repetition counts. Do not invent missing moves, rewind to an earlier position, or discard history to make validation pass. If the latest confirmed state cannot be established, remain blocked.
5. **Nearby recovery:** notify the connected opponent and pause both phones as soon as the problem is detected or communicated. Recovery requires the same match and opponent, agreement on valid state/history and any completed result, and both players’ readiness before further moves. One phone cannot unilaterally declare the match repaired.
6. **After successful recovery:** show **Resume** for a valid ongoing match; never resume automatically. If recovery establishes an already completed win or draw, show that result instead. If recovery fails, explain that it failed and keep the recovery actions available.
7. **Starting over is explicit:** Start New Game opens the relevant setup flow. Replacing the affected save requires D020 confirmation and occurs only when the new match actually starts. Cancelling setup preserves the old save. The abandoned game receives no win, loss, or draw, and the other mode’s save remains untouched.

The validation, reconstruction, and Nearby reconciliation mechanisms remain architecture work. These rules define the player experience and what recovery is allowed to change; they are not evidence of implemented or tested recovery.

## Proposed release success criteria

- Players can complete legal games with correct outcomes on the selected 23-point board with an approved graph and rules.
- Solo works offline and the difficulty settings show meaningful differences in evaluation.
- Two physical phones can discover, confirm, and complete a Nearby match.
- Disconnect, reconnect, interruption, and saved-state behaviour match the approved specification.
- Theme changes preserve the match state and board readability.
- No app registration is required for Solo or Nearby.

## Not yet decided

Routine theme/layout/sound choices, AI engineering targets and a device/window matrix are recorded in the D039 prerequisite package. Complete evaluation fixtures and measured results, rendered design checks, physical-phone availability, reviewed Tamil translations and difficulty names, monetization, analytics, and release distribution remain pending. D040 settles the app name and application ID. D031 approves Android version/window scope and English/Tamil support with system-language selection and English fallback. The board and game-rules baseline for issue #1 is approved in D017, incorporating D011–D016; the current GitHub repository is private. See [open questions](../open-questions.md).

## First planning milestone

An approved rules/board specification and rough user flows, followed by a bounded Nearby feasibility experiment. App scaffold and harness checks should support these milestones without assuming unresolved product rules.
