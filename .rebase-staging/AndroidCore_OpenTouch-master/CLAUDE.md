# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AndroidCore_OpenTouch is an Android library (AAR) providing reusable infrastructure for porting games to Android. It supports multiple game engines (GZDoom, Raze, ioquake3, and 40+ others) and provides touch controls, gamepad mapping, audio backend selection, and file system abstraction.

- **Min SDK:** 19 (Android 4.4 KitKat)
- **Target/Compile SDK:** 35 (Android 15)
- **Languages:** Java (primary) + Kotlin
- **Namespace:** `com.opentouchgaming.androidcore`

## Build Commands

```bash
./gradlew build        # Build the library
./gradlew clean        # Clean build artifacts
./gradlew assembleRelease  # Build release AAR
```

There are no tests in this project.

## Architecture

### Key Abstractions

**`GameEngine`** — Data class + enum listing all supported engines (~40+). Each engine has icons, colors, display name, and optional arguments. Engine-specific behavior is delegated via `EngineOptionsInterface`.

**`EngineOptionsInterface`** — Strategy interface implemented per-engine to provide settings dialogs and runtime configuration. Keeps engine-specific logic out of core library.

**`GameLauncherInterface`** — Interface implemented by host apps to provide subgame directories and command-line arguments.

**`LaunchIntent`** (`com.opentouchgaming.androidcore.common`) — Utility that centralizes populating an `Intent` with all game launch parameters (engine, args, audio backend, etc.).

### Initialization Flow

Host app calls `AppInfo.setAppInfo()` → sets up directories, storage mode, engine list → user configures via `MainFragment` + `EngineOptionsInterface` → `LaunchIntent.populateIntent()` packages everything → `SDLActivity` launches.

### SDL Layer (`org.libsdl.app2012`)

- `SDLActivity` — Base activity integrating LibSDL; host apps extend this
- `SDLAudioManager` — Selects audio backend (OpenAL, OpenSL, OBOE, AAudio, SDL)
- `SDLControllerManager` — Hardware gamepad input management
- `SDLOpenTouch*` classes — Touch input system

### Control System (`controls/`)

- `ControlConfig` — Serializable button/axis mapping configuration
- `ActionInputDefinition` / `ActionInput` — Define available game actions per engine
- `ControlInterpreter` — Translates hardware events to game input at runtime

### Settings & Storage

- `AppInfo` — Static singleton; app directories, scoped storage config
- `AppSettings` / `TouchSettings` — SharedPreferences wrappers
- `ScopedStorage` — Handles Android R+ scoped storage; legacy path is `/OpenTouch/[app_name]/`
- Dual-path support: primary internal + secondary SD card

### UI Layer

- `MainFragment` — Primary fragment coordinating engine list, tools, and sub-games tabs
- `OptionsDialogKt` — Kotlin object defining all app-level settings keys and UI
- Reusable Kotlin widgets in `ui/widgets/`: `SwitchWidget`, `SpinnerWidget`, `RadioLayoutWidget`, `TabLayoutWidget`

## Important Notes

- Glide is pinned to 4.16.0 for Nvidia Shield TV compatibility — do not upgrade without testing on Shield.
- `constraintlayout` is capped at 1.1.3 and `appcompat` at 1.6.1 to maintain API 19 support.
- The project uses `jcenter()` (deprecated) — migration to Maven Central may be needed for new dependencies.
- ViewBinding is enabled; use it instead of `findViewById`.
