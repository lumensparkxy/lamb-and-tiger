# Data safety worksheet — 2026-10-19

Prepared for the ad-enabled candidate. Saved as a complete Console declaration on 2026-10-19 after the owner-certified target-audience declaration; awaiting review with the app. Updated for D065 CDM/Bluetooth with no age collection; do not select “no collection.”

| Data / source | Proposed treatment | Evidence and remaining check |
| --- | --- | --- |
| Approximate location inferred from IP / Mobile Ads | Collected and shared; advertising, analytics, fraud prevention/security | Google documents IP collection and approximate-location use. No app geographic-coordinate targeting. |
| App interactions / Mobile Ads | Collected and shared; advertising, analytics, fraud prevention/security | SDK documents launch, tap and video-view interactions. No custom gameplay analytics. |
| Diagnostics / Mobile Ads | Collected and shared; advertising, analytics, fraud prevention/security | Console Diagnostics includes loading time, battery life, latency and technical diagnostics, matching SDK performance metrics. Separate crash logs and other performance data were not selected. |
| Device or other IDs / Mobile Ads | Conservatively disclose collection/sharing; advertising, analytics, fraud prevention/security | AD_ID and AdServices permissions removed; CHILD applied. This does not establish the absence of app-set or every other SDK identifier. Avoid claiming no identifiers without exact SDK evidence. |
| Solo saves, settings, local AI, ad-frequency timestamp | On-device only; not declared as off-device collection | App-owned local storage; backup disabled. |
| Peer identity, moves, saved-state agreement / secure Bluetooth | Direct encrypted peer transfer; excluded from collection under end-to-end encryption and sharing under user-initiated action | CDM selects the joining endpoint; secure RFCOMM joins the two endpoints without a developer server/intermediary. Both users confirm before game messages. Play's definitions exclude data unreadable by anyone except sender/recipient, and expected user-initiated sharing. This is the source-based disclosure rationale, not newly observed radio proof. |

Mobile Ads documentation confirms TLS in transit. Account creation is absent; do not invent an account-deletion URL. Clearing local storage removes local saves, not third-party SDK records. A support email is not proof that the developer can erase all Google-held data.

Do not declare all advertising collection optional merely because gameplay works offline: there is currently no global user opt-out. Do not declare processing ephemeral merely because the developer has no backend. Final purposes, required/optional answers, retention and deletion statements must match the final binary and SDK behavior.

Sources checked: [Mobile Ads 25.5.0 disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure), [Nearby data collection](https://developers.google.com/nearby/connections/overview#data_collection), [Play collection/sharing definitions](https://support.google.com/googleplay/android-developer/answer/10787469).
