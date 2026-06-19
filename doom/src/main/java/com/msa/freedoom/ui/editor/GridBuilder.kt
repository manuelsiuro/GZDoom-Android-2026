package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.MapDoc
import com.msa.freedoom.ui.editor.model.MapTheme
import com.msa.freedoom.ui.editor.model.MapThing
import com.msa.freedoom.ui.editor.model.TileType

/**
 * A tiny mutable grid for hand-authoring starter templates and prefabs. Holds a row-major
 * tile-ordinal array (`tiles[y*w+x]`, the same convention as [MapDoc] and [MapGridOps]) and
 * a handful of drawing helpers that delegate to [MapGridOps] so authoring code reads
 * declaratively. Internal: only the editor's template/prefab catalogs use it.
 */
internal class GridBuilder(val w: Int, val h: Int, fill: TileType = TileType.Room) {
    val tiles: IntArray = IntArray(w * h) { fill.ordinal }

    /** Set a single cell (silently ignored if out of bounds). */
    fun put(x: Int, y: Int, tile: TileType) {
        if (x in 0 until w && y in 0 until h) tiles[y * w + x] = tile.ordinal
    }

    /** Draw a rectangle outline (or a solid box when [filled]) between the two corners. */
    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, tile: TileType, filled: Boolean = false) {
        for (idx in MapGridOps.rectCells(w, h, x0, y0, x1, y1, filled)) tiles[idx] = tile.ordinal
    }

    /** Fill a solid rectangle between the two corners. */
    fun fill(x0: Int, y0: Int, x1: Int, y1: Int, tile: TileType) = rect(x0, y0, x1, y1, tile, filled = true)

    /** Stamp a Bresenham line between the two endpoints. */
    fun line(x0: Int, y0: Int, x1: Int, y1: Int, tile: TileType) {
        for (idx in MapGridOps.lineCells(w, h, x0, y0, x1, y1)) tiles[idx] = tile.ordinal
    }

    /** Wrap the authored grid as a [MapDoc] (used by templates). */
    fun doc(theme: MapTheme, things: List<MapThing> = emptyList()): MapDoc =
        MapDoc(w, h, tiles, theme, things)
}
