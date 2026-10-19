# Google advertising integration — 2026-10-19

D061 authorizes Home banners and ads between completed games. D062 authorizes review, privacy publication and commit/push; Play publication is separate. The repository already became public with Apache-2.0 code licensing under the earlier owner request.

## Account and configuration

Created in the supplied AdMob account, after confirming no Lamb and Tiger record existed:

| Item | Verified value |
| --- | --- |
| App | `ca-app-pub-6317522941728465~9048462765` |
| Home banner | `ca-app-pub-6317522941728465/3353457967` |
| Completed games interstitial | `ca-app-pub-6317522941728465/3137835542` |

These are public SDK identifiers, not credentials. They are stored in `gradle.properties`.

- `debug`: ad requests disabled, `.debug` package.
- `adsQa`: Google's test app and ad-unit IDs, `.adsqa` package. Even with `lamb.liveAds=true`, no publisher ad units are used. Add `-Plamb.qaOwnedApp=true` to check the owned app's UMP configuration while retaining test ad units.
- `release`: ads disabled by default; `-Plamb.liveAds=true` enables owned IDs and rejects missing/malformed/sample IDs. No signing configuration or publication was added.

Use the existing JDK/SDK setup in README:

```sh
./gradlew :app:assembleAdsQa
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedAdsQaAndroidTest \
  -Plamb.testBuildType=adsQa \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maswadkar.lambandtiger.AdsSdkSmokeTest
python3 scripts/prepare_play_release.py --live-ads
```

Run QA instrumentation separately from normal debug unit/device tasks: this AGP selects the unit-test build type using `testBuildType`. The smoke test is opt-in, uses the network, waits for the test creative's accessible Close control and tests its dismissal callback; it is not a deterministic offline CI test. The test runner removes the QA package afterward. Never target a phone containing personal saves.

## Behavior and boundaries

The integration resides only in `app`. The Home banner occupies a separate footer outside the scrolling Home content and is destroyed on departure/background. Banner failure removes its content. The board and active game controls do not contain ads.

A full-screen ad may appear only when Play Again/Switch Sides is selected on a completed Solo/Nearby result. Home, Back, first launch, paused/unfinished play, reconnect and View Board do not trigger it. A missing/stale ad immediately continues the action, without waiting for a load. Requests preload only from Home/completed-game routes. One attempt per completed match per process and a persisted 120-second minimum between displayed interstitials limit frequency. Rotation retains the attempt guard; stale/duplicate SDK callbacks cannot start a new match. Nearby still requires the existing peer rematch agreement.

All requests use GMA CHILD treatment, maximum content rating G and non-personalized extras; UMP separately receives under-age treatment. A successful fresh UMP update and eligibility are required before SDK initialization/requests. Consent forms are restricted to Home/Settings. Settings always links to the policy and exposes privacy choices when required by UMP. No ages, dates of birth, targeting locations or game moves are added to requests. Advertising/AdServices identifier permissions are removed from the merged manifest; SDK data disclosures still apply.

## Privacy publication and remaining account setup

On 2026-10-19, the advertising disclosure update in `PRIVACY.md` was published to the public repository in commit `9e67324`. The message **Lamb and Tiger European privacy choices** was then verified as **Published** in AdMob, assigned only to Lamb and Tiger with that policy URL. English is the default; Consent, Manage options and Do not consent are available, with Do not consent enabled in all regions. The close-X option and fallback ad-unit deployment remain off; the app integrates UMP directly.

The generic preview mentions 210 partners, personalized purposes and precise geolocation. Review found these are derived from shared account partner settings (198 common partners displayed in account settings), with **zero publisher-owned purposes**, consent mode off and special feature 2 off. Those account settings also affect another existing app and were preserved. They are not claims that this build requests those data or purposes: the app requests neither coordinates nor personalized ads and sends UMP under-age treatment for every user. Google's [UMP GDPR documentation](https://developers.google.com/admob/android/privacy/gdpr) says under-age treatment does not request consent. The published generic adult form is therefore not the current runtime path. Any future adult/age-screen or personalized-ad change must review this template and its account-wide vendor scope before activation. See [partner-settings scope](https://support.google.com/admob/answer/10113004?hl=en).

The original owned-app check failed closed with **Publisher misconfiguration: no form(s) configured**; post-publication verification is recorded below. AdMob advises propagation may take up to an hour.

Still finish AdMob store association/readiness, applicable developer-site/app-ads.txt setup, other applicable regional requirements, Play Contains ads/Data safety/audience declarations and the pre-existing Families/Nearby review. No live ads were clicked or requested by QA, no Play app/release was submitted, and no legal agreement was accepted.

## Evidence

Local evidence is under `artifacts/local/ads-integration-20261019/` (ignored by Git): AdMob unit/draft screenshots, the rendered Home test banner and interstitial, SDK logs, source baseline and build/test reports. The original source-only no-ads report is superseded for this candidate. Final check results are recorded below.

Limits: test creatives do not establish owned-account fill or approval, the all-under-age setup does not exercise an adult consent form, and the SDK smoke fixture is a completed-session key rather than a physical Nearby match. Tamil additions need fluent review. Two-phone acceptance, release signing, Play-delivered upgrade and publication remain separate.

### Earlier implementation checks (includes preserved unrelated working edits)

- Source: `9b95babfba6b7f747774666e13b349d4c6f70c7d` plus deliberate local changes; input SHA-256 `1ddd0a30e7b64feeadf55fc719187a7d8ddcc5d2ea4cc9fe88912695623883b5`. Inputs were unchanged during preparation.
- `python3 scripts/prepare_play_release.py --live-ads`: doctor, debug compilation, **54 JVM tests** (app 32, game 14, nearby 8), debug/release lint and unsigned release bundle passed. Release lint: **0 errors, 40 warnings** (31 app, 9 nearby); one new advisory suggests the KTX SharedPreferences helper.
- Release package `com.maswadkar.lambandtiger`, version code 1 / `0.1-solo`, min 29 / target 36. Live-ID configuration verified in generated BuildConfig. No AD_ID or Android AdServices permissions in merged release manifest.
- AAB SHA-256: `8ebc9e53c90d760543c95e49e3f06975ca54663c0cdd05cbba67f2cb78f4a664`. Evidence: `artifacts/local/play-preparation-20261018T225330.103693Z/report.json`. This bundle is **unsigned** and has not been installed or uploaded.
- Real SDK smoke: **1 passed** on API 36 using Google test IDs. Ad display, UI-tree-derived Close tap and exactly-once continuation verified. Earlier Back/accessibility-action attempts did not dismiss the SDK window; the final touch-driven test passed. Reports retained in `artifacts/local/ads-integration-20261019/sdk-passed/`.
- Owned-app UMP: failed closed with missing-message error, retained in `owned-app-missing-message/`; this historical failure predates the published configuration.
- Personal Google Play release skill structural validation passed again.
- Rendered Settings verified on API 36: policy link visible, no banner on Settings. Tapping it opened an ACTION_VIEW intent to the exact public repository policy URL.

- Focused debug API-36 regression: **6 passed** — complete Solo games as both sides plus Switch Sides, Settings preserving the match, Solo portrait/landscape result overlays, and Nearby portrait/landscape result layouts. Together with the separate SDK smoke test, **7 focused emulator tests passed**. These are not physical radio tests. The runner accepted only the first method in a comma-separated filter, so remaining checks were run independently and their reports snapshotted as `layout-*-results/`.

### Scoped publication candidate checks

To preserve pre-existing Nearby and hint-accessibility edits, staged ads/release changes were applied to an isolated checkout based on policy commit `9e67324`. That exact build input set passed `prepare_play_release.py --live-ads`: doctor, debug build, **54 JVM tests**, debug/release lint (**0 errors, 40 release warnings**) and unsigned release bundle. No advertising-ID/AdServices permissions appeared in the merged manifest.

- Source-input SHA-256: `48697c325e45ba7096e436183d77d23b20ac5df91ea8b366279cb815cac2b15f`.
- Unsigned AAB SHA-256: `eebceb085ca0969169dacfa0c89e81677b27fee3b0924f6474227fd493d3e27e`.
- Snapshot: `artifacts/local/ads-integration-20261019/scoped-candidate/report.json`. Earlier artifact hashes above include unrelated working edits and are not this scoped candidate. Documentation updates after this run do not change build inputs.
- Public GitHub policy content was fetched and byte-compared with the reviewed local policy. AdMob's saved message status was verified **Published**, with screenshot `admob-published.png`.
- Personal reusable release skill structural validation passed again.
- Post-publication owned-app UMP + SDK smoke: **1 passed** on API 36 (`emulator-5554`) using the owned AdMob app ID with Google's test banner/interstitial units. The earlier missing-message failure is resolved for the current all-under-age path. The real test interstitial displayed, its accessible Close control was tapped and continuation ran exactly once. No adult consent form or live publisher ad fill was exercised. Reports: `owned-app-published-passed/`; command log: `owned-app-published-sdk.log`.
