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

**App flow (Kotlin under `doom/src/main/java/net/nullsum/freedoom`):** the launcher UI is **Jetpack Compose Material 3** (dark Doom theme in `ui/theme/`, BOM via `org.jetbrains.kotlin.plugin.compose` — note `material-icons-core` is an explicit dependency and three extra icons are hand-inlined in `ui/DoomIcons.kt`).
- `EntryActivity` — launcher activity; loads `AppSettings`, then `setContent { DoomTheme { MainScreen() } }`. Still an `AppCompatActivity`: it forwards `onKeyDown/onKeyUp/onGenericMotionEvent` to `GamePadFragment` found via `supportFragmentManager`.
- `ui/MainScreen.kt` — `PrimaryTabRow` + `HorizontalPager` with **`beyondViewportPageCount = 2`** (keeps the gamepad fragment resident so input forwarding works from any tab — do not remove). Tab 1 hosts `GamePadFragment` via `androidx.fragment.compose.AndroidFragment` interop; tabs 0/2 are pure Compose.
- `ui/launch/` — `LaunchScreen` (two-pane: WAD cards + launch panel with mod chips and args field), `ModPickerSheet` (bottom-sheet add-on browser over `wads/`+`mods/`), `LaunchState` (state holder; first-run asset unpack), `LaunchArgs.kt` (pure `buildLaunchArgs`/`buildModArgs` — **must stay byte-identical to the legacy command line**; guarded by `doom/src/test/.../LaunchArgsTest.kt`). Selection persists to `last_iwad` (int, legacy) + `last_iwad_name` (preferred).
- `ui/options/` — `OptionsScreen` (base dir choose/reset/SD-card, resolution divider → `gzdoom_res_div`), `DirectoryPickerDialog`.
- `Game` — the actual gameplay `Activity` (View-based, NOT Compose). Owns a `MyGLSurfaceView` (an OpenGL ES `GLSurfaceView`), constructs `NativeLib`, wires a `ControlInterpreter` for touch/gamepad input, then assembles the engine command line (resolution from surface size ÷ `resDiv`, `+set vid_renderer 1`, the `-iwad`/`-file` args) and calls `NativeLib.init(...)`. The render loop drives `NativeLib.frame()` and swaps buffers. Launch contract from the UI: Intent extras `args`, `game_path`, `res_div`, `game`.
- `AppSettings` — static `SharedPreferences` wrapper; holds the Freedoom base dir (default `<external storage>/Freedoom`), music dir, graphics dir (`filesDir`), and toggles. `Utils.copyFreedoomFilesToSD` unpacks bundled WADs from assets to external storage on first run.
- Resource gotcha: `doom/src/main/res/layout/{fragment_gamepad,controls_listview_item,edit_controls_listview_item}.xml` intentionally **override** same-named layouts in `:touchcontrols` — do not delete them as "unused".

**touchcontrols module** (`com.beloko.touchcontrols`) — beloko/nvllsvm's on-screen touch controls and gamepad handling (`ControlInterpreter`, `ControlConfig`, `TouchControlsEditing`, `GamePadFragment`), plus a vendored drag-sort ListView under `com.mobeta.android.dslv`. Its native side is the `MobileTouchControls` submodule compiled into `libtouchcontrols.so`.

**Native source layout** (`doom/src/main/jni/`): `Android.mk` includes the per-component makefiles — `jwzgles` (GL ES1-over-ES2 shim), the `AudioLibs_OpenTouch` stack (`libsndfile-android`, `libmpg123`, `fluidsynth-lite`, `openal`, `android_external_flac`), `jpeg8d`, `gzdoom_android/mobile`, `SAFFAL`, `MobileTouchControls`, and `SDL`. SDL is an old 1.x branch; OpenAL provides audio output and FluidSynth handles MIDI/soundfont music (no FMOD).

## Notable constraints

- The **native engine** is intentionally frozen/legacy (GZDoom 1.9-era, SDL 1.x, GL ES 1.x); the **app shell** is modern (Kotlin, Compose M3, AGP 9 with built-in Kotlin, compileSdk 36, minSdk 23). When touching the native build it is easy to hit deprecated-tooling issues.
- App-facing strings live in heavily-translated `res/values-*/strings.xml` (Transifex-sourced); prefer editing `res/values/strings.xml` and leaving translations alone.
