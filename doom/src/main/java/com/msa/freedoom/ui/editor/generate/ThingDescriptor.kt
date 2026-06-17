package com.msa.freedoom.ui.editor.generate

import com.msa.freedoom.ui.editor.model.MapDoc
import com.msa.freedoom.ui.editor.model.MapThing

/**
 * Encodes a map's hand-placed things into the compact descriptor the native
 * converter parses: `type,cellX,cellY,angle,flags;...` in editor grid-cell
 * coordinates (the native side converts cells → Doom world units). Empty when
 * there are no things.
 *
 * Kept a pure function so it can be unit-tested and so the WAD generator's
 * cell-validity filtering stays separate from serialization.
 */
fun encodeThings(things: List<MapThing>): String =
    things.joinToString(";") { t ->
        "${t.type},${t.cellX},${t.cellY},${t.angle},${t.flags}"
    }

/**
 * The placed things on [map] that are actually valid to emit: in-bounds and on an
 * open-floor cell (a thing on a wall/door/secret/exit cell would spawn stuck, so it
 * is dropped even if it was placed before the tile under it changed).
 */
fun validThings(map: MapDoc): List<MapThing> =
    map.things.filter { t ->
        t.cellX in 0 until map.width &&
            t.cellY in 0 until map.height &&
            map.tileAt(t.cellX, t.cellY).acceptsThing
    }

/** Things that fall within a [width]×[height] grid (used to drop strays when the grid shrinks). */
fun thingsInBounds(things: List<MapThing>, width: Int, height: Int): List<MapThing> =
    things.filter { it.cellX in 0 until width && it.cellY in 0 until height }
