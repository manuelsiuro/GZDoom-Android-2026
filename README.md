# Freedoom for Android — 2026 Modernization
<img src="icon.png" width="200" hspace="10" vspace="10"></br>

A 2026 modernization fork of mkrupczak3's / nvllsvm's **GZDoom-Android** Freedoom port,
brought back to life on current Android tooling. It bundles the open-source
[Freedoom](https://freedoom.github.io/) assets (`freedoom1.wad` / `freedoom2.wad`) with a
GZDoom engine so it runs out of the box as a completely open-source Doom game — now building,
running, and **playable on modern 64-bit Android devices**.

> The original project had stopped active development because its build tooling was deprecated
> and several of its native source dependencies were deleted from GitHub. This fork rebuilds the
> app on a maintained foundation. See **What's new in 2026** below.

## ✨ What's new in 2026

- **In-app map editor (PNG2WAD).** A new **editor** tab lets you draw a level on a colored
  grid and generate a playable Doom map on-device — no PC tools. Pick a **theme**
  (`Tech`/`Cave`/`Hell`/`City`) and paint tiles (walls, rooms, doors, secrets, special floors/ceilings,
  sky, player start, exit), then tap **Test** to boot straight into `MAP01`, or **Generate WAD** to
  just write the file. Under the hood it renders the grid to a PNG and runs a
  bundled native converter (`libpng2wad.so`, built from the vendored
  [`png2wad-sdk/`](png2wad-sdk/) module) to emit a **nodeless
  Doom-format PWAD** into `<base>/mods/generated.wad`; GZDoom builds the nodes/blockmap on load.
  The editor is **mobile-first**: a persistent left **tool rail** of icon buttons (brush, eraser,
  fill, line, rect, eyedropper, pan), a compact top bar (undo/redo/test + overflow), a left
  **drawer** for project/setup actions (size, theme, maps, templates, tuning, projects, share), and
  a bottom tile palette — with an adaptive landscape layout (canvas + right-hand side panel) on
  larger screens. See [`PNG2WAD_MAP_EDITOR.md`](PNG2WAD_MAP_EDITOR.md) for the tile palette, pipeline,
  and the strict-lump-order fix that made generated WADs load on this 1.9-era engine.
- **Built-in WAD browser & downloader.** A new **Browse** tab fetches add-ons straight from the
  Doomworld [/idgames archive](https://www.doomworld.com/idgames/) — search by title/filename/author,
  see ratings and sizes, and one-tap download (with mirror fail-over) that unzips into the add-on
  folder so it shows up immediately in the launcher. Extras:
  - A curated **Featured classics** shelf (Scythe, Alien Vendetta, Hell Revealed, Requiem…).
  - A **Classic games** shelf that downloads the freely-distributable shareware/freeware IWADs
    (Doom shareware, Heretic shareware, Hexen/Strife demos, Chex Quest 1 & 3).
  - A **Commercial games — bring your own copy** shelf: the still-sold games (Doom, Doom II, Final
    Doom, Heretic, Hexen, Strife) are *never* downloaded; instead you **import your own `.wad`**
    from a copy you bought via the Android file picker — the same model GZDoom uses on PC.
  - **Compatibility badges** on every entry — Doom/Doom II/Heretic/Hexen/Strife/Chex plus the
    map-slot (ExMy vs MAPxx) — guessed up front from the archive path and confirmed from the
    upload's text file, so you know which IWAD a WAD needs before downloading.
  - A **rich detail sheet** (fetched via the idgames `get` API): full text file, base, credits,
    editors, build time, known bugs, reviews/ratings, and a "play with" IWAD hint.
  - An **Archive** mode that browses the idgames file tree by category (`levels/doom2/…`) straight
    from the gamers.org mirror, in addition to keyword search.
  - **`idgames://` deep links** — tapping an idgames link from a browser/forum opens the Browse
    tab on that WAD's detail sheet, ready to download.
  - **Per-game download folders:** add-ons are auto-sorted into `wads/doom2/`, `wads/heretic/`, …
    based on detected compatibility, so the picker can group them by game.
  - An **Installed** view listing everything you've installed (add-ons + IWADs) with multi-select
    and **bulk delete** (selected or all). Individual **delete** also works from any entry, and a
    **Reset** button on the launch screen clears a stuck game/mod selection.
- **100% Kotlin.** All 38 Java sources converted to Kotlin (app code + the vendored libSDL and
  DragSortListView libraries), preserving the exact JNI contract with the native engine.
- **Jetpack Compose Material 3 launcher.** The whole launcher UI was rewritten in Compose with a
  dark Doom theme (blood-red / bone-amber on near-black):
  - Two-pane launch screen: WAD cards with size and selected state, big Launch button.
  - Add-ons are first-class: a bottom-sheet picker browses `wads/`/`mods/` with checkboxes
    (folders included — no more long-press), selections show as removable chips, and `.deh`/`.bex`
    patches map to `-deh` automatically. The picker shows the **selected game** in its title,
    compatibility badges per add-on, an **"only compatible"** filter, and **favorites** — star any
    WAD/mod or folder and filter to just your starred ones (persisted across sessions).
  - Command-line args field with history dropdown; Options tab with data-folder picker and
    resolution divider.
  - First-run unpacking now shows a progress indicator (the old 10-second activity-restart hack
    is gone).
  - The in-game UI is untouched: the engine command-line contract is byte-identical (guarded by
    a unit test) and the gamepad-config screen stays View-based via Compose fragment interop.
- **Modern Android (compileSdk 36, minSdk 23).**
  - **Scoped storage** via app-specific external dirs (no `WRITE_EXTERNAL_STORAGE`).
  - `android:exported`, `WindowInsetsControllerCompat` immersive mode, `MODE_PRIVATE` prefs,
    edge-to-edge launcher with **themed system bars** (white status-bar icons; the navigation
    bar shares the bottom-navigation color).
- **64-bit ready.** Native libraries build for **`arm64-v8a`** *and* `armeabi-v7a`, so the app
  runs on today's 64-bit-only phones and satisfies the Google Play 64-bit requirement.
- **16 KB page support** (Android 15+): native `.so` are 16 KB `LOAD`-aligned and packaged
  uncompressed/page-aligned.
- **FMOD removed.** Audio now uses the open **OpenAL + FluidSynth** backend (this also unblocked
  arm64), so the project is fully open-source with no proprietary blobs.
- **Native engine on UZDoom 5.0.0-pre** — the engine was rebased onto GZDoom 4.15 and then
  swapped to Emile Belanger's **UZDoom 5.0.0-pre** (`uz_5.0_pre`, a maintained GZDoom fork —
  [emileb/gzdoom](https://github.com/emileb/gzdoom)), verified booting `doom2.wad` on an arm64
  device ("UZDoom version 5.0.0-pre", base pk3s + IWAD load, OpenAL+EFX up). That brings SDL2,
  the modern GLES3 renderer, ZMusic, and **ZScript support** (most idgames releases of the last
  decade need GZDoom 4.x, so the Browse tab's downloads actually run now). The base data is
  rebuilt from the engine's `wadsrc*` trees (`uzdoom.pk3` + `uzdoom_game_support.pk3`). Builds
  for both ABIs with the current **NDK (r27)**, C++20 + abseil from the engine's own makefiles —
  the dead 2017 submodule tree and the old `build.sh`/patch-overlay system are gone.
- **AGP 9 / Gradle Kotlin DSL**, version catalog, AGP-9 built-in Kotlin — **zero build, lint, and
  manifest warnings.**

## Building

The app is built in two steps (Gradle does not drive the native build):

```bash
# 1. Build the native engine (.so) with the NDK. Already committed under doom/src/main/libs,
#    so you only need this if you change anything under doom/src/main/jni.
ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/27.0.12077973 ./build_native.sh

# 2. Build the APK (Java/Kotlin + the prebuilt .so).
./gradlew :doom:assembleDebug
```

Because the prebuilt `.so` are committed, `./gradlew :doom:assembleDebug` produces a working APK
on its own. Requirements: **JDK 17**, Android SDK (compileSdk 36), and **NDK r27** for the native
build. `build_native.sh` auto-locates `ndk-build` (honours `ANDROID_NDK_HOME`). The full native
source for the engine and its dependencies is vendored under `doom/src/main/jni/`.

See [`CLAUDE.md`](CLAUDE.md) for the architecture and build details.

### Known limitation: rendering on the Android Emulator

On the Android Emulator the engine boots, plays sound, and accepts input, but the game
framebuffer presents **black** (the native touch-control overlay and the engine's own UI
windows are visible). This reproduces on both emulator GPU modes (SwiftShader and host-GPU
translation), which both run GL through a translation layer; the previous engine generation
had similar emulator-only artifacts that did not occur on real devices. Verify rendering on
real hardware.

## Third-party components

- **Engine:** [emileb/gzdoom](https://github.com/emileb/gzdoom) `uz_5.0_pre` — UZDoom 5.0.0-pre,
  a mobile GZDoom fork (vendored, built with NDK r27 for armeabi-v7a + arm64-v8a).
- **Audio:** OpenAL + [FluidSynth](https://github.com/FluidSynth/fluidsynth)-lite, mpg123,
  libsndfile (Emile Belanger's `AudioLibs_OpenTouch`).
- **Input:** [emileb/MobileTouchControls](https://github.com/emileb/MobileTouchControls).
- **Platform:** [SDL2](https://www.libsdl.org/) (emileb fork), [SAFFAL](https://github.com/emileb/SAFFAL),
  Clibs_OpenTouch glue, ZMusic, glslang, ZWidget.
- **Map editor:** **png2wad** PNG→WAD converter (originally a C# tool by
  [@akaAgar](https://github.com/akaAgar/png2wad), ported to C/C++ here), built as `libpng2wad.so`
  via the `:png2wad-sdk` module.

## Why Freedoom?
While the Doom engine and its many spin-offs are open-sourced, most of Doom's "assets" such as
textures, sounds, and game levels are copyrighted and not legal to redistribute. The Freedoom
project offers an alternative set of assets and game levels that are open-source and can be used
with most Doom engines in place of the originals. Freedoom is also compatible with much of the
vast library of fan-made "WADs" (i.e. game levels) as indexed in the idgames archive.

## Roadmap

- [x] Add **arm64** support (Google Play 64-bit requirement)
- [x] Switch to [emileb's MobileTouchControls](https://github.com/emileb/MobileTouchControls)
- [x] Remove the proprietary FMOD dependency (now OpenAL + FluidSynth)
- [x] 16 KB page-size support for Android 15+
- [x] Jetpack Compose Material 3 launcher UI (dark Doom theme, chip-based mod selection)
- [x] Integrate an idgames level browser/downloader (Browse tab)
- [x] Add a WAD-download feature (idgames + classic shareware/freeware games)
- [x] Import-your-own-copy flow for the commercial IWADs (Doom, Doom II, Final Doom, …)
- [x] Compatibility badges + rich detail view, archive-tree browser, `idgames://` deep links,
      per-game download folders, and an Installed view with bulk delete
- [x] Favorites in the add-on picker (star WADs/mods/folders + filter)
- [x] In-app PNG2WAD map editor (draw a grid → generate a playable map → launch it)
- [x] Update SDL 1.x → SDL2 and the GL ES 1.x path → GL ES 3.x (GZDoom 4.15, `mobile_4.15.x`)
- [x] Swap the native engine to UZDoom 5.0.0-pre (`uz_5.0_pre`) — builds & runs on device
- [ ] Verify/fix rendering on a real device, then investigate the emulator black-screen present

## Links to the Freedoom community
[Freedoom official GitHub](https://github.com/freedoom/freedoom) ·
[Freedoom forums](https://www.doomworld.com/forum/17-freedoom/)

## Credits
Thanks to:
- **Emile Belanger** (Beloko Games) — for the maintained, FMOD-free engine fork, the OpenTouch
  libraries (MobileTouchControls, AudioLibs, SDL, SAFFAL) this rebase is built on, and the
  rest of the OpenGames / Delta Touch suite.
- **Matthew Krupczak** (mkrupczak3) and **Andrew Rabert** (nvllsvm) — for the original
  GZDoom-Android / Freedoom-for-Android work this fork builds upon.
- The **Freedoom** authors for an excellent set of open-source assets. See
  `/doom/src/main/assets/CREDITS.txt` for the full list.
- The GZDoom / ZDoom teams and id Software for the engine lineage.

## Disclaimer
This project is not affiliated with Doom or its publishers, Id Software or parent companies, or
Bethesda. It is not officially endorsed by the Freedoom or GZDoom projects.

## License
Freedoom is released under a BSD-like license which can be found under
`/doom/src/main/assets/COPYING.txt`. The GZDoom engine and most other code is GPL.
