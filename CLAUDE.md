# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An Android port of the GZDoom engine bundled with the open-source Freedoom assets, published as "Freedoom for Android" (`net.nullsum.freedoom`). It is a fork of nvllsvm's GZDoom-Android. The app is a thin Java/Kotlin shell around a large C/C++ engine compiled via the NDK.

## Build

The native engine and the Android app are built in **two separate, sequential steps**. Gradle does *not* drive the native build (there is no `externalNativeBuild` block); instead `ndk-build` produces `.so` files into `doom/src/main/libs`, which Gradle picks up via `jniLibs.srcDirs("src/main/libs")`.

```bash
ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/27.0.12077973 ./build_native.sh   # ndk-build → .so
./gradlew :doom:assembleDebug                                                # APK = Kotlin + .so
```

- `build_native.sh` auto-locates `ndk-build` (honours `ANDROID_NDK_HOME`, else the highest NDK under the SDK) and runs it with `jni/Application.mk`. The committed `.so` under `doom/src/main/libs/{armeabi-v7a,arm64-v8a}/` mean the app **already assembles + runs without re-running the native build**; only re-run it if you change `doom/src/main/jni/**`.
- Native target ABIs are **`armeabi-v7a` and `arm64-v8a`** (`doom/src/main/jni/Application.mk`) — arm64 is required for modern 64-bit-only devices.
- Toolchain: **Java 17** (`gradle.properties` pins `org.gradle.java.home` to a local Temurin 17 path — adjust per machine); **NDK r27** for the native build. `local.properties` (gitignored) must point `sdk.dir` at your SDK.
- Verified playable: installs on an arm64 emulator/device, boots the tabbed UI, and launches Freedoom (engine renders at native resolution with the on-screen touch controls).

There is no test suite wired up beyond the default `androidx.test.runner.AndroidJUnitRunner` declaration.

## The native engine (rebased on emileb's maintained fork)

The original 2017 GZDoom 2.0.x native build was unrecoverable (3 of 4 submodule repos deleted from GitHub; toolchain GCC 4.9 + STLport removed from the NDK). The native layer was **rebased onto emileb's maintained, FMOD-free Freedoom/GZDoom port** ([`github.com/emileb/gzdoom` @ `freedoom_for_android_gz1.9_no_fmod`](https://github.com/emileb/gzdoom)), which uses the *same* `net.nullsum.freedoom.NativeLib` JNI surface, so it binds directly to the Kotlin shell.

The whole native stack is now **vendored** under `doom/src/main/jni/` (no submodules, no patch-overlay): `gzdoom_android/` (the engine, with `mobile/Android.mk` + `mobile/src/android-jni.cpp` glue), plus `SDL` (1.x), `jwzgles` (GL ES1-over-ES2), `MobileTouchControls`, `AudioLibs_OpenTouch` (openal/mpg123/sndfile/fluidsynth-lite/flac), `jpeg8d`, `SAFFAL`, `beloko_common`. `jni/Android.mk` is the master makefile; `jni/Application.mk` carries the ABI/C++14/Clang-strictness settings needed for NDK r27. The audio is OpenAL + FluidSynth (**no FMOD**). The engine's matching base resources are bundled at `doom/src/main/assets/gzdoom.pk3` (must match the engine version, or it errors on `IWADINFO`).

To change engine behavior, edit the source under `doom/src/main/jni/gzdoom_android/` directly and re-run `./build_native.sh` — there is no longer any `build.sh`/`diff.sh`/`android_gzdoom/patches` overlay.

## Architecture

**Gradle modules** (`settings.gradle.kts`): `:doom` (the application) depends on `:touchcontrols` (an Android library). The version catalog lives in `gradle/libs.versions.toml`; dependency repos are locked via `FAIL_ON_PROJECT_REPOS`.

**JNI bridge.** `NativeLib.kt` (`net.nullsum.freedoom`) is the single Kotlin↔C++ seam: it `System.loadLibrary(...)`s the engine libs (`touchcontrols`, `openal`, `gzdoom`, plus SDL via `SDLLib`; **no FMOD**) and declares the `external` native methods — `init`, `frame`, `setScreenSize`, `touchEvent`, `keypress`, `doAction`, the `analog*` motion calls, etc. These are implemented in `doom/src/main/jni/gzdoom_android/mobile/src/android-jni.cpp`, where the `JAVA_FUNC(x)` macro expands to `Java_net_nullsum_freedoom_NativeLib_##x`. The seam classes use the `class { companion object { @JvmStatic external fun … } }` pattern so the native methods stay `static` on the outer class (verify with the `javap` golden diff in `.jni-golden/`). `NativeLib` also implements `ControlInterface` so the touchcontrols module can feed input straight into the engine.

**App flow (Java/Kotlin under `doom/src/main/java/net/nullsum/freedoom`):**
- `EntryActivity` — launcher activity; an ActionBar with three tabs/fragments: GZDoom launch, Gamepad config, Options. Loads `AppSettings` on create.
- `LaunchFragmentGZdoom` — scans the Freedoom data dir for `.wad`/`.pk3`/`.pk7` files, lists them, and on selection builds an `Intent` to `Game` carrying `args` and `game_path` extras. `ModSelectDialog` handles add-on mod selection.
- `Game` — the actual gameplay `Activity`. Owns a `MyGLSurfaceView` (an OpenGL ES `GLSurfaceView`), constructs `NativeLib`, wires a `ControlInterpreter` for touch/gamepad input, then assembles the engine command line (resolution from surface size ÷ `resDiv`, `+set vid_renderer 1`, the `-iwad`/`-file` args) and calls `NativeLib.init(...)`. The render loop drives `NativeLib.frame()` and swaps buffers.
- `AppSettings` — static `SharedPreferences` wrapper; holds the Freedoom base dir (default `<external storage>/Freedoom`), music dir, graphics dir (`filesDir`), and toggles. `Utils.copyFreedoomFilesToSD` unpacks bundled WADs from assets to external storage on first run.

**touchcontrols module** (`com.beloko.touchcontrols`) — beloko/nvllsvm's on-screen touch controls and gamepad handling (`ControlInterpreter`, `ControlConfig`, `TouchControlsEditing`, `GamePadFragment`), plus a vendored drag-sort ListView under `com.mobeta.android.dslv`. Its native side is the `MobileTouchControls` submodule compiled into `libtouchcontrols.so`.

**Native source layout** (`doom/src/main/jni/`): `Android.mk` includes the per-component makefiles — `GL` (GL ES shim), `jpeg8d`, `gzdoom_android`, `FMOD_studio`, `fluidsynth`, `openal-soft-android`, `MobileTouchControls`, `SDL`. SDL is an old 1.x branch; FMOD and OpenAL provide audio; FluidSynth handles MIDI/soundfont music.

## Notable constraints

- This is an intentionally **frozen, legacy** project (engine is GZDoom 2.0.x-era / "super ancient" per the README, SDL 1.x, GL ES 1.x). When touching the build it is easy to hit deprecated-tooling issues — the recent in-flight work (see `git status`) is migrating Groovy `build.gradle` → Kotlin `.kts` with a version catalog and AGP 9.
- App-facing strings live in heavily-translated `res/values-*/strings.xml` (Transifex-sourced); prefer editing `res/values/strings.xml` and leaving translations alone.
