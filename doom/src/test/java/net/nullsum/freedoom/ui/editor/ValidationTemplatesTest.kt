package net.nullsum.freedoom.ui.editor

import net.nullsum.freedoom.ui.editor.model.MapDoc
import net.nullsum.freedoom.ui.editor.model.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTemplatesTest {

    private fun grid(w: Int, h: Int, vararg cells: Pair<Int, TileType>): MapDoc {
        val t = IntArray(w * h) // Room
        for ((idx, tile) in cells) t[idx] = tile.ordinal
        return MapDoc(w, h, t, net.nullsum.freedoom.ui.editor.model.MapTheme.Tech)
    }

    @Test
    fun validate_cleanMapHasNoWarnings() {
        val m = grid(3, 3, 0 to TileType.Start, 8 to TileType.Exit)
        assertTrue(validateMap(m).isEmpty())
    }

    @Test
    fun validate_missingStartAndExit() {
        val warnings = validateMap(grid(3, 3)) // all Room, no start/exit
        assertTrue(warnings.any { it.contains("player start", ignoreCase = true) })
        assertTrue(warnings.any { it.contains("exit", ignoreCase = true) })
    }

    @Test
    fun validate_allWallsHasNoOpenFloor() {
        val t = IntArray(4) { TileType.Wall.ordinal }
        val warnings = validateMap(MapDoc(2, 2, t, net.nullsum.freedoom.ui.editor.model.MapTheme.Tech))
        assertTrue(warnings.any { it.contains("open floor", ignoreCase = true) })
    }

    @Test
    fun templates_allBuildPlayableMaps() {
        assertEquals(5, MapTemplates.all.size)
        for (tpl in MapTemplates.all) {
            val doc = tpl.build()
            assertEquals("${tpl.name} width", 24, doc.width)
            assertEquals("${tpl.name} height", 24, doc.height)
            assertEquals("${tpl.name} tile count", 24 * 24, doc.tiles.size)
            assertTrue("${tpl.name} has a Start", doc.tiles.any { it == TileType.Start.ordinal })
            assertTrue("${tpl.name} has an Exit", doc.tiles.any { it == TileType.Exit.ordinal })
        }
    }
}
