package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.MapDoc
import com.msa.freedoom.ui.editor.model.TileType

/**
 * Advisory pre-launch checks for a map. None of these block generation — the converter still
 * auto-encloses the map and drops a player start somewhere — they just warn about things that
 * usually make a level less playable. Returns an empty list when the map looks fine.
 */
fun validateMap(map: MapDoc): List<String> {
    val warnings = ArrayList<String>()
    var hasStart = false
    var hasExit = false
    var openCells = 0
    for (ordinal in map.tiles) {
        when (ordinal) {
            TileType.Start.ordinal -> hasStart = true
            TileType.Exit.ordinal -> hasExit = true
        }
        if (ordinal != TileType.Wall.ordinal) openCells++
    }
    // A hand-placed player start (DoomEdNum 1–4) also satisfies the start requirement.
    val placedStart = map.things.any { it.type in 1..4 }
    val stranded = map.things.count { t ->
        t.cellX !in 0 until map.width || t.cellY !in 0 until map.height ||
            !map.tileAt(t.cellX, t.cellY).acceptsThing
    }

    if (openCells == 0) warnings.add("No open floor — there's nothing to walk on.")
    if (!hasStart && !placedStart) warnings.add("No player start — the engine will drop you somewhere random.")
    if (!hasExit) warnings.add("No exit — players can't finish the level.")
    if (stranded > 0) warnings.add("$stranded placed thing(s) sit on a wall/door cell and will be skipped.")
    return warnings
}
