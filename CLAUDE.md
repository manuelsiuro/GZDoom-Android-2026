# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An Android port of the GZDoom engine bundled with the open-source Freedoom assets, published as "Freedoom for Android" (`com.msa.freedoom`). It is a fork of nvllsvm's GZDoom-Android. The app is a thin Java/Kotlin shell around a large C/C++ engine compiled via the NDK.

## Build

The native engine and the Android app are built in **two separate, sequential steps**. Gradle does *not* drive the native build (there is no `externalNativeBuild` block); instead `ndk-build` produces `.so` files into `doom/src/main/libs`, which Gradle picks up via `jniLibs.srcDirs("src/main/libs")`.

```bash
ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/27.0.12077973 ./build_native.sh   # ndk-build → .so
./gradlew :doom:assembleDebug                                                # APK = Kotlin + .so
```

- `build_native.sh` auto-locates `ndk-build` (honours `ANDROID_NDK_HOME`, else the highest NDK under the SDK) and runs it with `jni/Application.mk`. The committed `.so` under `doom/src/main/libs/{armeabi-v7a,arm64-v8a}/` mean the app **already assembles + runs without re-running the native build**; only re-run it if you change `doom/src/main/jni/**`.
- Native target ABIs are **`armeabi-v7a` and `arm64-v8a`** (`doom/src/main/jni/Application.mk`).
- Toolchain: **Java 17** (`gradle.properties` pins `org.gradle.java.home` to a local Temurin 17 path — adjust per machine); **NDK r27** for the native build. `local.properties` (gitignored) must point `sdk.dir` at your SDK.
- `Application.mk` carries three load-bearing settings: `APP_STL := c++_shared`, `-DNO_SEC` (disables the Delta Touch licence-check code in the vendored glue), and **`-Wl,-z,nostart-stop-gc`** — GZDoom registers CVARs/classes/actions in custom ELF sections (`areg`/`creg`/`vreg`/…) walked via `__start_*`/`__stop_*`; modern lld + the NDK's default `--gc-sections` strips those entries silently and the engine crashes on the first CVAR access. Do not remove that flag.
- Unit tests: `./gradlew :doom:testDebugUnitTest` (guards the launch command line). JNI seam signatures are golden-tested: `bash .jni-golden/verify.sh` after a debug build (`--update` to regenerate).

## The native engine (UZDoom 5.0.0-pre, emileb's uz_5.0_pre / GZDoom 4.15 fork)

The native layer is **rebased onto emileb's maintained UZDoom mobile port** (UZDoom 5.0.0-pre, [`github.com/emileb/gzdoom`](https://github.com/emileb/gzdoom) branch `uz_5.0_pre`, a GZDoom 4.15-derived Delta Touch engine) plus his OpenTouch support libraries. The engine reports `GAMENAME "UZDoom"` / `VERSIONSTR "5.0.0-pre"` (see `engine/gzdoom/src/version.h`). Everything is **vendored** under `doom/src/main/jni/` (no submodules):

- `engine/gzdoom/` — the engine (nested one level so the glue's `../../../Clibs_OpenTouch/...` relative paths resolve to the jni root). Its `mobile/Android.mk` pulls in `lzma`, `bzip2`, `glslang`, `zwidget`, ZMusic (from `libraries/ZMusic`) and the engine module `gzdoom`.
- `Clibs_OpenTouch/` — emileb's shared JNI glue (`android_jni_inc.cpp`, `idtech1/` game interface, `port_act_defs.h`). The Google Play licence check lives behind `#ifndef NO_SEC` (we define `NO_SEC`). Also provides `logwritter` and the prebuilt `vpx_player`.
- `SDL2_OpenTouch/` — SDL 2.0.12 fork built with `OPENTOUCH_SDL_EXTRA` (beloko extras: swap-buffer callback, mouse injection; Java prefix `org.libsdl.app2012`). Plus `SDL2_net`.
- `MobileTouchControls/` — current touch-controls library (+ libpng/libzip/sigc++/TinyXML). Calls back into `com.beloko.touchcontrols.*` and `org.libsdl.app.NativeConsoleBox` Java by name.
- `AudioLibs_OpenTouch/` — `openal` (source build, OpenSL backend), `libsndfile`, `libmpg123`, `fluidsynth-lite`, flac (ZMusic deps). No FMOD.
- `SAFFAL/` — scoped-storage file-access layer (libsaffal + Java `com.opentouchgaming.saffal`, vendored into the app).
- `jpeg8d/`, `Android_webp.mk` (builds `webpmux` from `engine/gzdoom/libraries/webp` — our makefile, upstream only ships CMake).

Local patches to the vendored engine (grep for these before re-vendoring a newer branch): `ALooper_pollAll→ALooper_pollOnce` (NDK r27, SDL sensor), `-DUZDOOM` (selects the `Mobile_IN_Move(usercmd_t*)` glue signature), crashcatcher disabled on Android (`i_main.cpp` — it fork()s a debugger and deadlocks), ES 3.1→3.0 context fallback in `sdlglvideo.cpp`, module renamed `g`→`gzdoom`, `core_shared` dropped from the link line (not needed), webp include path fixed.

**Engine data**: UZDoom 5.0 (`__MOBILE__`) loads `<game_path>/res/uzdoom.pk3` (BASEWAD) + `res/uzdoom_game_support.pk3` (OPTIONALWAD, carries IWADINFO). These are built from the engine's own `wadsrc*/static` trees (plain zips) and bundled in `doom/src/main/assets/` together with `lights.pk3`, `brightmaps.pk3`, `game_widescreen_gfx.pk3` and the engine's `gzdoom.sf2`; `LaunchState.launchGame()` unpacks them. If you bump the engine, rebuild the pk3s from the matching `wadsrc` or the engine errors at boot. Touch-control art is `doom/src/main/assets/*.png` + `font_dual.png` (a generated 16×16-cell glyph atlas; glyph spacing is auto-derived from alpha).

## Architecture

**Gradle modules** (`settings.gradle.kts`): `:doom` (the application) depends on `:touchcontrols` (an Android library). The version catalog lives in `gradle/libs.versions.toml`; dependency repos are locked via `FAIL_ON_PROJECT_REPOS`.

**JNI bridge.** `org.libsdl.app.NativeLib` (Java) is the engine seam: the glue's `JAVA_FUNC(x)` expands to `Java_org_libsdl_app_NativeLib_##x`. `init(...)` **never returns** — the engine main loop owns the calling thread. SDL2's Java glue is the vendored `org.libsdl.app2012` package (`SDLActivity` + `SDL`, `SDLAudioManager`, `SDLControllerManager`, HID classes); SDL owns the surface/EGL. `org.libsdl.app.SDLOpenTouch` is **our** minimal replacement for emileb's AndroidCore hook class: `Setup` (creates the beloko `ControlInterpreter`), `RunApplication` (builds args — forces `vid_preferbackend 2` (GLES), `vid_fullscreen 1`, `vid_defwidth/height` from the surface — then calls `NativeLib.init`), `surfaceChanged` (res-div via `setFixedSize`), `onTouchEvent`/`onKey` routing, and `CommandHandler` (handles `COMMAND_EXIT_APP` 0x8007 from the native `exit()` override by killing the process). `NativeLib` implements beloko's `ControlInterface` (same `PORT_ACT_*` numbering as `Clibs_OpenTouch/port_act_defs.h`). Signature drift breaks at runtime, not compile time — keep `.jni-golden/verify.sh` green.

**App flow (Kotlin under `doom/src/main/java/com/msa/freedoom`):** the launcher UI is **Jetpack Compose Material 3** (dark Doom theme in `ui/theme/`, BOM via `org.jetbrains.kotlin.plugin.compose` — note `material-icons-core` is an explicit dependency and three extra icons are hand-inlined in `ui/DoomIcons.kt`).
- `EntryActivity` — launcher activity; loads `AppSettings`, then `setContent { DoomTheme { MainScreen() } }`. Still an `AppCompatActivity`: it forwards `onKeyDown/onKeyUp/onGenericMotionEvent` to `GamePadFragment` found via `supportFragmentManager`.
- `ui/MainScreen.kt` — `PrimaryTabRow` + `HorizontalPager` with **`beyondViewportPageCount = 2`** (keeps the gamepad fragment resident so input forwarding works from any tab — do not remove). Tab 1 hosts `GamePadFragment` via `androidx.fragment.compose.AndroidFragment` interop; tabs 0/2 are pure Compose.
- `ui/launch/` — `LaunchScreen` (two-pane: WAD cards + launch panel with mod chips and args field), `ModPickerSheet` (bottom-sheet add-on browser over `wads/`+`mods/`), `LaunchState` (state holder; first-run asset unpack + per-launch engine-data unpack into `res/`), `LaunchArgs.kt` (pure `buildLaunchArgs`/`buildModArgs` — **must stay byte-identical to the legacy command line**; guarded by `doom/src/test/.../LaunchArgsTest.kt`; engine-version-specific cvars are appended in `SDLOpenTouch.RunApplication`, not here). Selection persists to `last_iwad` (int, legacy) + `last_iwad_name` (preferred).
- `ui/options/` — `OptionsScreen` (base dir choose/reset/SD-card, resolution divider → `gzdoom_res_div`), `DirectoryPickerDialog`.
- `Game` — the gameplay activity, now a thin subclass of `org.libsdl.app2012.SDLActivity`: overrides `getLibraries()` (`hidapi, saffal, openal, zmusic_uz, touchcontrols, SDL2, uzdoom`) and kills the process on destroy (engine can't re-init in-process; the activity runs in the separate `:Game` process). Launch contract from the UI is unchanged: Intent extras `args`, `game_path`, `res_div`, `game`.
- `AppSettings` — static `SharedPreferences` wrapper; holds the Freedoom base dir (default `<app external files>/Freedoom`), music dir, graphics dir (`filesDir`), and toggles. `Utils.copyFreedoomFilesToSD` unpacks bundled WADs from assets on first run.
- Resource gotcha: `doom/src/main/res/layout/{fragment_gamepad,controls_listview_item,edit_controls_listview_item}.xml` intentionally **override** same-named layouts in `:touchcontrols` — do not delete them as "unused".

**touchcontrols module** (`com.beloko.touchcontrols`) — beloko/nvllsvm's Java layer for gamepad mapping and config UI (`ControlInterpreter`, `ControlConfig`, `TouchControlsEditing`, `GamePadFragment`), plus a vendored drag-sort ListView under `com.mobeta.android.dslv`. The on-screen touch controls themselves are now rendered **natively by the engine glue** (`Clibs_OpenTouch` + `MobileTouchControls`), which still calls back into these Java classes by name (`TouchControlsEditing` natives, `ShowKeyboard`, `CustomCommands`) — the action-code numbering (`PORT_ACT_*`) matches the native `port_act_defs.h`.

## Notable constraints

- The app shell is modern (Kotlin, Compose M3, AGP 9 with built-in Kotlin, compileSdk 36, app `minSdk 33` — the `:touchcontrols`/`:png2wad-sdk` library modules and `Application.mk` `APP_PLATFORM` are android-23); the engine is UZDoom 5.0 (GZDoom 4.15-derived) with C++17. NDK r27 quirks are handled in `Application.mk` — read it before changing toolchain settings.
- **Known issue (emulator-only as far as observed):** on the Android emulator (both `-gpu auto`/SwiftShader and `-gpu host` GL translation) the engine's framebuffer present renders black — touch controls, engine UI windows and (under the GL3 backend) weapon sprites are visible, and the game runs/accepts input/plays sound. Needs verification on real hardware (previous engine also had emulator-only artifacts that were absent on a real S24). If black on device too: compare against Delta Touch flags (`gles_force_glsl_v100`, `-gles2_renderer`, `gl_pipeline_depth`, `USE_GL_HW_BUFFERS`).
- App-facing strings live in heavily-translated `res/values-*/strings.xml` (Transifex-sourced); prefer editing `res/values/strings.xml` and leaving translations alone.
