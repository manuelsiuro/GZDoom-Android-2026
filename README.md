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

- **100% Kotlin.** All 38 Java sources converted to Kotlin (app code + the vendored libSDL and
  DragSortListView libraries), preserving the exact JNI contract with the native engine.
- **Modern Android (targets Android 14/15, API 34/35).**
  - `ViewPager2` + Material `TabLayout` (replacing the removed `ActionBar` navigation tabs).
  - **Scoped storage** via app-specific external dirs (no `WRITE_EXTERNAL_STORAGE`).
  - `android:exported`, `WindowInsetsControllerCompat` immersive mode, `MODE_PRIVATE` prefs.
- **64-bit ready.** Native libraries build for **`arm64-v8a`** *and* `armeabi-v7a`, so the app
  runs on today's 64-bit-only phones and satisfies the Google Play 64-bit requirement.
- **16 KB page support** (Android 15+): native `.so` are 16 KB `LOAD`-aligned and packaged
  uncompressed/page-aligned.
- **FMOD removed.** Audio now uses the open **OpenAL + FluidSynth** backend (this also unblocked
  arm64), so the project is fully open-source with no proprietary blobs.
- **Native engine rebased** onto Emile Belanger's actively-maintained, FMOD-free Freedoom/GZDoom
  fork ([emileb/gzdoom](https://github.com/emileb/gzdoom)), which uses the same
  `net.nullsum.freedoom` JNI surface. Builds with the current **NDK (r27)** — the dead 2017
  submodule tree and the old `build.sh`/patch-overlay system are gone.
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
on its own. Requirements: **JDK 17**, Android SDK (compileSdk 34), and **NDK r27** for the native
build. `build_native.sh` auto-locates `ndk-build` (honours `ANDROID_NDK_HOME`). The full native
source for the engine and its dependencies is vendored under `doom/src/main/jni/`.

See [`CLAUDE.md`](CLAUDE.md) for the architecture and build details.

## Third-party components

- **Engine:** [emileb/gzdoom](https://github.com/emileb/gzdoom) — maintained, FMOD-free
  Freedoom/GZDoom port (vendored, built with NDK r27 for armeabi-v7a + arm64-v8a).
- **Audio:** OpenAL + [FluidSynth](https://github.com/FluidSynth/fluidsynth)-lite, mpg123,
  libsndfile (Emile Belanger's `AudioLibs_OpenTouch`).
- **Graphics:** [jwzgles](https://www.jwz.org/jwzgl/) GL ES 1.x-over-ES2 wrapper.
- **Input:** [emileb/MobileTouchControls](https://github.com/emileb/MobileTouchControls).
- **Platform:** [SDL](https://www.libsdl.org/) 1.x, [SAFFAL](https://github.com/emileb/SAFFAL).

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
- [ ] Update SDL 1.x → SDL2 and the GL ES 1.x path → GL ES 3.x / Vulkan (modern GZDoom 4.x)
- [ ] Integrate an idgames level browser/downloader
- [ ] Add a "download WAD from URL" feature

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
