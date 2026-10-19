# Delivery and Play readiness — 19 October 2026

**Later setup update:** [Console setup evidence](console-setup-20261019.md) supersedes the Console gaps and categorical age-screen assessment below. All initial setup tasks are saved, including owner-certified all-age targeting, Data safety and the listing. The subsequent [closed Alpha submission receipt](alpha-submission-20261019.md) records the version 1 upload and review submission. The owner plans physical verification from Play downloads. The engineering evidence and assessment scope below describe the earlier stage and are unchanged.

**Local engineering checks pass; Google Play production submission is not ready.**
The owner authorized commit, push, PR, merge and branch cleanup, followed by a
readiness assessment. No Play upload, declaration submission or rollout is part
of this assessment.

## Delivered behavior and candidate

Nearby keeps its last settled position in the ViewModel while a transaction is
unsettled or a connection is lost. Starting another connection clears that
fallback. The pairing-screen choice survives recreation. A disconnected game
without a save returns to Host/Join rather than attempting an impossible resume;
the completed-game dialog follows the same rule. Solo hint semantics name the
hinted action/points instead of relying only on the visual ring and arrow.
Game rules and protocol semantics are unchanged.

The tested source is base `6b716001f089d126d080f6fc8c2ae85120d3510e` plus the four
code/test files in this delivery. Its complete build-input SHA-256 is
`3e874e1949db9bc220587f1c205f346127d20cbf5d94a1af3e6acb63a1581ad6`.
The report records dirty inputs explicitly; documentation added afterward does
not change that fingerprint. Retained source manifests permit comparison with
the final merged revision. Existing `output/`, `.playwright-mcp/` and unrelated
detached worktrees were excluded from the commit.

## Fresh verification

- `python3 scripts/prepare_play_release.py --live-ads` passed with the README JDK
  25 / SDK configuration. This ran doctor, debug assembly, the three JVM suites,
  app debug/release lint, Nearby release lint and release bundle assembly.
- **57 JVM tests passed**: app 32, game 14, Nearby 11; no failures, errors or skips.
- Release lint: **0 errors, 33 warnings** (app 31, Nearby 2). This is not a
  zero-warning build; the retained XML reports contain the findings.
- `:app:assembleDebugAndroidTest` passed. Debug/test APKs were installed with
  `adb -s emulator-5554 install -r`, then `am instrument -w -r -e class
  com.maswadkar.lambandtiger.FriendLayoutTest` ran **8/8 passing tests**. No clear
  or uninstall was used. The focused fixture suite does not replace either save.
  No active instrumentation/build owner was found before this run.
- Device: API 36 ARM64 emulator, build
  `google/sdk_gphone64_arm64/emu64a:16/BE2A.250530.026.D1/13818094:user/release-keys`.
  Tests cover retained geometry, blocked input, no-save disconnect, pairing,
  readiness, draws and portrait/landscape results. Captured disconnected-board
  and landscape-result screens were visually inspected. These are debug fixture
  screenshots, not release store assets or physical Bluetooth proof.
- Google bundletool 1.18.3 validation passed. Its Java 25 protobuf Unsafe
  deprecation warning remains in the log.

Local reports, command exits, source manifests and the AAB are retained in
`artifacts/local/play-preparation-20261019T160826.582007Z/`; instrumentation,
screenshots and bundletool output are in `artifacts/local/delivery-20261019/`.
These generated files stay outside Git.

## Release artifact inspection

| Item | Result |
| --- | --- |
| Package/version | `com.maswadkar.lambandtiger`, code 1, name `0.1-solo` |
| SDK | min 29 / target 36 / compile 37 |
| AAB SHA-256 | `1872c274cc8c0ee29bd4868df59d59c027c7d740a6baa6e2f06d59c1ba35a334` |
| Signing | **Unsigned**; no signing credential used; not uploadable |
| Ads | Owned IDs enabled in the inspected AAB; no live-ad installation/request performed |
| Manifest | No debug/test flags; backup disabled; no location, scan, Wi-Fi, AD_ID or AdServices permissions |
| Exported components | Launcher activity; WorkManager job service protected by BIND_JOB_SERVICE; diagnostics/profile receivers protected by DUMP |
| Native libraries | Four ABI copies of `libandroidx.graphics.path.so`; no new 16 KB runtime or APK alignment certification |

No installed release, production upgrade, Play-delivered build or physical-phone
qualification was performed. Earlier signed artifacts predate the current source
and must not be substituted. The dedicated signing identity and outstanding vault
backup remain documented in [signing](signing.md).

## Live Console and policy

Read-only inspection of the intended account's [Lamb and Tiger dashboard](https://play.google.com/console/u/0/developers/6594351209748992514/app/4972975891791973201/app-dashboard)
found **0 of 11 initial setup tasks complete**. The [publishing overview](https://play.google.com/console/u/0/developers/6594351209748992514/app/4972975891791973201/publishing)
has **Send app for review disabled**, requiring dashboard setup. It lists pending
default English listing and Google Play Games on PC opt-in changes. The PC form
factor needs owner review against the approved Android phone/tablet scope; no
setting was changed here. The dashboard offers internal testing for up to 100
testers, but this does not establish production access or authorize distribution.

The current [Families full policy, Ads SDKs](https://support.google.com/googleplay/android-developer/answer/9893335)
requires age-screening measures for ad-supported mixed child/older audiences.
D065 deliberately removed the age screen. Restricted ads for everyone do not
resolve that documented requirement. Preserve the approved audience and ad
choices until the owner resolves this product/policy conflict.

The current [Mobile Ads 25.5.0 disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure)
still documents IP-derived location, interactions, diagnostics and identifiers.
The [Data safety worksheet](data-safety-draft.md) remains a draft; removing AD_ID
does not justify declaring no collection. [Target API guidance](https://support.google.com/googleplay/android-developer/answer/11926878)
was rechecked; target 36 is not the identified submission blocker.

## Remaining gates and next setup step

1. **Owner decision:** resolve mixed-audience advertising age screening. Do not
   silently restore a screen, remove ads or narrow the audience.
2. **Physical verification:** qualify CDM/secure Bluetooth on two physical phones
   in both roles, including rejection, confirmation, moves, reconnect,
   interruptions, results and rematches. Only one physical phone is connected.
3. **Materials and declarations:** finish the 11 Console tasks, IARC/audience and
   Data safety; review the PC opt-in; apply the already-approved free/global
   distribution. [Exact proposed copy and asset inventory](store-listing-draft.md)
   remain drafts: final icon/feature exports and candidate phone/tablet screenshots
   are not certified. Obtain fluent Tamil review and finish ad-experience/device
   qualification. D049's TalkBack exception is preserved; no spoken pass claimed.
4. **Release engineering and custody:** complete key backup/recovery and Play App
   Signing enrollment, freeze the version, build/sign the final source, and verify
   representative installed release, upgrade and Play-delivered behavior.

The next setup step is the age-screening decision alongside arranging a second
physical phone. Engineering/Console preparation can continue, but this report
does not authorize publication or close issues #8, #11, #13 or #14.
