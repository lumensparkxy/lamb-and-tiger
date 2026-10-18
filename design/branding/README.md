# Lamb and Tiger brand design — v1

Design assets created on 2026-10-18 with the built-in `image_gen` tool, using the approved in-game animal artwork as an identity reference. Approved and integrated under D052: the icon replaces the Android launcher art and the wordmark appears on Home.

- `lamb-and-tiger-icon-v1.png`: 1254 × 1254 RGB square, paired animal mark on teal. Rounded shapes in the preview are CSS masks; the source remains a full square.
- `lamb-and-tiger-logo-v1.png`: 1983 × 793 RGBA wordmark with verified transparent pixels, intended for light backgrounds.
- `preview.html`: renders the actual files together, including 32/48/64 CSS-pixel icon checks.
- `brand-preview-v1.png`: browser screenshot of the design presentation.
- `prompts-v1.json`: exact generation and refinement prompts.

The identity uses ivory wool, sage, amber and dark teal to connect to the Quiet Tabletop UI. Both characters remain recognizable in the inspected small-size previews. These are raster masters. The app packages exact copies as `brand_icon.png` and `brand_logo.png`; the adaptive launcher resource scales the approved square art into the foreground layer over a teal background without repainting the characters. Home uses the transparent wordmark on an ivory surface in both themes. The icon follows Android's [adaptive resource format](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive); no separate monochrome artwork is supplied.

Integration verification: debug assembly and app lint passed. SHA-256 comparisons confirmed the packaged PNGs are exact copies of the approved assets. Installed with an in-place update on `emulator-5554` (API 36); inspected the Pixel launcher's circular icon, tapped it to open the app, and checked Home in Modern and Midnight Prowl. Restored Modern and left Home open. Saved-game Continue remains available. Local screenshots and build log are under `work/brand-integration-20261018/`. No gameplay tests were rerun for this resource/layout-only change; no physical-device or store-publication claim is made.
