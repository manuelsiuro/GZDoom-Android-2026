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
}
