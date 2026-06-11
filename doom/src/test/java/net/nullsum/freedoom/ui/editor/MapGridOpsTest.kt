package net.nullsum.freedoom.ui.editor

import net.nullsum.freedoom.ui.editor.model.TileType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapGridOpsTest {

    private val W = TileType.Wall.ordinal
    private val R = TileType.Room.ordinal

    @Test
    fun clearResize_blanksToRoom() {
        val src = IntArray(4) { W } // 2x2 all wall
        val out = MapGridOps.resizeGrid(src, 2, 2, 3, 3, ResizeMode.Clear)
        assertEquals(9, out.size)
        assertArrayEquals(IntArray(9) { R }, out)
    }

    @Test
    fun cropPad_growKeepsTopLeftAndPadsRoom() {
        // 2x2: [W W ; W W]
        val src = intArrayOf(W, W, W, W)
        val out = MapGridOps.resizeGrid(src, 2, 2, 3, 3, ResizeMode.CropPad)
        // Expect the 2x2 wall block anchored top-left, rest Room.
        val expected = intArrayOf(
            W, W, R,
            W, W, R,
            R, R, R,
        )
        assertArrayEquals(expected, out)
    }

    @Test
    fun cropPad_shrinkCrops() {
        // 3x3 with a marker at (2,2)
        val src = IntArray(9) { R }.also { it[8] = W }
        val out = MapGridOps.resizeGrid(src, 3, 3, 2, 2, ResizeMode.CropPad)
        assertArrayEquals(intArrayOf(R, R, R, R), out) // (2,2) cropped away
    }

    @Test
    fun cropPad_nonSquare() {
        // 2 wide x 3 tall, all wall
        val src = IntArray(6) { W }
        val out = MapGridOps.resizeGrid(src, 2, 3, 4, 2, ResizeMode.CropPad)
        // new 4x2: top-left 2x2 region copied (wall), rest Room
        val expected = intArrayOf(
            W, W, R, R,
            W, W, R, R,
        )
        assertArrayEquals(expected, out)
    }

    @Test
    fun floodFill_fillsConnectedRegionOnly() {
        // 3x3: a Room cross of Walls splitting two Room cells — fill from (0,0)
        // grid:
        // R W R
        // W W W
        // R W R
        val g = intArrayOf(
            R, W, R,
            W, W, W,
            R, W, R,
        )
        val out = MapGridOps.floodFill(g, 3, 3, 0, 0, TileType.Sky)!!
        val S = TileType.Sky.ordinal
        // Only the top-left Room becomes Sky; the other Room corners are not 4-connected to it.
        assertEquals(S, out[0])
        assertEquals(R, out[2])
        assertEquals(R, out[6])
        assertEquals(R, out[8])
    }

    @Test
    fun floodFill_noopWhenSameType() {
        val g = IntArray(4) { R }
        assertNull(MapGridOps.floodFill(g, 2, 2, 0, 0, TileType.Room))
    }

    @Test
    fun floodFill_outOfBoundsReturnsNull() {
        val g = IntArray(4) { R }
        assertNull(MapGridOps.floodFill(g, 2, 2, 5, 5, TileType.Wall))
    }

    @Test
    fun floodFill_doesNotMutateInput() {
        val g = IntArray(4) { R }
        MapGridOps.floodFill(g, 2, 2, 0, 0, TileType.Wall)
        assertArrayEquals(IntArray(4) { R }, g)
    }
}
