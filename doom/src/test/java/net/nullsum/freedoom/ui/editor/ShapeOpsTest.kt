package net.nullsum.freedoom.ui.editor

import net.nullsum.freedoom.ui.editor.model.TileType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeOpsTest {

    private val W = TileType.Wall.ordinal
    private val R = TileType.Room.ordinal

    @Test
    fun line_horizontal() {
        val cells = MapGridOps.lineCells(5, 5, 1, 2, 3, 2).toSet()
        assertEquals(setOf(2 * 5 + 1, 2 * 5 + 2, 2 * 5 + 3), cells)
    }

    @Test
    fun line_vertical() {
        val cells = MapGridOps.lineCells(5, 5, 2, 0, 2, 2).toSet()
        assertEquals(setOf(0 * 5 + 2, 1 * 5 + 2, 2 * 5 + 2), cells)
    }

    @Test
    fun line_diagonal() {
        val cells = MapGridOps.lineCells(4, 4, 0, 0, 2, 2).toSet()
        assertEquals(setOf(0, 1 * 4 + 1, 2 * 4 + 2), cells)
    }

    @Test
    fun line_clampsOffGridEndpoints() {
        // From inside to far off-grid: only in-bounds cells returned, none out of range.
        val cells = MapGridOps.lineCells(4, 4, 0, 0, 10, 0)
        assertTrue(cells.all { it in 0 until 16 })
        assertEquals(setOf(0, 1, 2, 3), cells.toSet())
    }

    @Test
    fun rect_outline_3x3() {
        // Outline of a full 3x3 grid = all 8 border cells, center excluded.
        val cells = MapGridOps.rectCells(3, 3, 0, 0, 2, 2, filled = false).toSet()
        assertEquals((0..8).toSet() - setOf(4), cells)
    }

    @Test
    fun rect_filled_3x3() {
        val cells = MapGridOps.rectCells(3, 3, 0, 0, 2, 2, filled = true).toSet()
        assertEquals((0..8).toSet(), cells)
    }

    @Test
    fun rect_cornersAnyOrderAndClamped() {
        // Corners reversed and partly off-grid; clamps to the grid.
        val a = MapGridOps.rectCells(4, 4, 3, 3, 1, 1, filled = true).toSet()
        val b = MapGridOps.rectCells(4, 4, 1, 1, 3, 3, filled = true).toSet()
        assertEquals(a, b)
        val clamped = MapGridOps.rectCells(4, 4, 2, 2, 9, 9, filled = true)
        assertTrue(clamped.all { it in 0 until 16 })
    }

    @Test
    fun drawRect_outline_setsBorderOnly_andIsImmutable() {
        val src = IntArray(9) { R }
        val out = MapGridOps.drawRect(src, 3, 3, 0, 0, 2, 2, TileType.Wall, filled = false)
        val expected = intArrayOf(W, W, W, W, R, W, W, W, W)
        assertArrayEquals(expected, out)
        assertArrayEquals(IntArray(9) { R }, src) // input untouched
    }

    @Test
    fun drawLine_setsCells_andIsImmutable() {
        val src = IntArray(9) { R }
        val out = MapGridOps.drawLine(src, 3, 3, 0, 0, 2, 0, TileType.Wall)
        assertEquals(W, out[0]); assertEquals(W, out[1]); assertEquals(W, out[2])
        assertEquals(R, out[3])
        assertArrayEquals(IntArray(9) { R }, src)
    }
}
