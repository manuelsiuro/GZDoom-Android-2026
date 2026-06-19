package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.TileType

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

    /** How edits are mirrored across the grid's centre. */
    enum class SymmetryMode { None, Horizontal, Vertical, Both }

    /**
     * The cell ([x],[y]) plus its mirror partners under [mode] — Horizontal mirrors left↔right
     * (`w-1-x`), Vertical top↔bottom (`h-1-y`), Both is 4-way. Out-of-range inputs and duplicate
     * partners (on a centre line) are dropped.
     */
    fun mirrorIndices(width: Int, height: Int, x: Int, y: Int, mode: SymmetryMode): List<Int> {
        if (x !in 0 until width || y !in 0 until height) return emptyList()
        val xs = if (mode == SymmetryMode.Horizontal || mode == SymmetryMode.Both) intArrayOf(x, width - 1 - x) else intArrayOf(x)
        val ys = if (mode == SymmetryMode.Vertical || mode == SymmetryMode.Both) intArrayOf(y, height - 1 - y) else intArrayOf(y)
        val out = LinkedHashSet<Int>()
        for (yy in ys) for (xx in xs) out.add(yy * width + xx)
        return out.toList()
    }

    /** Union of every cell in [cells] with its [mode] mirror partners. */
    fun expandSymmetry(cells: IntArray, width: Int, height: Int, mode: SymmetryMode): IntArray {
        if (mode == SymmetryMode.None) return cells
        val out = LinkedHashSet<Int>()
        for (idx in cells) {
            out.addAll(mirrorIndices(width, height, idx % width, idx / width, mode))
        }
        return out.toIntArray()
    }

    /** The in-bounds cell indices of a [size]×[size] square centred on ([x],[y]) (brush tip). */
    fun blockCells(width: Int, height: Int, x: Int, y: Int, size: Int): IntArray {
        if (size <= 1) return if (x in 0 until width && y in 0 until height) intArrayOf(y * width + x) else IntArray(0)
        val half = (size - 1) / 2
        val out = ArrayList<Int>(size * size)
        for (dy in -half until size - half) {
            for (dx in -half until size - half) {
                val cx = x + dx
                val cy = y + dy
                if (cx in 0 until width && cy in 0 until height) out.add(cy * width + cx)
            }
        }
        return out.toIntArray()
    }

    /** Copies [tiles] and replaces every [fromOrdinal] cell with [toOrdinal]; returns the new array. */
    fun replaceTile(tiles: IntArray, fromOrdinal: Int, toOrdinal: Int): IntArray {
        val out = tiles.copyOf()
        for (i in out.indices) if (out[i] == fromOrdinal) out[i] = toOrdinal
        return out
    }

    /**
     * Stamps [src] (a [srcW]×[srcH] tile-ordinal grid) onto a copy of [dst] with its top-left
     * corner at ([originX],[originY]), overwriting every covered cell and clipping anything that
     * falls outside the [dstW]×[dstH] destination. Returns the new array (the prefab "paste").
     */
    fun blitCells(
        dst: IntArray,
        dstW: Int,
        dstH: Int,
        src: IntArray,
        srcW: Int,
        srcH: Int,
        originX: Int,
        originY: Int,
    ): IntArray {
        val out = dst.copyOf()
        for (sy in 0 until srcH) {
            val dy = originY + sy
            if (dy !in 0 until dstH) continue
            for (sx in 0 until srcW) {
                val dx = originX + sx
                if (dx !in 0 until dstW) continue
                out[dy * dstW + dx] = src[sy * srcW + sx]
            }
        }
        return out
    }

    /**
     * Returns [cells] rotated clockwise by [quarterTurns]×90°. For an odd number of turns the
     * result's dimensions are swapped (a [w]×[h] grid becomes h×w), so callers must swap
     * width/height to match. [quarterTurns] is normalised, so negative and >3 values are fine.
     */
    fun rotateCells(cells: IntArray, w: Int, h: Int, quarterTurns: Int): IntArray {
        val turns = ((quarterTurns % 4) + 4) % 4
        var cur = cells.copyOf()
        var curW = w
        var curH = h
        repeat(turns) {
            val newW = curH
            val newH = curW
            val out = IntArray(newW * newH)
            for (y in 0 until curH) {
                for (x in 0 until curW) {
                    // Clockwise: src (x,y) → dst (curH-1-y, x).
                    out[x * newW + (curH - 1 - y)] = cur[y * curW + x]
                }
            }
            cur = out
            curW = newW
            curH = newH
        }
        return cur
    }

    /** Returns [cells] mirrored left↔right (a [w]×[h] grid); dimensions are unchanged. */
    fun mirrorCells(cells: IntArray, w: Int, h: Int): IntArray {
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                out[y * w + x] = cells[y * w + (w - 1 - x)]
            }
        }
        return out
    }
}
