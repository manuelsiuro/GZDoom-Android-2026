# PNG2WAD Map Editor (in-app)

This app embeds a full **map studio**: draw colored grids, generate a Doom WAD
from them, and launch it in the GZDoom engine — all on-device, no PC tools.

It is built on **png2wad** (a PNG→WAD converter, originally a C# tool by
@akaAgar, ported to C/C++ here) wired into the GZDoom-Android launcher as a new
**"editor"** tab.

---

## How to use it

1. Launch the app and swipe to the **editor** tab (rightmost).
2. **Tile palette** (scrollable row): pick a tile type. The whole grid is
   paintable — pick a **tool** and **tap or drag** the canvas:
   - **Brush** paints the selected tile, **Erase** paints Room, **Fill** flood-fills
     a region, **Pan** moves the view with one finger.
   - **Two fingers** always pinch-zoom + pan, so you can work on large grids.
   - **Undo / Redo** (a whole drag = one step).
3. **Size**: pick a square preset (16–64) or a custom non-square W×H; resizing
   keeps your drawing (crop/pad) or clears it.
4. **Theme** (`Tech`/`Cave`/`Hell`/`City`): sets the texture/lighting set; applied
   to the map's top-left pixel at render time (you don't sacrifice a visible cell).
5. **Maps**: a project can hold up to 32 maps (MAP01…MAP32) — add/duplicate/
   reorder/delete, pick which one you edit, and which one **Test** launches.
6. **Tuning**: monster/item/ammo density sliders + Doom II / Doom I format.
7. **Level name** → the output `mods/<name>.wad`; **IWAD picker** chooses the test
   IWAD (freedoom2, doom2, …). The **⋮ → Projects** sheet saves/opens/deletes named
   projects. Your in-progress drawing also **auto-saves** and survives app restarts.
8. Tap **Test** to generate + boot straight into the test map, or **Generate** to
   just write the WAD (a toast shows its name).

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
- Each grid cell = one 64×64 Doom tile (a 16×16 grid ≈ a 1024×1024-unit map).
- You don't need to draw a wall border — the map is automatically closed at its
  edges. Monsters, items and a player start are placed automatically (controlled
  by the generated `Preferences.ini`).

---

## How it works (pipeline)

```
editor project (1..32 maps) ──► one W×H PNG per map (cacheDir/editor_gen/map_NN.png)
            ──► Png2WadConverter.generateWad([png...], out.wad, Preferences.ini)  [JNI → libpng2wad.so]
            ──► <base>/mods/<level-name>.wad   (a nodeless Doom-format PWAD, MAP01..MAPnn)
            ──► Intent → Game: "-iwad <chosen>.wad -file mods/<name>.wad +map MAP0n"
            ──► GZDoom builds nodes/blockmap on load and runs the map
```

- The converter writes a **nodeless PWAD** (THINGS, LINEDEFS, SIDEDEFS, VERTEXES,
  SECTORS). GZDoom builds SEGS/SSECTORS/NODES/BLOCKMAP/REJECT itself on load, so
  no external node builder (ZDBSP) is needed or run.
- The test IWAD is chosen in-editor (defaults to `freedoom2.wad`); Freedoom reuses
  the classic Doom 2 texture/flat names the themes reference, so textures resolve.
- The `Preferences.ini` is generated per project: the `[Themes]`/`[Theme.*]` blocks
  are the engine-tested defaults, while `[Options]`, format, and the `[Count.*]`
  thing ranges come from the project's tuning sliders.

### Key code

| Piece | Location |
|-------|----------|
| Editor screen (M3 shell) | `doom/src/main/java/net/nullsum/freedoom/ui/editor/MapEditorScreen.kt` |
| State holder (project + viewport + undo + autosave) | `ui/editor/MapEditorState.kt` |
| Interactive canvas (gestures + zoom/pan) | `ui/editor/MapCanvas.kt` |
| Grid ops (resize, flood-fill) + undo | `ui/editor/MapGridOps.kt`, `ui/editor/UndoManager.kt` |
| Data model + tiles (canonical RGB) | `ui/editor/model/MapProject.kt`, `ui/editor/model/Tiles.kt` |
| Project persistence (JSON) | `ui/editor/data/ProjectStore.kt` |
| INI / PNG / WAD generation | `ui/editor/generate/{PreferencesIni,MapPngRenderer,WadGenerator}.kt` |
| Launch (reuses the Game contract) | `ui/editor/launch/EditorLauncher.kt`, `ui/launch/{LaunchArgs,IwadScan}.kt` |
| Tab wiring + hoisted state | `ui/MainScreen.kt` |
| JNI wrapper | `com.doomandroid.png2wad.Png2WadConverter` (in the `:png2wad-sdk` module) |
| Native converter (C/C++) | `png2wad-sdk/src/main/cpp/` (see "Module layout" below) |

---

## Module layout

The native converter is the **`:png2wad-sdk`** Gradle module, **vendored in this
repo** under [`png2wad-sdk/`](png2wad-sdk/) — no sibling checkout required:

```kotlin
// settings.gradle.kts
include(":png2wad-sdk")
project(":png2wad-sdk").projectDir = file("png2wad-sdk")

// doom/build.gradle.kts
implementation(project(":png2wad-sdk"))
```

The module is a standard `com.android.library` (compileSdk 36, minSdk 23) whose
`src/main/cpp/` holds the C/C++ converter (`CMakeLists.txt`, the png2wad sources,
plus a bundled `zdbsp`/`zlib`) and whose `src/main/java/` holds the
`com.doomandroid.png2wad.Png2WadConverter` JNI wrapper. Gradle runs the CMake
build automatically and packages `libpng2wad.so` (arm64-v8a + armeabi-v7a) into
the APK next to the engine libs.

Upstream sources: **png2wad** by [@akaAgar](https://github.com/akaAgar/png2wad)
(GPLv3 — see [`png2wad-sdk/LICENSE`](png2wad-sdk/LICENSE)). The standalone
`AndroidPng2WadApplication` editor app is **no longer needed** — its editor lives
here.

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

**Texture lists must be `;`-separated, not `,`.** The native INI parser splits
*string* arrays on `;` and *numeric* arrays on `,` (`GetArraySeparator` in
`png2wad-sdk/src/main/cpp/INIFile.h`). The legacy inline `DEFAULT_PREFERENCES`
wrote `Textures.Floor=FLAT1_1,FLAT1_2,…`, so the parser read the whole list as one
8-char-truncated name (`FLAT1_1,`) — every generated map had **missing floor/wall
textures** (the blue/black "unknown texture" checkerboard). `buildPreferencesIni`
now emits the `Textures.*` lines with semicolons, so textures resolve in-engine.
(The `Types.MonstersMedium` vs `Count.MonstersAverage` key mismatch was fixed the
same way — the C++ side keys both off `ToString(ThingCategory)` == `MonstersAverage`.)

---

## Extending it

- **Bigger grids / limits**: `MIN_GRID`/`MAX_GRID` and `GRID_PRESETS` in
  `MapEditorState.kt`; `MapProject.MAX_MAPS` caps the map count.
- **Themes / textures / monster sets**: the baseline `[Themes]`/`[Theme.*]`/`Types.*`
  blocks live in `generate/PreferencesIni.kt`; the density→`[Count.*]` mapping is
  `scaleRange()` there.
- **Density curve / per-theme texture overrides**: extend `Tuning`
  (`model/MapProject.kt`) and `buildPreferencesIni`.
- **Import a PNG as a map, thumbnails in the map strip**: feasible — the renderer
  already round-trips a `MapDoc` ⇄ PNG (`generate/MapPngRenderer.kt`).
