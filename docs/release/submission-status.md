# Current Google Play submission status — 2026-10-19

**Closed Alpha submitted at approximately 18:18 UTC.** At that observation, Console showed **Changes in review** for **0.1-solo (1) — Closed Alpha**, with quick checks still running before review could proceed. This record does not establish the current live review status. This is not approval or download availability. See the [signed candidate and submission receipt](alpha-submission-20261019.md).

The submitted Alpha source `674411874bfec2b2e494735e15b61b28e4253482` was rebuilt with live-ad configuration and verified upload signing. AAB SHA-256: `9d127336e1b8deab1fb4976225bc9e8eda3308591d028bcadead05ac70fd23ff`. Doctor, build, 57 JVM tests, lint (0 errors / 33 warnings), bundle/APK signature checks, bundletool validation, APK ZIP/ELF alignment and a signed-release emulator smoke check passed. Earlier revision-matched evidence records 8 emulator tests.

The later UI fixes merged in [PR #28](https://github.com/lumensparkxy/lamb-and-tiger/pull/28), merge commit `40cae01b82a0d4712bb5bc5949f829938f11e66d`, are not included in this version 1 AAB. They improve Start Game visibility, board-piece sizing and the landscape board layout. Delivering them through Play requires a new release candidate and submission.

All initial Console setup, all six audience bands, owner-authorized child-law certification, Data safety and English listing are included. Closed Alpha uses the owner-supplied two-account list and all 177 named countries/regions plus Rest of World. PC is opted out per owner instruction. Advertising ID use is declared No, consistent with the verified configuration. Managed publishing is off; no production release was submitted.

Remaining steps:

1. Await automated checks and Google review, then verify tester opt-in access, download, installed version and Play signing identity.
2. Complete independent upload-key/password backup and recovery verification. The owner explicitly deferred this until after the Alpha submission (D068).
3. Perform final two-physical-phone Bluetooth testing from Play downloads as planned by the owner: host/join, code rejection/confirmation, moves, saved-peer reconnect, interruptions and rematch. This remains planned, not observed.
4. Retain representative ad-dismissal qualification and fluent Tamil review. Test creatives do not certify every served creative.
5. Complete AdMob store association/readiness and developer-site/app-ads.txt checks. The privacy message is published; all ages retain restricted advertising settings.

The live Console offers whole-app compliance or a neutral age screen; the earlier absolute age-screen-blocker assessment was corrected in D067. Owner certification and submitted declarations are not Google approval.

## Earlier signing/setup evidence (before D064)

- Created a dedicated RSA-4096 upload key outside Git; configured opt-in environment-based signing and certificate checking. See [signing and backup handoff](signing.md). No existing app key was reused; Play had not registered this key at that earlier stage.
- Built a signed ad-enabled AAB from isolated `4e933cd` plus only the signing/helper changes, preserving unrelated local Nearby/accessibility edits. Source fingerprint: `ee9141c12404dc45103f257ea7aeb74b81b1906f874b117b7c2cae561cd448f3`.
- Signed AAB SHA-256: `e68a4af91b622eb63946b2f6425cb4231af89d0d4c9be0afa555b10973215faa`. That candidate used version code 1 / `0.1-solo`; no Play library record existed at that stage to confirm release-version availability.
- Doctor, debug/release builds, **54 JVM tests**, release lint **0 errors / 40 warnings**, AAB signature and expected upload-certificate verification passed.
- Google bundletool **1.18.3** `validate` and signed universal-APK generation passed. Download SHA-256 matched the official GitHub release asset digest. APK signature verification and `zipalign -c -P 16 -v 4` passed. This is APK ZIP alignment, not a 16-KB-page runtime or Play-delivered build test. No live-ad APK was installed or requested for QA.
- `https://maswadkar.com/app-ads.txt` returned `google.com, pub-6317522941728465, DIRECT, f08c47fec0942fa0`, matching the owned AdMob app publisher. No website change is needed for this entry. Store developer-website association and AdMob crawler verification remain pending.
- Verified Google Mobile Ads 25.5.0 is covered by the current Families self-certified listing (AdMob 19.0.0 or later). No mediation adapters are included. This closes the SDK-version-list check, not whole-app policy review.
- Prepared a [Data safety worksheet](data-safety-draft.md) with SDK collection, sharing, purposes and unresolved exemptions.

Evidence stays in ignored `artifacts/local/play-setup-20261019/`, including the exact signed candidate, build/test reports, bundletool logs, APK checks and staged Console screenshot. The first build failed on a Gradle Kotlin DSL namespace reference; it was fixed before the successful candidate. That failed run is not passing evidence.
