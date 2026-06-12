package net.nullsum.freedoom.ui.editor

import net.nullsum.freedoom.ui.editor.model.MapDoc
import net.nullsum.freedoom.ui.editor.model.TileType

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
    if (openCells == 0) warnings.add("No open floor — there's nothing to walk on.")
    if (!hasStart) warnings.add("No player start — the engine will drop you somewhere random.")
    if (!hasExit) warnings.add("No exit — players can't finish the level.")
    return warnings
}
