package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.MapDoc
import com.msa.freedoom.ui.editor.model.MapTheme
import com.msa.freedoom.ui.editor.model.TileType

/** A named starter layout the user can spawn a fresh map from. */
data class MapTemplate(val name: String, val build: () -> MapDoc)

/**
 * A handful of ready-made layouts (24×24) to start from instead of a blank grid. Each places a
 * player Start and, where it makes sense, an Exit, so the result is immediately playable.
 */
object MapTemplates {
    private const val S = 24

    val all: List<MapTemplate> = listOf(
        MapTemplate("Empty room", ::emptyRoom),
        MapTemplate("Single room", ::singleRoom),
        MapTemplate("Arena", ::arena),
        MapTemplate("Four rooms", ::fourRooms),
        MapTemplate("Cross", ::cross),
    )

    private fun blank(fill: TileType = TileType.Room) = IntArray(S * S) { fill.ordinal }
    private fun IntArray.put(x: Int, y: Int, t: TileType) {
        if (x in 0 until S && y in 0 until S) this[y * S + x] = t.ordinal
    }
    private fun doc(tiles: IntArray) = MapDoc(S, S, tiles, MapTheme.Tech)

    private fun emptyRoom(): MapDoc = blank().also {
        it.put(4, 12, TileType.Start)
        it.put(19, 12, TileType.Exit)
    }.let(::doc)

    private fun singleRoom(): MapDoc {
        val t = MapGridOps.drawRect(blank(), S, S, 2, 2, S - 3, S - 3, TileType.Wall, filled = false)
        t.put(5, 12, TileType.Start)
        t.put(18, 12, TileType.Exit)
        return doc(t)
    }

    private fun arena(): MapDoc {
        val t = blank()
        var x = 4
        while (x <= 20) {
            var y = 4
            while (y <= 20) {
                t.put(x, y, TileType.Wall) // pillar
                y += 4
            }
            x += 4
        }
        t.put(2, 2, TileType.Start)
        t.put(21, 21, TileType.Exit)
        return doc(t)
    }

    private fun fourRooms(): MapDoc {
        var t = blank()
        t = MapGridOps.drawLine(t, S, S, 12, 2, 12, 21, TileType.Wall) // vertical divider
        t = MapGridOps.drawLine(t, S, S, 2, 12, 21, 12, TileType.Wall) // horizontal divider
        // Door gaps through each arm of the cross.
        t.put(12, 6, TileType.Door)
        t.put(12, 17, TileType.Door)
        t.put(6, 12, TileType.Door)
        t.put(17, 12, TileType.Door)
        t.put(5, 5, TileType.Start)
        t.put(18, 18, TileType.Exit)
        return doc(t)
    }

    private fun cross(): MapDoc {
        var t = blank(TileType.Wall)
        t = MapGridOps.drawRect(t, S, S, 2, 10, 21, 13, TileType.Room, filled = true) // horizontal corridor
        t = MapGridOps.drawRect(t, S, S, 10, 2, 13, 21, TileType.Room, filled = true) // vertical corridor
        t.put(3, 11, TileType.Start)
        t.put(20, 12, TileType.Exit)
        return doc(t)
    }
}
