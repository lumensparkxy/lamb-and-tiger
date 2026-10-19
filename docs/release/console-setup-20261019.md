# Console setup — 19 October 2026

**Later update:** [Closed Alpha submission](alpha-submission-20261019.md) records the signed version 1 upload, PC opt-out, advertising ID declaration and review submission. The setup-only boundaries below describe the earlier stage.

Owner-authorized setup for Lamb and Tiger, package `com.maswadkar.lambandtiger`, in the intended Play account. The source at this setup stage was merged revision `674411874bfec2b2e494735e15b61b28e4253482`; no app code changed in that pass. [Engineering evidence](readiness-20261019-delivery.md) remains revision-bound.

## Verified saved state

The [dashboard](https://play.google.com/console/u/0/developers/6594351209748992514/app/4972975891791973201/app-dashboard) initial setup checklist is **complete**: after the final listing save, it disappeared and closed-testing tasks became available. The following eight items were saved first:

- Privacy: public game-specific GitHub `PRIVACY.md` URL.
- App access: no account/payment; instructions explain Solo and the two-phone Bluetooth/CDM requirement. The hardware-dependent access option was selected because Play explicitly includes actions on another device.
- Ads: contains ads.
- Content rating: Game questionnaire saved after owner-authorized IARC terms acceptance. Abstract captures remove board pieces without depicted injury; no chat, purchases, location sharing or other rated content. Computed ratings include ESRB Everyone, PEGI 3 and regional general/all-age ratings.
- Government apps: No.
- Financial features: none.
- Health features: none.
- Category/contact: Game / Board; support maswadkar@gmail.com. Website and phone left blank.

## Final audience, Data safety and listing saves

Target audience: all six owner-approved bands (5 and under, 6–8, 9–12, 13–15, 16–17, 18+) saved after explicit owner confirmation of the child-law certification including COPPA/GDPR. The Ads step was answered Yes: all ads suitable for children and from a certified network, matching all-user CHILD/G treatment and the verified AdMob SDK listing. Optional Teacher Approved enrollment was declined.

Data safety: complete questionnaire saved after audience completion. Discloses approximate location, app interactions, diagnostics and device/other IDs, each collected/shared for advertising, analytics and fraud prevention/security/compliance. No ephemeral processing or global opt-out claimed; encryption in transit Yes; no accounts, external account login or deletion-request mechanism claimed. Optional Families promotional badge not selected. See [worksheet](data-safety-draft.md).

Default English listing: exact name, short and full text from [listing copy](store-listing-draft.md), 512-square icon, 1024×500 feature graphic and two 1080×1920 phone screenshots saved. Publishing overview confirms all required listing information is provided. Optional tablet/PC/desktop/XR/video assets are empty. Text is 76/80 and 1227/4000 characters. Saved for review, not published.

AI labels: icon and feature graphic labeled as created/edited using AI in the new asset declaration. Phone screenshots are raw emulator captures, not AI-generated or edited screenshots. [Google’s asset-declaration guidance](https://support.google.com/googleplay/android-developer/answer/17262077) was checked.

### Asset provenance

Files are in ignored `artifacts/local/console-setup-20261019/store-assets/`:

| Asset | SHA-256 |
| --- | --- |
| `icon-512.png` | `0619d5d64a43f2eca3ae0bd4ad627961074ed2ee3844c706f6f008038d101cda` |
| `feature-1024x500.png` | `a53b8e729325e272317fd6612d69311645a34598386d0c619bb076611dd5df7f` |
| `02-solo.png` (first screenshot) | `177f4a3c204abce21a8399fa565afaea7ec27b58cf047cdc88624c583ffcb464` |
| `01-home.png` (second screenshot) | `0d52f9383733031e5cb45d0377d54fd9bb091034f76898b613341be567183c9c` |

Icon mechanically downscaled from the approved master; feature graphic generated with the built-in image tool from the approved logo, then exported at Console dimensions. Prompt: preserve animals, dark green lettering and exact Lamb and Tiger wording; center on warm ivory; no extra text, badges, characters or board. Original generated file remains under Codex generated_images. All final exports were visually inspected.

Screenshots show real Home and initial Solo play on API-36 emulator-5554, English/Modern, debug package `com.maswadkar.lambandtiger.debug` (ads disabled), version 1 / 0.1-solo. Installed APK SHA-256 matched the tested local debug APK: `42fb498bc1f1574f0e19ff43c5132188758709c3f527065acbc12399578c4471`. Capture configuration temporarily used 1080×1920 / 360 dpi, then restored original 1080×2424 / 420 dpi. No app data was cleared or existing save overwritten; a new initial Solo game was created and saved. Screenshots do not certify live ads, release signing, physical Bluetooth or a Play-delivered build. Debug-labelled setup/learning screens were not uploaded.

## Corrected audience assessment

The live target-audience policy summary offers making the entire app compliant **or** using a neutral age screen and complying for children. The previous absolute age-screen-blocker wording was too categorical. Current setup proceeds with the owner's all-age choice and existing all-user CHILD/G/non-personalized/under-age settings. Those flags alone do not certify whole-app legal or Families compliance; Google's review remains outstanding.

Owner accepts proceeding past the two-phone gate and plans final Bluetooth verification from closed-testing downloads. This is owner acceptance, not new physical-test evidence.

## Closed testing and boundaries

Closed testing Alpha country targeting saved for **177 named countries/regions plus Rest of World**, matching D063; this track is now separately targeted from production. Publishing overview confirms those changes, audience age 0+, complete Data safety and English listing information under **Changes not yet submitted for review**. Send app for review remains disabled pending release setup.

The owner-supplied two-account list **Lamb and Tiger closed Alpha** is saved and selected for this track. Other existing tester lists remain unselected and unchanged. Console confirms **2 of 4** track setup tasks complete: countries and testers. The track remains Inactive; its download/opt-in link is not yet available. Exact tester addresses are retained only in the ignored local CSV and Console evidence.

No AAB upload, Play App Signing enrollment, release creation, review submission or rollout occurred. Final signed build/key backup, PC opt-in review, Tamil review and Play-delivered verification remain open. Existing PC opt-in is unchanged. Production country targeting is unchanged.

Next: complete signing custody/current signed candidate, then prepare the closed Alpha release. Google review and final Play-download testing remain separate steps.

Local proof: `initial-setup-complete.png`, `audience-saved.png`, `alpha-countries-saved.png`, `alpha-testers-saved.png`, `publishing-setup-saved.png`; earlier `dashboard-8-of-11.png`, `children-certification.png` and `listing-draft-saved.png` are historical intermediate states.
