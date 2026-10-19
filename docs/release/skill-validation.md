# Google Play skill validation — 2026-10-19

Installed personal skill: `~/.codex/skills/google-play-release/`. The portable workflow was informed by the existing repository-specific N-Back release skill; that original skill and its project commands were not changed. The new skill contains SKILL.md, two focused references and agents/openai.yaml. It is normally discoverable, not explicit-only.

## Checks performed

- Skill Creator `quick_validate.py`: **passed** (frontmatter, name, required structure, no unfinished scaffold).
- Reference review: both linked files exist and separate Console actions from materials review; official source links are included for execution-time refresh.
- Portability review: no fixed app package, account ID, device serial, target SDK, repository path, test command, signing identity or N-Back-only requirement in the skill.
- Real application: used the workflow for Lamb and Tiger's release packet, source/artifact fingerprinting, actual build tasks, unsigned bundle inspection, Console inventory, SDK/privacy review and explicit unresolved gates.
- Repository helper: `python3 scripts/prepare_play_release.py` completed successfully against this checkout and retained 47 passing JVM tests, lint and the AAB with matching pre/post source inputs. Python compilation passed. Missing required environment is rejected before building.

## Behavior review against concrete cases

| Case | Reviewed skill behavior |
| --- | --- |
| “Prepare production” with no signing identity/app record | Finish local bundle/materials and report missing setup; do not create keys, accept terms or publish. Observed in this run. |
| No INTERNET permission but Nearby Play services SDK | Review SDK diagnostics; do not automatically declare no data collection. Observed in this run. |
| Owner wants ads eventually, current build has no ads | Describe present behavior, add future-change preparation, defer ad SDK/configuration and update declarations when it ships. Included in the packet. |
| Owner supplies another app's privacy URL | Read the destination, identify mismatch, draft the correct policy and preserve the other app's notice. Observed with Krishi AI. |
| Child/adult audience and direct Bluetooth permissions | Surface the current Families/CDM finding; do not silently change audience or remove the feature. Included in the blocker list. |
| Another agent uses the same emulator/build reports | Detect contention, leave its process alone, do not call overwritten/stale results passing evidence. Added a specific shared-device/report instruction from this failure. |
| Promote an already validated bundle | Reference says reuse the library artifact rather than rebuild/version-bump. Reviewed statically; no live promotion performed. |
| Ambiguous upload timeout / unrelated pending changes | Inspect Console state before retry; do not duplicate submission or include unrelated changes. Reviewed statically; no live upload performed. |

This is structural validation, direct application and a scenario review by the authoring agent. It is **not** an independent-agent evaluation, a test of every Console branch, or evidence that a production release was published successfully. No Play submission was performed to test the skill. The later D062 privacy publication is documented separately in the advertising evidence.

## Advertising follow-up

Revalidated the personal skill structurally after D061: passed. The repository preparation helper now accepts `--live-ads`, records the requested/actual serving configuration and rejects advertising/AdServices permissions in the merged child-treatment release manifest. SDK test IDs and owned-account UMP readiness are tracked separately; an actual missing-message error is retained instead of reporting the test creatives as production readiness.

The reusable materials reference now includes three distinct ad evidence levels (fake behavior, real SDK test creatives, owned-account readiness), separate UMP/ad age signals, compiled QA isolation, stale-callback/dismissal checks, and draft/account-wide privacy-message review. This addition contains no Lamb and Tiger account IDs, package, test device or repository-specific commands. Structural validation passed after the edit.

## D064 follow-up

Updated the reusable materials reference for actual CDM endpoint binding, authorization for radio changes, association/lifecycle cleanup, physical-device evidence, neutral age selection and unknown/corrupt/shared-device behavior. Retained separate UMP and GMA treatment checks and the distinction between a test creative and production dismissal-time qualification. Skill Creator structural validation passed. This is a focused author review and direct application, not an independent-agent evaluation or a production submission test.
