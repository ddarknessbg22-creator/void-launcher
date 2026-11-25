# Verification notes

## Build
- Gradle 8.7, AGP 8.6.0, Kotlin 2.0.21, Compose BOM 2024.09.03.
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- `:app:testDebugUnitTest` — 4 tests, 0 failures (offline UUID algorithm, username validation, byte formatting).

## Device checks
- APK installed on Android 16 / arm64-v8a; launched with no crash in logcat.
- AMOLED home screen renders with correct status-bar insets (screenshot verified).
- Offline profile "Rei" persisted in `shared_prefs/void_accounts.xml`.
- Live Mojang manifest parsed: 926 versions, latest release 26.3.
- 1.21.11 reported installed and verified from local game files.
- Monochrome toast appears/dismisses; no light Material snackbar remains.
- Settings tabs (Account, Game, Java, Network, Engine, Storage, About) render.

## Honest status
- Actual Minecraft rendering: **BLOCKED** — no compatible native engine is installed on the test device,
  and VOID deliberately does not install a second launcher APK.
- Engine handoff: **BLOCKED** on this device for the same reason.
- Game download foreground service, Java 8/17 ARM64 runtime download, Microsoft auth and mod management:
  **BUILT, NOT DEVICE-VERIFIED** end-to-end.
