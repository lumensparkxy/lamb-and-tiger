# Data safety worksheet — 2026-10-19

Prepared for the ad-enabled candidate, not submitted to Play. Update after the audience/Nearby changes; do not select “no collection.”

| Data / source | Proposed treatment | Evidence and remaining check |
| --- | --- | --- |
| Approximate location inferred from IP / Mobile Ads | Collected and shared; advertising, analytics, fraud prevention/security | Google documents IP collection and approximate-location use. No app geographic-coordinate targeting. |
| App interactions / Mobile Ads | Collected and shared; advertising, analytics, fraud prevention/security | SDK documents launch, tap and video-view interactions. No custom gameplay analytics. |
| Diagnostics and other app performance / Mobile Ads | Collected and shared; advertising, analytics, fraud prevention/security | SDK documents performance metrics. Confirm final Console subtype wording; do not assume a separate crash-reporting product. |
| Device or other IDs / Mobile Ads | Conservatively disclose collection/sharing; advertising, analytics, fraud prevention/security | AD_ID and AdServices permissions removed; CHILD applied. This does not establish the absence of app-set or every other SDK identifier. Avoid claiming no identifiers without exact SDK evidence. |
| Nearby performance/device diagnostics | Review as diagnostics/analytics; conditional on system Usage & diagnostics | SDK documents model, country, build and package with connection performance. Do not equate location permission with coordinate collection. Confirm precise Play categorization and applicable service-provider exemptions. |
| Solo saves, settings, local AI, ad-frequency timestamp | On-device only; not declared as off-device collection | App-owned local storage; backup disabled. |
| Peer identity, moves, saved-state agreement / Nearby | User-initiated encrypted peer transfer; review collection/sharing exemptions independently | Nearby documents encrypted peer connections. Validate the precise end-to-end scope and absence of intermediary access before applying that exemption. |

Mobile Ads documentation confirms TLS in transit. Account creation is absent; do not invent an account-deletion URL. Clearing local storage removes local saves, not third-party SDK records. A support email is not proof that the developer can erase all Google-held data.

Do not declare all advertising collection optional merely because gameplay works offline: there is currently no global user opt-out. Do not declare processing ephemeral merely because the developer has no backend. Final purposes, required/optional answers, retention and deletion statements must match the final binary and SDK behavior.

Sources checked: [Mobile Ads 25.5.0 disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure), [Nearby data collection](https://developers.google.com/nearby/connections/overview#data_collection), [Play collection/sharing definitions](https://support.google.com/googleplay/android-developer/answer/10787469).
