> **Latest setup:** See [submission status](submission-status.md) for the signed candidate, package availability, approved free/global distribution, completed SDK/app-ads.txt checks and remaining policy/setup gates.

> **Advertising update, 2026-10-19:** D061 adds Google Mobile Ads/UMP and changes the candidate inputs. The earlier no-ads artifact and counts below are historical. Use [current advertising evidence](ads-integration.md) and [updated privacy review](privacy-and-ads-review.md) for this candidate. The repository is now public with Apache-2.0 code licensing, and the advertising privacy-policy update and AdMob European message were published under D062.

# Google Play production preparation — 2026-10-19

**Historical initial preparation snapshot; production submission remains blocked.** No app record was created, no key generated, and no upload, submission, rollout, commit or push was performed. Existing application changes and local output were preserved. This is preparation for issues #13/#14, not closure of their acceptance criteria.

## Candidate and reproducible evidence

| Item | Observed result |
| --- | --- |
| Source | `main` at `183c0f47ff97f9b54979b1957516b797ee4ec1b0` plus existing dirty files; not a clean/reviewed release revision |
| Identity | `com.maswadkar.lambandtiger`, Lamb and Tiger |
| Current version | code **1**, name **0.1-solo**; kept unchanged. Choose the final name/code after Console creation/library check. |
| Platform | min SDK 29, target 36, compile 37; target 36 meets the current [Play target requirement](https://support.google.com/googleplay/android-developer/answer/11926878). |
| Bundle | **Unsigned**, 0c9dfa593d824762ae233b6ab42098a5a470a63a5533afb10c7784762a0bfc85 SHA-256 |
| Source-input fingerprint | `0be6baaf6d38f9ee57bf79e373aac03fabcd3c3de8de32dcc3578a5fdf7bb997`; complete per-file list retained with the artifact |
| Doctor / build | Passed with configured JDK 25 / SDK; debug APK and release AAB built |
| JVM tests | **47 passed**: game 14, app 25, nearby 8; no failures/errors/skips |
| Release lint | App: 0 errors / 30 warnings. Nearby: 0 errors / 9 warnings. Reports preserved; no suppression or dependency changes made. |
| Bundle inspection | ZIP integrity passed; expected production package/version; no debuggable/testOnly flags; jarsigner reports unsigned. A task called `signReleaseBundle` did not establish signing. |
| Native code | Four ABI copies of `libandroidx.graphics.path.so`; inspected PT_LOAD alignment is 16384 bytes. This does not prove generated-APK ZIP alignment, bundletool validity or 16 KB runtime compatibility. |
| Runtime | This turn's API 36 instrumentation attempt is **inconclusive** due to another active Claude test session on the same emulator/reports. Do not count its overwritten reports as this run's evidence. No physical acceptance performed here. |

The evidence directory is `artifacts/local/play-preparation-20261018T222136.546007Z/` (UTC filename; prepared on **19 October in Europe/Zurich**). It contains the copied AAB, merged manifest, before/after source hashes, command list, logs, unit results, lint reports and `report.json`. Initial investigation and conflicting emulator-run logs are under `artifacts/local/play-preparation-20261019/`. Generated evidence stays ignored by Git.

The first inspection found four pre-existing modified files: FriendLayoutTest.kt, FriendScreen.kt, Screens.kt and FriendViewModel.kt. The other session changed FriendLayoutTest.kt during this work; the successful reproducible preparation captured the later stable input set. Source and device evidence must be refreshed after subsequent changes. No other session was stopped.

The initial command included a non-existent `:app:testReleaseUnitTest` task and stopped before validation. Task discovery corrected this to the actual debug JVM tasks plus release build/lint. That failed invocation is not counted as a passing check.

### Re-run local preparation

With `JAVA_HOME` and `ANDROID_HOME` set per README:

```sh
python3 scripts/prepare_play_release.py
```

The script uses the documented doctor and JVM/debug checks plus release lint/bundle, discovers checkout-specific build paths, copies evidence, and rejects source changes during its run. It returns nonzero on failed commands, missing reports, incomplete JVM results, fatal/error lint, invalid package/development flags or corrupt ZIPs. It never creates keys or submits to Play. It does not certify policy, runtime or signing; `publication_ready` remains false. Do not run it concurrently with another build in the same checkout.

The current release has no R8/minification setting enabled; no mapping was produced. Enabling shrinking is a separate change needing release runtime verification. The script does not invent a release-unit-test task or silently change the version.

Run the documented connected suite only on an exclusively available emulator. The existing CI runner's installed AGP test engine uninstalls its debug/test packages after tests; it is unsuitable for proving an in-place user-data upgrade. Keep physical phones and their saves untouched. An isolated release smoke install and real upgrade test remain separate checks.

## Console findings

Read-only inspection of the supplied developer account found no Lamb and Tiger or `com.maswadkar.lambandtiger` app record. This establishes absence in the inspected account, not global package availability or authority to select another account. Other apps were not modified.

Create the game record only after confirming this is its intended account and resolving the required app-creation declarations. There is currently no game-specific track, upload-key registration, artifact library, production-access screen or Publishing overview to validate. The account predates the newer-personal-account testing policy based on visible historical app dates, but account-specific production requirements must still be checked on the new game; do not impose or waive a tester threshold by assumption. [Google's testing guidance](https://support.google.com/googleplay/android-developer/answer/9845334) describes the applicable routes.

## Publication blockers and next actions

| Gate | Concrete next action |
| --- | --- |
| Families / Bluetooth / SDK | Resolve the current Families CDM requirement against direct Nearby permission requests and verify SDK suitability for children. See the [evidence review](privacy-and-ads-review.md). Do not bypass this by declaring adults-only. |
| Two physical phones | Complete host/join in both directions, matching codes, permissions/denial, legal gameplay, save/reconnect, interruptions, draws/results and rematches on the same final candidate. D047 local verification is not this acceptance. |
| Privacy policy | Completed under D062: [public policy](../../PRIVACY.md), advertising disclosures and Settings link. Keep it aligned with the final distributed binary. |
| Owner setup | Confirm intended developer account, price/countries, initial test audience, signing-key custody and recovery. Support email is supplied; product direction includes children and adults. |
| Signed release | After identity/custody are settled, configure secret-free signing inputs, produce a signed AAB, verify upload certificate, bundletool/package/version/hash and Play App Signing configuration. Do not reuse N-Back's key or debug signing. |
| Candidate freeze / runtime | Reconcile the current edits, rerun full relevant checks on stable source and exclusive devices, inspect installed release screens and 16 KB behavior; preserve exact artifacts. Final merge remains separate. |
| Languages / device qualification | Obtain fluent Tamil review and finish the applicable phone/tablet, orientations, large-text, feedback and physical performance checks. D049's TalkBack exception remains documented; no new spoken test is claimed or required by this packet. |
| Store assets / forms | Finalize listing copy, Play-format icon/feature graphic and actual candidate screenshots; complete Data safety, rating, audience, app access and policy/setup forms with evidence. |
| Distribution | Recommended next distribution step after blockers: an owner-approved internal test before production. This is a proposal, not authorization or a promise that internal testing grants production access. |

Existing lint warnings cover dependency/version suggestions, unused resources, monochrome icon, API/attribute compatibility and a Kotlin convenience suggestion. No lint error was hidden. Review final warnings in context; target-36's “not latest” warning does not contradict current Play eligibility, and automatically upgrading all dependencies is outside this preparation.

## Deliverables

- [Listing, notes, reviewer instructions and asset inventory](store-listing-draft.md)
- [Published privacy policy source and update history](privacy-policy-draft.md)
- [SDK / Families / Data safety / future advertising review](privacy-and-ads-review.md)
- [Reusable skill validation record](skill-validation.md)
- Reusable command: `python3 scripts/prepare_play_release.py`
- Personal reusable skill installed at `~/.codex/skills/google-play-release/SKILL.md`; invoke **`$google-play-release`** in other Android repositories. It discovers each repository's requirements instead of importing this game's commands.

No external submission approval is requested yet: the signing, policy and physical-device prerequisites must first produce a concrete release candidate to approve.
