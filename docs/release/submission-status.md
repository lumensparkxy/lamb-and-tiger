# Current Google Play submission status — 2026-10-19

Latest assessment: [delivery and Play readiness](readiness-20261019-delivery.md)
records the current source fingerprint, fresh 57-test JVM and 8-test emulator
results, release build/lint, unsigned bundle hash and live Console setup gaps.

The user-created [Lamb and Tiger app record](https://play.google.com/console/u/0/developers/6594351209748992514/app/4972975891791973201/app-dashboard) was verified on the dashboard. Initial setup tasks are incomplete. No upload, track release, review submission or rollout was performed in this pass.

D064 implements CDM device selection with secure Bluetooth transport. D065 removes the age-group screen at the owner’s explicit request; all-user restricted ad settings remain. **Mixed-audience age screening is an open Play submission blocker again.** See [age-screen removal verification](age-screen-removal.md) for the current debug candidate and [Families fix verification](families-fix-evidence.md) for earlier transport and signed-build evidence. Existing signed bundles predate D065 and must not be uploaded as the current release.

Remaining steps:

1. Resolve mixed-audience age screening before Play submission. The current app has no age screen by D065; all-user child treatment alone is not a documented exemption from the current Families requirement. Do not change the audience or remove ads without an explicit product decision.

2. Verify host/join, matching-code rejection, mutual confirmation, saved-opponent reconnect, interruption and rematch on two physical phones. Android CDM availability, system pairing and actual radio behavior cannot be certified by emulator or JVM tests. Both phones need the new transport version.
3. AdMob high-engagement ads were disabled and the persisted Off state verified for Lamb and Tiger. Google documents up-to-five-second interstitial skip time with this setting off. Retain representative ad-dismissal qualification; a test creative does not certify every served creative. Complete fluent Tamil review, including the new pairing strings.
4. Complete Play listing, audience/IARC, app access, Data safety and approved all-supported-country setup. Price is owner-approved free with ads. Review account-specific production/testing access on the live Console; app creation alone does not grant production access.
5. Complete key vault backup/recovery, Play App Signing enrollment, final signed binary/version checks and representative installed/Play-delivered verification. The dedicated key exists outside Git; owner backup remains open.
6. Complete AdMob store association, readiness and developer-site/app-ads.txt checks. The privacy message is already published; all ages retain the restricted ad configuration.

The local policy and Data safety worksheet now describe CDM/Bluetooth and the removal of age collection. Final Console declarations remain unsubmitted. No claim of whole-app Families compliance or production readiness is made.

## Earlier signing/setup evidence (before D064)

- Created a dedicated RSA-4096 upload key outside Git; configured opt-in environment-based signing and certificate checking. See [signing and backup handoff](signing.md). No existing app key was reused; Play has not registered this key yet.
- Built a signed ad-enabled AAB from isolated `4e933cd` plus only the signing/helper changes, preserving unrelated local Nearby/accessibility edits. Source fingerprint: `ee9141c12404dc45103f257ea7aeb74b81b1906f874b117b7c2cae561cd448f3`.
- Signed AAB SHA-256: `e68a4af91b622eb63946b2f6425cb4231af89d0d4c9be0afa555b10973215faa`. Version remains code 1 / `0.1-solo`; no Play library record exists to confirm final release-version availability.
- Doctor, debug/release builds, **54 JVM tests**, release lint **0 errors / 40 warnings**, AAB signature and expected upload-certificate verification passed.
- Google bundletool **1.18.3** `validate` and signed universal-APK generation passed. Download SHA-256 matched the official GitHub release asset digest. APK signature verification and `zipalign -c -P 16 -v 4` passed. This is APK ZIP alignment, not a 16-KB-page runtime or Play-delivered build test. No live-ad APK was installed or requested for QA.
- `https://maswadkar.com/app-ads.txt` returned `google.com, pub-6317522941728465, DIRECT, f08c47fec0942fa0`, matching the owned AdMob app publisher. No website change is needed for this entry. Store developer-website association and AdMob crawler verification remain pending.
- Verified Google Mobile Ads 25.5.0 is covered by the current Families self-certified listing (AdMob 19.0.0 or later). No mediation adapters are included. This closes the SDK-version-list check, not whole-app policy review.
- Prepared a [Data safety worksheet](data-safety-draft.md) with SDK collection, sharing, purposes and unresolved exemptions.

Evidence stays in ignored `artifacts/local/play-setup-20261019/`, including the exact signed candidate, build/test reports, bundletool logs, APK checks and staged Console screenshot. The first build failed on a Gradle Kotlin DSL namespace reference; it was fixed before the successful candidate. That failed run is not passing evidence.
