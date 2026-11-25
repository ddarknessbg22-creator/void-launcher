# VOID Launcher

**Beyond the ordinary.**

A native Android launcher for **Minecraft: Java Edition**, written in Kotlin, Jetpack Compose and Material 3.
VOID manages real accounts and real game files itself, then hands the session to a proven native game
engine for rendering.

> This is an honest, working launcher **frontend and game manager**. It does **not** contain a JVM,
> LWJGL or a GL translation stack of its own — see [Engine architecture](#engine-architecture) and
> [Feature verification](#feature-verification).

---

## What actually works

### Offline accounts (first-class)
- Create multiple local profiles with a username (3–16 characters).
- Deterministic Minecraft-compatible offline UUID (`OfflinePlayer:<name>`).
- Create, edit, select and delete profiles. The last used profile is auto-selected.
- Profiles persist in local `SharedPreferences`.
- No Microsoft sign-in is required to install and manage game files.
- Offline profiles can join servers that **explicitly allow offline mode**. Servers with online
  authentication still require an authenticated Microsoft account — the app says so in Settings.

### Version manager
- Fetches the **live Mojang version manifest** (`version_manifest_v2.json`), with an on-device cache
  for offline browsing.
- Search, filter by Release / Snapshot / Older versions.
- Resolves each version's real JSON metadata, including `inheritsFrom` chains used by mod loaders.
- Installs the real client jar, desktop libraries and every asset object.
- **SHA-1 verification** of each downloaded artifact before it is committed; a single mismatch aborts
  the install and nothing is marked installed.
- Live progress (files + bytes), cancellation and retry with backoff.

### Java manager
- Detects Java runtimes by reading their `release` descriptor (no shell execution, no root).
- Multiple runtimes, user-added paths, version/vendor reporting.
- Reads `javaVersion.majorVersion` from Mojang metadata and selects the lowest compatible runtime.
- **No JRE is bundled**: an Android-compatible runtime must be supplied by the game engine.

### Mods
- Per-version `mods` folder, real `.jar` inspection (`fabric.mod.json`, `neoforge.mods.toml`,
  `META-INF/mods.toml`, `mcmod.info`).
- Enable/disable by renaming, import, delete.
- Live Fabric loader version data from `meta.fabricmc.net`.

### Microsoft accounts (optional)
- Dedicated one-purpose WebView for Microsoft sign-in only (the main UI is 100 % native Compose).
- Full chain: Microsoft OAuth → Xbox Live → XSTS → Minecraft profile → entitlement.
- Access and refresh tokens are encrypted with a hardware-backed **Android Keystore AES-GCM** key;
  no plaintext token is ever stored, and no password is ever handled.

### Settings
Launcher-level controls only: choosing MobileGlues / ANGLE / GL4ES changes which engine is launched;
Java selection, isolated working directory, memory allocation, JVM arguments, automation, download
retries and thread count. In-game video/audio settings are deliberately **not** exposed here.

---

## Engine architecture

Running Minecraft Java on Android requires a native stack: an Android-compatible JVM, patched LWJGL,
audio, input, and a GL translation layer (ANGLE / VirGL / GL4ES / **MobileGlues** etc.). That stack is a
multi-year project of its own. VOID deliberately reuses proven engines instead of pretending to have one:

- It only detects compatible engines that are **already installed**. VOID never installs a second
  launcher APK; if none is found, it says so honestly and links to the engine source.
- On **Launch**, game files and any missing Java 8/17 ARM64 runtime are downloaded by a foreground
  service (so downloads survive leaving the app). It then writes a real launch plan
  (`VOID/exports/void-session-<version>.json`) and opens an already-installed engine with an explicit
  Android intent.
- The engine performs the actual rendering and input. VOID reports honestly that the version must be
  selected inside the engine to start the game.

**Sodium / MobileGlues:** choosing MobileGlues makes VOID prefer ZalithLauncher (whose engine uses
MobileGlues); choosing ANGLE/GL4ES prefers PojavLauncher. Actual Sodium rendering depends on the
external engine. VOID does not claim Sodium works. The selected engine must already be installed.

---

## Feature verification

| Feature | Status | Notes |
| --- | --- | --- |
| Offline account CRUD + persistence | **VERIFIED** | Exercised by local unit tests |
| Version browser (live Mojang manifest) | **VERIFIED** | Parsed real manifest over the network |
| Game installation + SHA-1 checks | **BUILT, NOT DEVICE-VERIFIED** | Implemented; full install needs a long download |
| Java runtime detection | **BUILT, NOT DEVICE-VERIFIED** | Descriptor parsing implemented |
| Microsoft authentication | **BUILT, NOT DEVICE-VERIFIED** | Requires a real Microsoft account to test |
| Mod management | **BUILT, NOT DEVICE-VERIFIED** | Jar parsing + enable/disable implemented |
| Engine handoff | **BUILT, NOT DEVICE-VERIFIED** | No compatible engine installed on the test device |
| In-app Minecraft rendering | **BLOCKED** | Requires bundling a native JRE + LWJGL + GL stack |
| In-app touch controls | **BLOCKED** | Requires the native engine's input layer |

The in-app About screen shows this same classification, so the app never overstates what it does.

---

## Build

```sh
git clone https://github.com/ddarknessbg22-creator/void-launcher.git
cd void-launcher
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (the debug build carries a `.debug` application
suffix so it can be installed next to a release build).

`local.properties` must point at an Android SDK (`sdk.dir=...`); it is intentionally gitignored.

## Install

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the device and open it. Android 11 (API 30) or newer and ARM64 are recommended.
No root, no special permissions, no background service.

## First run

1. Create an **offline profile** (no sign-in needed).
2. Open **Versions**, pick a version and press **Install**. Wait for SHA-1 verified downloads.
3. Install a compatible game engine if none is detected (**Settings → Game engine**).
4. Return to **Home** and press **Launch Minecraft**. Select the version inside the engine.

---

## Known limitations

- VOID does not render Minecraft itself and has no built-in touch controls.
- No JRE is bundled. Java 8/17 ARM64 runtimes can be downloaded from Pojav's builds; Java 21+ is
  provided by the installed engine, since no public ARM64 JRE 21 binary is available.
- Mod *installation* (running Fabric/Forge installers to produce modded jars) is delegated to the
  engine; VOID manages and detects mod jars and loaders.
- Microsoft sign-in uses the public client id employed by open-source Android launchers; it needs a
  real account with Minecraft ownership and has not been device-verified here.
- No second launcher APK is downloaded or installed by VOID. Without an already-installed compatible
  engine, pressing Launch cannot start Minecraft — this is reported rather than faked.
- Multiplayer: offline profiles only work on servers explicitly configured for offline mode.

---

## Third-party research and attribution

No third-party source code is copied into this repository. The following projects were studied to
choose an integration strategy, and remain under their own licenses:

- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) — LGPL-3.0
- [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2) — GPL-3.0 (MobileGlues-based
  renderer research)
- [FoldCraftLauncher](https://github.com/FCL-Team/FoldCraftLauncher) — GPL-3.0

Build-time dependencies (AndroidX, Compose, Kotlin, kotlinx.serialization) retain their own licenses.

## License

MIT — see [LICENSE](LICENSE).
