package net.nullsum.freedoom.ui.editor

import net.nullsum.freedoom.ui.editor.MapGridOps.SymmetryMode
import net.nullsum.freedoom.ui.editor.model.TileType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SymmetryReplaceTest {

    @Test
    fun mirror_horizontal() {
        // 4 wide: x=0 mirrors to x=3 (same row).
        assertEquals(setOf(0, 3), MapGridOps.mirrorIndices(4, 4, 0, 0, SymmetryMode.Horizontal).toSet())
    }

    @Test
    fun mirror_vertical() {
        // 4 tall: y=0 mirrors to y=3 (same col 1).
        assertEquals(setOf(1, 3 * 4 + 1), MapGridOps.mirrorIndices(4, 4, 1, 0, SymmetryMode.Vertical).toSet())
    }

    @Test
    fun mirror_bothIs4Way() {
        // (0,0) on a 4x4 → (0,0),(3,0),(0,3),(3,3)
        val got = MapGridOps.mirrorIndices(4, 4, 0, 0, SymmetryMode.Both).toSet()
        assertEquals(setOf(0, 3, 3 * 4, 3 * 4 + 3), got)
    }

    @Test
    fun mirror_onCentreLineDedupes() {
        // Odd width 3, x=1 is the centre column → horizontal mirror is itself.
        assertEquals(setOf(1), MapGridOps.mirrorIndices(3, 3, 1, 0, SymmetryMode.Horizontal).toSet())
    }

    @Test
    fun expandSymmetry_noneIsIdentity() {
        val cells = intArrayOf(0, 5)
        assertArrayEquals(cells, MapGridOps.expandSymmetry(cells, 4, 4, SymmetryMode.None))
    }

    @Test
    fun expandSymmetry_bothQuadruples() {
        val got = MapGridOps.expandSymmetry(intArrayOf(0), 4, 4, SymmetryMode.Both).toSet()
        assertEquals(setOf(0, 3, 12, 15), got)
    }

    @Test
    fun block_size1IsSingleCell() {
        assertArrayEquals(intArrayOf(5), MapGridOps.blockCells(4, 4, 1, 1, 1))
    }

    @Test
    fun block_size3CentredAndClamped() {
        // 3x3 block centred at (0,0) on a 4x4 → only the in-bounds quadrant.
        val got = MapGridOps.blockCells(4, 4, 0, 0, 3).toSet()
        assertEquals(setOf(0, 1, 4, 5), got)
    }

    @Test
    fun block_size3Interior() {
        val got = MapGridOps.blockCells(5, 5, 2, 2, 3).toSet()
        // rows 1..3, cols 1..3
        val expected = buildSet { for (y in 1..3) for (x in 1..3) add(y * 5 + x) }
        assertEquals(expected, got)
    }

    @Test
    fun replaceTile_swapsAllMatches_immutable() {
        val w = TileType.Wall.ordinal
        val d = TileType.Door.ordinal
        val src = intArrayOf(w, 0, w, 0)
        val out = MapGridOps.replaceTile(src, w, d)
        assertArrayEquals(intArrayOf(d, 0, d, 0), out)
        assertArrayEquals(intArrayOf(w, 0, w, 0), src) // input untouched
    }
}
