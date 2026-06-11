package net.nullsum.freedoom.ui.editor.model

import androidx.compose.ui.graphics.Color

/**
 * The paintable tile types and their canonical RGB encodings.
 *
 * These RGB values are a hard contract with the native png2wad converter:
 * `MapGenerator::GetTileTypeFromPixel` (png2wad-sdk/src/main/cpp/Generator.cpp:286-296)
 * maps each exact colour back to a tile, and **any unrecognised colour falls through
 * to Room**. That is why [Room] is ordinal 0 (a freshly zeroed grid is all-Room) and
 * why the eraser simply paints Room. Do not change these numbers without changing the
 * C++ table in lockstep.
 */
enum class TileType(val displayName: String, val rgb: Int) {
    Room("Room", 0x000000),
    Wall("Wall", 0xFFFFFF),
    SpecialFloor("Sp. Floor", 0xFF0000),
    SpecialCeiling("Sp. Ceiling", 0x008000),
    Sky("Sky", 0x0000FF),
    Door("Door", 0x808000),
    Secret("Secret", 0xFF00FF),
    Start("Start", 0xFFFF00),
    Exit("Exit", 0x00FF00);

    /** Cached opaque Compose colour for rendering the editor canvas and palette swatches. */
    val composeColor: Color = Color(0xFF000000 or rgb.toLong())

    companion object {
        private val byOrdinal = entries.toTypedArray()

        /** Safe lookup used when decoding persisted grids; unknown ordinals degrade to Room. */
        fun fromOrdinal(ordinal: Int): TileType = byOrdinal.getOrElse(ordinal) { Room }
    }
}

/**
 * The map theme. The converter reads the theme from the PNG's top-left pixel colour
 * ([pixelRgb]) and matches it against the `[Themes]` section of Preferences.ini, then
 * overwrites that pixel with white on load. The editor applies the theme colour only
 * at PNG-render time, so the user never has to sacrifice a visible cell.
 */
enum class MapTheme(val displayName: String, val pixelRgb: Int) {
    Tech("Tech", 0xFFFFFF), // [Themes] Default=255,255,255
    Cave("Cave", 0x808080),
    Hell("Hell", 0xFF0000),
    City("City", 0x8080FF);

    val composeColor: Color = Color(0xFF000000 or pixelRgb.toLong())
}
