# Privacy, audience and advertising review

Updated 2026-10-19 after D061. Evidence and proposed answers, not submitted declarations or a legal-compliance certification. Earlier no-ad candidates are superseded by the current integration.

## Current implementation

- Google Mobile Ads 25.5.0 and UMP 4.0.0 are included in `app`; no mediation adapters, billing or custom game analytics added. Nearby remains 19.5.1.
- Debug disables ad requests; adsQa uses Google test IDs; release requires `-Plamb.liveAds=true` to use the owner's verified IDs. SDK presence remains relevant even when serving is disabled.
- All users receive GMA `AgeRestrictedTreatment.CHILD`, maximum content rating G, non-personalized request extras and UMP under-age-of-consent treatment. There is no age/date-of-birth collection. UMP is not parental consent or age verification.
- A successful current UMP status update and `canRequestAds()` are required before initialization/requests. Required forms are shown only from Home/Settings. Settings exposes privacy options when UMP requires them, and always links to the policy. Errors disable ads without preventing play.
- INTERNET is added. AD_ID and Android AdServices ID/attribution/topics permissions are explicitly removed. Backup remains disabled. Inspect the final merged release manifest, not only the source.
- Google's Mobile Ads disclosure describes SDK collection/sharing of IP, product interactions, diagnostics and identifiers. Child treatment, non-personalized ads or absent AD_ID permission do not establish zero data processing. Nearby diagnostics remain a separate review item.

See the [current submission status](submission-status.md) for the confirmed CDM and mixed-age-screen gaps and final ad dismissal qualification.

## Remaining publication gates

1. **Families/Bluetooth:** [Families policy](https://support.google.com/googleplay/android-developer/answer/9893335) requires Companion Device Manager for covered Bluetooth requests. Resolve applicability to the direct Nearby flow and verify a compliant implementation on two physical phones. Do not silently change the audience.
2. **Child-audience SDK eligibility:** Google Ads 25.5.0 is covered by the current self-certified list (AdMob 19.0.0 or later), verified 2026-10-19; review serving restrictions and Nearby's child-audience suitability and diagnostics separately. App-level treatment signals do not certify the whole product.
3. **Public policy:** the game-specific policy is public at https://github.com/lumensparkxy/lamb-and-tiger/blob/main/PRIVACY.md. The advertising disclosure update was published on 2026-10-19 in commit `9e67324`. Preserve Krishi AI's separate policy.
4. **Data safety:** complete the collection/sharing/purpose/encryption/deletion assessment for the actual SDK configuration and applicable exemptions. Local-only saves and peer exchanges need separate treatment. No final “no collection” conclusion has been made.
5. **AdMob:** the app/units exist, but store association, app readiness review, developer-site/app-ads.txt verification and relevant privacy-message/account configuration still need production validation. Test creatives do not prove publisher ad fill or account approval.
6. **Privacy behavior:** all users receive UMP under-age treatment, which suppresses consent requests. The Lamb and Tiger European message is published with its policy URL and decline option. Review confirmed the generic preview's 210-partner/precise-geolocation language comes from shared account configuration, with zero publisher-owned purposes. Shared settings were preserved; this generic adult consent form is not exercised by the current under-age path. Reassess the template and partner scope before any future age-screen/personalized-ads change. See [publication and verification evidence](ads-integration.md). Test-ID success does not establish every jurisdiction or revocation path. Do not force adult treatment merely to obtain a screenshot.

## Proposed Play answers

| Field | Current preparation |
| --- | --- |
| Contains ads | Yes for the explicitly enabled release. Match the actual distributed binary. |
| Advertising ID | The merged release must have no AD_ID permission. Check all SDK behavior and the actual Console question before submitting. |
| App access | No login; Nearby reviewer instructions require two phones. |
| Audience | Inclusive child/adult direction; owner must select suitable age bands. |
| Content rating | Complete IARC accurately; no rating assigned by this work. |
| Data safety | Draft only; SDK data mapping remains unresolved. |
| Privacy URL | Repository policy URL above; advertising update published. |
| Support | maswadkar@gmail.com |
| Price / countries | Owner approved free with ads in all supported countries on 2026-10-19; not yet applied in Play. |

## Official sources checked 2026-10-19

- [Google Mobile Ads setup](https://developers.google.com/admob/android/quick-start)
- [Age and content treatment](https://developers.google.com/admob/android/targeting)
- [UMP setup and request eligibility](https://developers.google.com/admob/android/privacy)
- [Mobile Ads data disclosures](https://developers.google.com/admob/android/privacy/play-data-disclosure)
- [Nearby diagnostics](https://developers.google.com/nearby/connections/overview)
- [Play Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Families policy](https://support.google.com/googleplay/android-developer/answer/9893335)
