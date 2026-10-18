# Harness permission policy

Resolved for the scaffold on 2026-10-17. Target SDK is 36; minimum SDK is 29. The original policy below is retained as history; the current runtime correction is recorded next. Permission grants alone do not establish connectivity.

## Runtime correction — 2026-10-18

The physical Pixel 9a (API 37, harness target 36) returned `MISSING_PERMISSION_ACCESS_WIFI_STATE` (8032) on Host after all runtime grants. Remove the API-32 caps from the normal `ACCESS_WIFI_STATE` and `CHANGE_WIFI_STATE` permissions. Keep the coarse/fine location caps and the API-33 nearby-Wi-Fi runtime boundary. No additional location or Internet permission was added. Host/Join now requests the runtime permission set; Refresh remains read-only. API 37 is covered by the same target-36 policy and unit test. SDK 37 targeting would require a separate local-network permission review.

[Google status-code reference](https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes) identifies 8032/8033 as missing Wi-Fi state/change-state permissions. The previous capped declarations below describe the initial scaffold, not the corrected manifest.

## Original API 32 discrepancy

The [Nearby setup example](https://developers.google.com/nearby/connections/android/get-started) currently caps location/Wi-Fi declarations at API 31 and labels nearby Wi-Fi from 32. The [Android permission reference](https://developer.android.com/reference/android/Manifest.permission#NEARBY_WIFI_DEVICES) and the installed API 36 `data/api-versions.xml` both identify `NEARBY_WIFI_DEVICES` as introduced in API 33. [Android's Wi-Fi guide](https://developer.android.com/develop/connectivity/wifi/wifi-permissions) retains fine location through API 32 for backward compatibility.

Use the platform boundary: retain location and legacy Wi-Fi declarations through API 32; include nearby Wi-Fi in the diagnostic/runtime policy only from API 33. Do not use a `minSdkVersion` attribute on individual permission declarations as a runtime gate. Unsupported permission names are not requested on older systems.

| Device API | Runtime permissions listed by the diagnostic screen | Normal / legacy manifest declarations |
| --- | --- | --- |
| 29–30 | Coarse and fine location | Bluetooth, Bluetooth admin, Wi-Fi state and change Wi-Fi state |
| 31–32 | Coarse and fine location; Bluetooth scan, connect, advertise | Wi-Fi state and change Wi-Fi state |
| 33–36 | Bluetooth scan, connect, advertise; nearby Wi-Fi devices | Network-state permission inherited from Nearby |

Nearby also contributes `ACCESS_NETWORK_STATE` on older supported versions. The Wi-Fi state declarations are extended through API 32 to avoid the same gap as the location declaration. Confirm SDK behaviour on real devices before adopting the experiment in production.

On Android 12/12L, future location requests must request coarse and fine together and handle approximate-only grants as insufficient when precise access is needed. See [Android's location request guidance](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime).

The harness does not derive physical location. Bluetooth scan and nearby Wi-Fi declarations use `neverForLocation`. Android documents that this flag can filter some BLE beacons, so physical discovery remains a required check. See [Bluetooth permission guidance](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions). Do not add location access above API 32 without new evidence that the chosen Nearby operations require it.

No file/storage, camera, microphone, background location, or foreground-service functionality is needed for this byte-message experiment. Permission requests were not implemented in the initial scaffold; they are now initiated by Host/Join. Refresh only reads status; it never launches a permission or Play services repair dialog.

## Verification scope

Unit tests cover API 29/30, the API 31/32 boundary, API 33–36, and rejection of unsupported API 28. Manifest/package inspection must confirm the version caps survive dependency merging and identify any transitive additions. These checks cannot test Android permission dialogs, denial/retry, radio capabilities, or two-phone discovery; those remain later execution steps.
