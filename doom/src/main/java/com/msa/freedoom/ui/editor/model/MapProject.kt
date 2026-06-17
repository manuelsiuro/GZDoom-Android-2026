package com.msa.freedoom.ui.editor.model

import kotlinx.serialization.Serializable

/** WAD lump naming format. DOOM2 → MAP01.. ; DOOM1 → ExMy. */
@Serializable
enum class WadFormat { DOOM2, DOOM1 }

/**
 * A hand-placed map thing (monster, key, item, player start…), addressed in editor
 * grid cells. [type] is a Doom editor number (DoomEdNum, see [ThingCatalog]); the
 * native generator converts ([cellX], [cellY]) → Doom world units at generation time.
 * [flags] is the skill/option bitmask (1|2|4 = all skills = 7).
 *
 * Placement is restricted to *open* cells — a thing on a Wall/Door/Secret/Exit cell
 * would spawn stuck inside geometry (the converter excludes those from its free-tile
 * set). See [TileType.acceptsThing].
 */
@Serializable
data class MapThing(
    val type: Int,
    val cellX: Int,
    val cellY: Int,
    val angle: Int = 0,
    val flags: Int = ALL_SKILLS,
) {
    companion object {
        const val ALL_SKILLS = 7
    }
}

/**
 * One map in a project. [tiles] holds [TileType] ordinals row-major (`tiles[y*width+x]`)
 * and may be non-square. A new map is all-Room (ordinal 0 = an empty floor that the
 * converter auto-encloses), which is immediately playable. [things] are optional
 * hand-placed things layered on top of the procedural ones.
 */
@Serializable
data class MapDoc(
    val width: Int = DEFAULT_SIZE,
    val height: Int = DEFAULT_SIZE,
    val tiles: IntArray = IntArray(width * height),
    val theme: MapTheme = MapTheme.Tech,
    val things: List<MapThing> = emptyList(),
) {
    fun tileAt(x: Int, y: Int): TileType = TileType.fromOrdinal(tiles[y * width + x])

    // Content-based equals/hashCode: the default for a data class with an array field
    // is identity-based, which would break Compose state diffing and value semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapDoc) return false
        return width == other.width && height == other.height &&
            theme == other.theme && tiles.contentEquals(other.tiles) &&
            things == other.things
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + theme.hashCode()
        result = 31 * result + tiles.contentHashCode()
        result = 31 * result + things.hashCode()
        return result
    }

    companion object {
        const val DEFAULT_SIZE = 16
    }
}

/**
 * A texture role the user can override with real textures/flats picked from the IWAD.
 * [iniKey] matches the `Textures.<key>` lines the native converter reads per theme
 * (`ThemeTexture::ToString`, png2wad-sdk Config.h); [isFlat] selects which IWAD list
 * the picker browses (flats for floors/ceilings, composite textures for walls/doors).
 *
 * Note: `DOORTRAK`, `CRATOP1` and `F_SKY1` are fixed structural textures in the C++
 * generator and are intentionally not overridable, so no roles map to them.
 */
enum class TextureRole(val displayName: String, val iniKey: String, val isFlat: Boolean) {
    Wall("Walls", "Wall", false),
    Floor("Floors", "Floor", true),
    Ceiling("Ceilings", "Ceiling", true),
    Door("Door faces", "Door", false),
    DoorSide("Door sides", "DoorSide", false),
    FloorSpecial("Special floors", "FloorSpecial", true),
    CeilingSpecial("Special ceilings", "CeilingSpecial", true),
    WallExterior("Outdoor walls", "WallExterior", false),
    FloorExterior("Outdoor floors", "FloorExterior", true),
    FloorExit("Exit floors", "FloorExit", true);
}

/**
 * High-level generation tuning. The density values (0f..1f) are scaled onto the
 * `[Count.*]` ranges of Preferences.ini by [com.msa.freedoom.ui.editor.generate.buildPreferencesIni].
 * Note the converter further scales thing counts by free-tile area, so the same density
 * yields different absolute counts on different map sizes.
 */
@Serializable
data class Tuning(
    val monsterDensity: Float = 0.5f,
    val itemDensity: Float = 0.5f,
    val ammoDensity: Float = 0.5f,
)

/**
 * A saved editor project: one or more maps bundled into a single WAD, plus the test
 * IWAD, format and tuning. Serialized to JSON by
 * [com.msa.freedoom.ui.editor.data.ProjectStore].
 */
@Serializable
data class MapProject(
    val schemaVersion: Int = SCHEMA_VERSION,
    val name: String = "mymap",
    val format: WadFormat = WadFormat.DOOM2,
    val episode: Int = 1,
    val iwadFile: String = "freedoom2.wad",
    val maps: List<MapDoc> = listOf(MapDoc()),
    val tuning: Tuning = Tuning(),
    val testMapIndex: Int = 0,
    /** When false, the converter spawns only a player start (no monsters/items) — a sandbox map. */
    val generateThings: Boolean = true,
    /** Test-launch skill, 1 (I'm Too Young To Die) … 5 (Nightmare). */
    val skill: Int = 3,
    /**
     * Real texture/flat names chosen by the user per [TextureRole] (keyed by
     * [TextureRole.name]). An empty/absent list keeps the theme default. Applied to
     * every theme block when generating Preferences.ini.
     */
    val textureOverrides: Map<String, List<String>> = emptyMap(),
    /**
     * When true, the procedural monster/item scatter is disabled and only
     * hand-placed [MapDoc.things] appear (the Start tile still spawns the player).
     */
    val manualThings: Boolean = false,
) {
    companion object {
        const val MAX_MAPS = 32
        const val SCHEMA_VERSION = 2
    }
}
