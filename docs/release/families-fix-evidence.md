# D064 Families-related fixes — 19 October 2026

> Historical candidate: D065 subsequently removes the age screen and age storage at the owner’s request. Its age-screen evidence and signed AAB do not describe the current source. See [current submission status](submission-status.md).

Implementation and local evidence; not a Play policy certification or two-phone acceptance.

## Candidate

Base `e445d0f2843d71a726e4c6b7003eb358a6cd599b` plus the scoped D064 patch, built in an isolated checkout. Pre-existing local Nearby layout/recovery and accessibility edits were preserved and excluded from the signed candidate. Candidate source fingerprint (release helper): `af66921eb7bb6119b61ba13fc805f37440349a8f827612ed74e2e5927dd09f13`. Staged source files were byte-compared against that isolated checkout.

Signed AAB SHA-256: `fb77badd27281529306bf63a6675af1bf3211215d0ce91106fd480d75a9df61f`. Package `com.maswadkar.lambandtiger`, version code 1 / `0.1-solo`, min SDK 29 / target 36. Verified dedicated upload certificate: `9D:67:4E:60:86:D7:49:F6:E5:F3:DE:5F:52:8D:04:FA:3E:0A:84:7A:15:1E:7C:BD:BC:A9:A4:96:27:1D:EB:FD`. Live IDs are enabled only in this retained release bundle; it was not installed or uploaded.

## Changes

- Join uses Android CDM with the app’s service UUID filter. The chosen address must belong to a current CDM association at connection time. Host uses Android’s discoverability prompt; secure RFCOMM sockets provide the one-peer link. No insecure socket fallback, app-level discovery or device renaming.
- Bounded frames/queue, generation guards, stage timeouts and socket closure protect cancellation and stale work. Both users confirm a fresh shared session checksum before app payload delivery. Android bonding supplies transport authentication; the checksum is not a new cryptographic protocol. Existing saved-opponent HMAC continuity, game rules, durable ordering, reconnect and rematch remain in `app`/`game` unchanged.
- App-owned CDM associations are removed on leaving Play Friend and stale grants cleared on a new Join request. Android bonds are separate and can be removed in system Bluetooth settings. Host discoverability expires after at most two minutes.
- Google Nearby and its obsolete dependencies are removed. Final merged manifest has no location, BLUETOOTH_SCAN, NEARBY_WIFI_DEVICES, Wi-Fi state/change, AD_ID or AdServices permissions. Old BLUETOOTH/ADMIN are capped at API 30; CONNECT/ADVERTISE are requested only during setup, with no ADVERTISE request on Join.
- Neutral age-group screen has no preset, no date of birth and no age-based reward. The answer is stored locally, with Change player in Settings. Unknown/corrupt answers keep the gate closed. All groups retain CHILD/G/non-personalized ads and UMP under-age treatment; no adult ad path was introduced. Stale consent callbacks cannot restore eligibility after resetting the audience.
- Privacy policy, Data safety worksheet, listing/reviewer instructions and protocol documentation now describe Bluetooth-only play and local age groups. Tamil additions remain drafts pending fluent review.

## Verification

- Doctor passed. Debug, adsQa and release builds passed. Isolated signed preparation passed with **57 JVM tests** (app 32, game 14, nearby 11), no failures/errors. New tests exercise mutual confirmation, matching checksums, pre-confirmation payload rejection, duplicate/early frames, bounded allocation/truncation and the Android-version permission matrix.
- Release lint: app **0 errors / 33 warnings**, nearby **0 errors / 2 warnings**. Warnings remain; this is not a zero-warning build. Signed bundle verification and expected certificate check passed. Google bundletool 1.18.3 validation passed. No installed release, upgrade, 16-KB runtime or Play-delivered verification is claimed.
- API 36 Pixel9a emulator (`emulator-5554`): **35/35** full debug instrumentation tests passed in the working checkout, including the preserved local changes. On the isolated candidate, **7/7** existing Friend layout tests and **2/2** neutral-age tests passed (no preset, disabled Continue, persistence, child/adult selection, player reset and unknown stored group).
- Rendered age screen inspected. Join displayed the Android CDM picker; Cancel returned to the retry explanation. The emulator picker had no peer; no discovery success, bond, radio connection or reconnect is inferred. Screenshots are local evidence, not Play listing uploads. The picker and cancellation checks were repeated on the isolated candidate with the final result-stage guards.
- Owned-app UMP with **Google test ad units** passed the focused SDK smoke test (**1/1**): unknown audience ineligible, child selection, test interstitial load/show/Close and exactly one continuation. This does not prove live fill, every jurisdiction, or every creative’s dismissal time.
- One initial lint run failed for missing Tamil strings and one subsequent build exposed stale dependency locks; both were fixed, then the resolved graphs and fresh isolated build passed. An accidentally unfiltered adsQa run included debug-only Compose fixture tests without their fixture Activity and was stopped; its failure is retained. The intended single-class SDK smoke subsequently passed. No failed run is counted as passing evidence.

## Console and ad configuration

The manually created [Play app dashboard](https://play.google.com/console/u/0/developers/6594351209748992514/app/4972975891791973201/app-dashboard) was verified; setup is incomplete. No Play form, signing enrollment, upload, review submission or rollout was performed.

AdMob’s Lamb and Tiger **High-engagement ads** setting was On. It was saved Off, reopened, and the persisted Off value verified. Only this app’s setting changed. Google documents interstitial skip time up to five seconds when disabled (up to twelve seconds enabled), with up to two exit steps; this setting does not govern partner-bidding units. The app uses standard units without mediation. App readiness still shows Requires review / limited serving pending store association.

## Remaining acceptance

Use two physical phones running this candidate: Host/Join in both directions, OS selection and numeric pairing, reject mismatched codes, mutual confirmation, actual moves, interruption/background/process death, saved-opponent identity and state agreement, draw/result and rematch. Also cover API 29–32 legacy CDM result handling and API 33+ association results. The single connected physical phone was not modified during this pass. The former Google Nearby phone/emulator evidence does not qualify this replacement.

Complete fluent Tamil review, final ad experience checks, key backup/recovery, Play listing/audience/IARC/Data safety/app-access/countries, signing enrollment and account-specific testing/production access. See [current submission status](submission-status.md).

Evidence is retained under ignored `artifacts/local/families-20261019/` (source patch/backup, signed candidate and reports, dependency logs, UI/test results, SDK logs and screenshots). No keys, passwords, device logs or binaries are committed.

Sources checked: [Families](https://support.google.com/googleplay/android-developer/answer/9893335), [CDM](https://developer.android.com/develop/connectivity/bluetooth/companion-device-pairing), [secure Bluetooth sockets](https://developer.android.com/develop/connectivity/bluetooth/connect-bluetooth-devices), [ad experience setting](https://support.google.com/admob/answer/15525707).
