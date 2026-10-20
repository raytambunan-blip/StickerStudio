# Sticker Studio v1.3

Private Android WhatsApp sticker maker without ads. Processing takes place on device.

## Static stickers
Crop, text, remove background offline with Silueta AI (unchanged from v1.2). Separate photo pack.

## Animated stickers
- Pick videos supported by Android, or animated GIF files.
- Select video start and 1–3 seconds, GIF uses first 1–3 seconds.
- Android MediaMetadataRetriever or GIF Movie extracts frames (up to 6 fps), scales with transparent padding to 512x512.
- AnimatedWebp builds a standards-compliant animated WebP RIFF container from Android-compressed WebP frames (RFC 9649).
- Adaptive quality and FPS to fit within 500,000 bytes. If the video cannot fit, choose a shorter duration.
- WhatsApp requires at least 3 animated stickers per pack. Animated and static packs cannot mix; use separate buttons.
- No server; no cloud upload; **AI background removal for animated frames is not included yet**.

## Install
GitHub Actions > latest successful build > Artifacts > StickerStudio-APK > extract app-debug.apk.
Your debug APK signature may differ from previous builds. If Android refuses an update, uninstalling the old app will **delete local sticker collections**.
