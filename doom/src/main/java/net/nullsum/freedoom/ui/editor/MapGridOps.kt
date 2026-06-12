package net.nullsum.freedoom.ui.editor

import net.nullsum.freedoom.ui.editor.model.TileType

/** How [resizeGrid] treats existing content when the grid dimensions change. */
enum class ResizeMode { Clear, CropPad }

/**
 * Pure grid helpers operating on a row-major tile-ordinal array (`tiles[y*width+x]`).
 * All functions return a NEW array so callers can snapshot the previous state for undo.
 */
object MapGridOps {

    /**
     * Returns a [newW]×[newH] grid. [ResizeMode.Clear] blanks to Room; [ResizeMode.CropPad]
     * copies the overlapping top-left region (anchoring the Start corner) and pads any new
     * area with Room, cropping anything that no longer fits.
     */
    fun resizeGrid(
        tiles: IntArray,
        width: Int,
        height: Int,
        newW: Int,
        newH: Int,
        mode: ResizeMode,
    ): IntArray {
        val out = IntArray(newW * newH) // zero == TileType.Room.ordinal
        if (mode == ResizeMode.Clear) return out
        val copyW = minOf(width, newW)
        val copyH = minOf(height, newH)
        for (y in 0 until copyH) {
            for (x in 0 until copyW) {
                out[y * newW + x] = tiles[y * width + x]
            }
        }
        return out
    }

    /**
     * 4-connected flood fill starting at ([x],[y]), replacing the contiguous region of the
     * starting tile's type with [replacement]. Iterative (an explicit index stack) so a
     * full-grid fill can't overflow the call stack. Returns null when it would be a no-op
     * (out of bounds, or the start already equals [replacement]).
     */
    fun floodFill(
        tiles: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        replacement: TileType,
    ): IntArray? {
        if (x !in 0 until width || y !in 0 until height) return null
        val target = tiles[y * width + x]
        val repl = replacement.ordinal
        if (target == repl) return null

        val out = tiles.copyOf()
        val stack = ArrayDeque<Int>()
        stack.addLast(y * width + x)
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            if (out[idx] != target) continue
            out[idx] = repl
            val cx = idx % width
            val cy = idx / width
            if (cx > 0) stack.addLast(idx - 1)
            if (cx < width - 1) stack.addLast(idx + 1)
            if (cy > 0) stack.addLast(idx - width)
            if (cy < height - 1) stack.addLast(idx + width)
        }
        return out
    }

    /**
     * The in-bounds cell indices (`y*width+x`) on the Bresenham line from ([x0],[y0]) to
     * ([x1],[y1]). Endpoints outside the grid are tolerated — only the cells that fall inside
     * are returned.
     */
    fun lineCells(width: Int, height: Int, x0: Int, y0: Int, x1: Int, y1: Int): IntArray {
        val out = ArrayList<Int>()
        var x = x0
        var y = y0
        val dx = kotlin.math.abs(x1 - x0)
        val dy = -kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            if (x in 0 until width && y in 0 until height) out.add(y * width + x)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
        return out.toIntArray()
    }

    /**
     * The in-bounds cell indices of a rectangle spanning the two corners. [filled] returns the
     * solid box; otherwise just its 1-cell-thick outline. Corners may be given in any order and
     * may lie partly off-grid (clamped to the grid).
     */
    fun rectCells(width: Int, height: Int, x0: Int, y0: Int, x1: Int, y1: Int, filled: Boolean): IntArray {
        val minX = minOf(x0, x1)
        val maxX = maxOf(x0, x1)
        val minY = minOf(y0, y1)
        val maxY = maxOf(y0, y1)
        val out = ArrayList<Int>()
        for (y in maxOf(minY, 0)..minOf(maxY, height - 1)) {
            for (x in maxOf(minX, 0)..minOf(maxX, width - 1)) {
                val edge = x == minX || x == maxX || y == minY || y == maxY
                if (filled || edge) out.add(y * width + x)
            }
        }
        return out.toIntArray()
    }

    /** Copies [tiles] and stamps [tile] along the Bresenham line; returns the new array. */
    fun drawLine(tiles: IntArray, width: Int, height: Int, x0: Int, y0: Int, x1: Int, y1: Int, tile: TileType): IntArray {
        val out = tiles.copyOf()
        val v = tile.ordinal
        for (idx in lineCells(width, height, x0, y0, x1, y1)) out[idx] = v
        return out
    }

    /** Copies [tiles] and stamps [tile] over the rectangle (outline or [filled]); returns the new array. */
    fun drawRect(tiles: IntArray, width: Int, height: Int, x0: Int, y0: Int, x1: Int, y1: Int, tile: TileType, filled: Boolean): IntArray {
        val out = tiles.copyOf()
        val v = tile.ordinal
        for (idx in rectCells(width, height, x0, y0, x1, y1, filled)) out[idx] = v
        return out
    }
}
