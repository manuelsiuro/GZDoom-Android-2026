# PNG2WAD Map Editor (in-app)

This app embeds a small **map editor**: draw a colored grid, generate a Doom
map from it, and launch it in the GZDoom engine — all on-device, no PC tools.

It is built on **png2wad** (a PNG→WAD converter, originally a C# tool by
@akaAgar, ported to C/C++ here) wired into the GZDoom-Android launcher as a new
**"editor"** tab.

---

## How to use it

1. Launch the app and swipe to the **editor** tab (rightmost).
2. **Theme** (top row): pick `Tech` / `Cave` / `Hell` / `City`. This sets the
   texture/lighting theme and is stored in the map's top-left pixel.
3. **Tile palette**: pick a tile type, then **tap or drag** on the 16×16 grid to
   paint. The top-left cell is reserved for the theme and can't be painted.
4. Tap **Generate & Play** — the map is built and GZDoom boots straight into it
   (`MAP01`). Or **Generate only** to just write the WAD (a toast shows its path).

### Tile palette → map meaning

| Tile         | Color (RGB)      | Becomes in the map |
|--------------|------------------|--------------------|
| Wall         | 255,255,255      | Solid wall (impassable) |
| Room         | 0,0,0            | Normal floor/room |
| Sp. Floor    | 255,0,0          | Room with the theme's "special floor" (e.g. nukage/lava) |
| Sp. Ceiling  | 0,128,0          | Room with the theme's "special ceiling" |
| Sky          | 0,0,255          | Open/exterior room (sky ceiling) |
| Door         | 128,128,0        | A working door |
| Secret       | 255,0,255        | Secret area |
| Start        | 255,255,0        | Player start / entrance |
| Exit         | 0,255,0          | Exit trigger (ends the level) |

Notes:
- Each grid cell = one 64×64 Doom tile. The 16×16 grid is a ~1024×1024-unit map.
- You don't need to draw a wall border — the map is automatically closed at its
  edges. Monsters, items and a player start are placed automatically (controlled
  by the embedded `Preferences.ini`).

---

## How it works (pipeline)

```
editor grid ──► 16×16 PNG (cacheDir/temp_map.png)
            ──► Png2WadConverter.generateWad(png, out.wad, Preferences.ini)   [JNI → libpng2wad.so]
            ──► <base>/mods/generated.wad   (a nodeless Doom-format PWAD)
            ──► Intent → Game: "-iwad freedoom2.wad -file mods/generated.wad +map MAP01"
            ──► GZDoom builds nodes/blockmap on load and runs the map
```

- The converter writes a **nodeless PWAD** (THINGS, LINEDEFS, SIDEDEFS, VERTEXES,
  SECTORS). GZDoom builds SEGS/SSECTORS/NODES/BLOCKMAP/REJECT itself on load, so
  no external node builder (ZDBSP) is needed or run.
- The IWAD is **Freedoom** (`freedoom2.wad`), which reuses the classic Doom 2
  texture/flat names the themes reference, so textures resolve.

### Key code

| Piece | Location |
|-------|----------|
| Editor UI + Generate & Play | `doom/src/main/java/net/nullsum/freedoom/ui/editor/MapEditorScreen.kt` |
| Tab wiring (5th tab) | `doom/src/main/java/net/nullsum/freedoom/ui/MainScreen.kt` |
| Launch arg builder (reused) | `doom/src/main/java/net/nullsum/freedoom/ui/launch/LaunchArgs.kt` |
| JNI wrapper | `com.doomandroid.png2wad.Png2WadConverter` (in the `:png2wad-sdk` module) |
| Native converter (C/C++) | `../png2wad/android/src/main/cpp/` (see "Module layout" below) |

---

## Module layout & the dependency on `../png2wad`

The native converter is the **`:png2wad-sdk`** Gradle module, included by path:

```kotlin
// settings.gradle.kts
include(":png2wad-sdk")
project(":png2wad-sdk").projectDir = file("../png2wad/android")

// doom/build.gradle.kts
implementation(project(":png2wad-sdk"))
```

> ⚠️ **This project depends on the sibling `../png2wad` repo** for the native
> sources. Keep `png2wad` checked out next to this repo. The standalone
> `AndroidPng2WadApplication` editor app is **no longer needed** — its editor now
> lives here.
>
> To make this repo fully self-contained later, copy `png2wad/android` into this
> project (e.g. `:png2wad-sdk` as a real submodule/dir) and update the
> `projectDir` line. The C++ sources, `CMakeLists.txt`, and `Png2WadConverter.kt`
> are all that's required.

Gradle runs the SDK's CMake build automatically and packages `libpng2wad.so`
(arm64-v8a + armeabi-v7a) into the APK next to the engine libs.

---

## Build & run

```bash
# Native engine libs are prebuilt and committed; you normally only need:
./gradlew :doom:assembleDebug      # also compiles libpng2wad.so via CMake
adb install -r doom/build/outputs/apk/debug/doom-debug.apk
```

(Only re-run `./build_native.sh` if you change the GZDoom engine under
`doom/src/main/jni/**`; it is unrelated to the map editor / png2wad.)

### Logs

The converter logs every stage under the `png2wad` tag:

```bash
adb logcat -s png2wad
# PNG size, theme, sub-grid, sector/vertex/linedef/thing counts, each lump+size
```

---

## Why this works now (history / gotcha)

The generated WAD originally **failed to load** in this engine. Root cause: this
build is **GZDoom 1.9.0** (despite the "3.8.2" folder name), whose map loader
(`GetMapIndex` in `…/gzdoom_android/src/p_setup.cpp`) enforces a **strict lump
order** — the first lump after the `MAP01` marker must be `THINGS`, otherwise it
aborts with `I_Error("'THINGS' not found …")`. The converter was emitting
`LINEDEFS` first. It now writes the canonical order
`THINGS, LINEDEFS, SIDEDEFS, VERTEXES, SECTORS`.
(Modern desktop GZDoom 3.x reads lumps order-independently, which is why the
original tool "worked" on PC but not here.)

Two related converter fixes: `MapSubWidth/Height` were swapped (broke non-square
images), and the sector flood-fill now includes the map border (borderless
editor maps previously produced hundreds of empty "orphan" sectors).

---

## Extending it

- **Map size**: change `gridSize` in `MapEditorScreen.kt` (and the canvas is
  square; png2wad supports non-square too).
- **Themes / textures / monster sets**: edit the embedded `DEFAULT_PREFERENCES`
  string in `MapEditorScreen.kt` (it mirrors png2wad's `Preferences.ini`).
- **IWAD / starting map**: `launchGeneratedMap()` hardcodes
  `-iwad freedoom2.wad … +map MAP01`; make these selectable if needed.
- **Save/load drawings, multiple maps (MAP01..MAP32), import a PNG**: all
  feasible — the converter already accepts multiple PNGs and emits `MAPnn`.
