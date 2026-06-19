package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.MapDoc
import com.msa.freedoom.ui.editor.model.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTemplatesTest {

    private fun grid(w: Int, h: Int, vararg cells: Pair<Int, TileType>): MapDoc {
        val t = IntArray(w * h) // Room
        for ((idx, tile) in cells) t[idx] = tile.ordinal
        return MapDoc(w, h, t, com.msa.freedoom.ui.editor.model.MapTheme.Tech)
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
        val warnings = validateMap(MapDoc(2, 2, t, com.msa.freedoom.ui.editor.model.MapTheme.Tech))
        assertTrue(warnings.any { it.contains("open floor", ignoreCase = true) })
    }

    @Test
    fun templates_allBuildPlayableMaps() {
        assertTrue("expected a richer library", MapTemplates.all.size >= 12)
        for (tpl in MapTemplates.all) {
            val doc = tpl.build()
            assertEquals("${tpl.name} tile count", doc.width * doc.height, doc.tiles.size)
            assertTrue(
                "${tpl.name} ordinals in range",
                doc.tiles.all { it in 0..TileType.entries.lastIndex },
            )
            // Playable = no advisory warnings: has a Start, an Exit, open floor, no stranded things.
            assertTrue(
                "${tpl.name} should have no warnings but got ${validateMap(doc)}",
                validateMap(doc).isEmpty(),
            )
        }
    }

    @Test
    fun templates_groupingCoversEverything() {
        assertEquals(MapTemplates.all.size, MapTemplates.byCategory.values.sumOf { it.size })
    }
}
