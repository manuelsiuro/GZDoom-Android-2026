package net.nullsum.freedoom.ui.editor.model

import kotlinx.serialization.Serializable

/** WAD lump naming format. DOOM2 → MAP01.. ; DOOM1 → ExMy. */
@Serializable
enum class WadFormat { DOOM2, DOOM1 }

/**
 * One map in a project. [tiles] holds [TileType] ordinals row-major (`tiles[y*width+x]`)
 * and may be non-square. A new map is all-Room (ordinal 0 = an empty floor that the
 * converter auto-encloses), which is immediately playable.
 */
@Serializable
data class MapDoc(
    val width: Int = DEFAULT_SIZE,
    val height: Int = DEFAULT_SIZE,
    val tiles: IntArray = IntArray(width * height),
    val theme: MapTheme = MapTheme.Tech,
) {
    fun tileAt(x: Int, y: Int): TileType = TileType.fromOrdinal(tiles[y * width + x])

    // Content-based equals/hashCode: the default for a data class with an array field
    // is identity-based, which would break Compose state diffing and value semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapDoc) return false
        return width == other.width && height == other.height &&
            theme == other.theme && tiles.contentEquals(other.tiles)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + theme.hashCode()
        result = 31 * result + tiles.contentHashCode()
        return result
    }

    companion object {
        const val DEFAULT_SIZE = 16
    }
}

/**
 * High-level generation tuning. The density values (0f..1f) are scaled onto the
 * `[Count.*]` ranges of Preferences.ini by [net.nullsum.freedoom.ui.editor.generate.buildPreferencesIni].
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
 * [net.nullsum.freedoom.ui.editor.data.ProjectStore].
 */
@Serializable
data class MapProject(
    val schemaVersion: Int = 1,
    val name: String = "mymap",
    val format: WadFormat = WadFormat.DOOM2,
    val episode: Int = 1,
    val iwadFile: String = "freedoom2.wad",
    val maps: List<MapDoc> = listOf(MapDoc()),
    val tuning: Tuning = Tuning(),
    val testMapIndex: Int = 0,
    /** When false, the converter spawns only a player start (no monsters/items) — a sandbox map. */
    val generateThings: Boolean = true,
) {
    companion object {
        const val MAX_MAPS = 32
    }
}
