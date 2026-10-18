# Play Friend Compact Tabletop adaptation

Date: 2026-10-18. Scope: D058 local native UI adaptation to the approved D056/D057 Solo design.

## Changes

- Nearby owns its compact Back / Lamb and Tiger / Menu header, avoiding the previous duplicate header. Host/Join use matching cards; side selection uses blue lamb/orange tiger icons, a selected outline and checkmark, and stacked cards at large text sizes.
- Matching-code comparison, explicit opponent confirmation/rejection, saved-game replacement confirmation, opposite-side selection and both players' readiness retain the existing coordinator commands.
- The game uses the original upright board coordinates, all 23 points and 38 connections, without visible A–W labels. Compact player identities, a stable turn banner and shared reserve/capture counts follow Solo. Pause/Resume replaces Solo-only Hint/Undo.
- Offer Draw, Rules, Settings, Save & Return Home and Disconnect are available in Menu. Opening Menu does not pause. The existing point-selection alternative remains available as a dialog through Menu.
- Result and connection-error dialogs overlay the board. Results can be dismissed and reopened; Play Again/Switch Sides still send existing rematch requests. Disconnect remains visible after error dismissal, with an explicit reconnect action. Pending draw offers remain reviewable after dismissal and require an explicit response.
- During an unsettled transaction, the coordinator may temporarily publish no match. The UI retains only the last confirmed position while showing Confirming move and blocking input. It also retains that position if the pending transaction disconnects. It clears the display cache for a new connection or setup. No unconfirmed candidate is rendered, and no protocol/storage/engine code changed.
- Both themes, original board motion, existing landscape fallback and large-text scrolling remain supported. New copy has English and draft Tamil resources; fluent Tamil approval is separate.

## Verification

- `scripts/doctor.sh` with the documented Android Studio JBR and Android SDK paths: passed.
- `:game:test`, `:app:testDebugUnitTest`, `:nearby:testDebugUnitTest`: 47 JVM tests, zero failures (14 game, 25 app, 8 Nearby).
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:lintDebug`, `:nearby:lintDebug`: passed. App lint reports zero errors and 29 warnings, including existing toolchain/target/icon suggestions and unused resources retained from earlier layouts.
- API 36 emulator, `emulator-5554`, 1080 × 2424, density 420, font scale 1.0: seven `FriendLayoutTest` cases and three existing `BoardAnimationTest` cases passed. The seven Friend tests were rerun after the final pending-transaction display fix.
- Narrow portrait 840 × 1600 at density 420 (320 dp wide), system font scale 2.0: five focused Friend tests passed for pairing/save/code confirmation, opposite-side readiness, results, pause/resume and draw offers. Cards stack and essential controls remain reachable through scrolling. Normal size/font settings restored afterward.
- Rendered light/dark match screens, Host/Join, side selection, code confirmation, disconnect, draw offer, portrait/landscape result dialogs and enlarged-text screens were inspected. Tests assert stationary board bounds across blocked states, a withheld pending-move snapshot, connection loss, error dismissal and results. They also assert no board input in blocked states and explicit confirmation before sending a draw offer.

Instrumentation renders the production composables with isolated `FriendUi` states and callback spies. It does not create fake saved games, start radio discovery, or write to either match slot. Screenshots of connected matches are UI fixtures, not evidence of a live Nearby session.

Local ignored evidence: `work/friend-tabletop/` contains build/doctor logs, `instrumentation.log`, `final-instrumentation.log`, `large-text.log`, normal screenshots and `large-text/` screenshots. Installed the debug build on the emulator only. No physical-phone install, two-phone acceptance, commit, merge or publication was performed.

The next connectivity acceptance step remains testing the approved protocol on two physical Android phones; this UI adaptation does not establish that acceptance.

## Authorized Pixel 9a installation

In a follow-up request on 2026-10-18, the user explicitly authorized deploying this version to the Pixel 9a. Rechecked `:app:assembleDebug`, installed `com.maswadkar.lambandtiger.debug` with `adb install -r` (no uninstall or data clear), and launched `MainActivity` successfully (`Status: ok`). The installed APK SHA-256 matches the local tested APK: `57e246c6b4773e0d970b71528f873cec9166bf04033739b4d23f98a7f055ae56`. Captured `work/friend-tabletop/pixel-9a-installed.png` for launch verification. This installation supersedes the earlier emulator-only installation boundary above; it does not constitute two-phone Nearby acceptance or release publication.
