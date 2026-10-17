# Pixel9a diagnostic smoke check — 2026-10-17

Scope: install the existing debug APK, inspect its rendered screen, exercise Refresh, and review launch logs. No source changes, new build, discovery, connection, or message exchange was performed.

## Tested artifact and environment

- APK SHA-256: `16e001c211ec10e7031fe2ed138f9140271fc70d8ec1bf3654b5052577813c67` (matches the verified build).
- Base Git commit: `ec1d4dd20a0777545832cd7322ac436133e20e76`; scaffold is uncommitted.
- Source snapshot SHA-256: `317f27bda2733841a7ee2ae5c7496ed34c9b2e5b4592d1ab057622f32a5e401a`. Computed over the sorted per-file SHA-256 list for source, resources, build configuration, and wrapper files; the list is retained with local evidence.
- Pixel9a AVD, Android 16/API 36, portrait 1080 × 2424 px, density 420 dpi, font scale 1.0.
- Google Play services 26.34.36 (260400-981326859); Play Store 53.0.27-34 [0] [PR] 973951861.

## Results

| Check | Result |
| --- | --- |
| Install and activity resolution | ADB install returned Success; the launcher resolved to the expected MainActivity. |
| Rendered screen | Inspected launch and restored-state screenshots plus UI trees. All headings, diagnostic fields, notices, and Refresh were visible without clipping or overlap at this configuration. |
| Service status | Screen reported SUCCESS and the expected package versions. This is Play services availability, not Nearby connectivity. |
| Feature status | Bluetooth, BLE, and Wi-Fi were reported by the emulator; no radio capability claim follows from that report. |
| Initial permissions | All four API 36 Nearby runtime permissions showed Not granted; no permission prompt appeared. |
| Refresh with unchanged state | A UI-tree-derived tap left the correct diagnostics visible. |
| Refresh with changed state | ADB temporarily granted only BLUETOOTH_SCAN while the app remained foreground in the same process. The screen still showed Not granted before tapping Refresh; afterward it showed Granted. Other permissions stayed Not granted. This proves the button recalculated visible status. |
| Restoration | Revoked the temporary grant, relaunched, and verified all four permissions Not granted in both the UI and package state. Android recorded the old process exit as PERMISSION CHANGE, not a crash. |
| Launch log review | No FATAL EXCEPTION, SecurityException, or ANR found in the captured app-process logs; no harness entry in the crash buffer. Rendering/ashmem messages and skipped frames were observed and retained. |

## Open performance finding

ADB reported `LaunchState: COLD` and `TotalTime: 12193 ms` on first launch; after permission restoration it reported `TotalTime: 4885 ms`. Wait times were 12305 ms and 5034 ms respectively. The first process logged skipped-frame batches and long frames, plus EGL/HWUI configuration warnings and an ashmem deprecation message. Similar rendering/ashmem messages appeared in the second process.

These are two observations from this emulator, not a benchmark or a diagnosed cause. Functional diagnostics and Refresh passed; smooth startup is not established. Investigate the launch delay before treating performance as acceptable. No SDK reinstall, emulator reconfiguration, or speculative code fix was made.

## Evidence and boundaries

Raw screenshots, UI trees, package-state dumps, app-process logs, crash-buffer snapshots, timing metadata, and source checksums remain in the ignored repository directory `work/nearby-emulator-smoke-20261017/`. No raw device logs or generated APK are added to version control. Existing device logs were preserved rather than cleared.

The app remains installed and open on Pixel9a, with the temporary grant revoked. No device-wide setting was changed. This check covers the diagnostic portion of E01 only. Host/Join and permission request/denial flows are not implemented; E02–E10 remain unrun. ADB grant/revoke is test instrumentation, not evidence of the app's permission UX. Landscape, enlarged text, TalkBack, additional Android versions, and two physical phones were not tested.

Follow-up investigation of the recorded emulator launch delay was proposed, then deferred by the user under D036. Preserve this finding for later; do not resume Nearby work automatically on NEXT. Issue #4's module/UI architecture was subsequently approved in D037; the state/action contract was approved in D038. D039 now batches the remaining architecture prerequisites for one final review; the physical-phone experiment remains deferred.
