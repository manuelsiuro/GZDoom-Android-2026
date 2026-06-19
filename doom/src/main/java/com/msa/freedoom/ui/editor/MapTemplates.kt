package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.MapDoc
import com.msa.freedoom.ui.editor.model.MapTheme
import com.msa.freedoom.ui.editor.model.MapThing
import com.msa.freedoom.ui.editor.model.TileType

/** Library groupings shown as chips in the "New from template" sheet. */
enum class TemplateCategory(val displayName: String) {
    Starter("Starter"),
    Combat("Combat"),
    Layout("Layout"),
    Maze("Maze"),
    Themed("Themed"),
}

/** A named starter layout the user can spawn a fresh map from. */
data class MapTemplate(val name: String, val category: TemplateCategory, val build: () -> MapDoc)

/**
 * Ready-made layouts to start a map from instead of a blank grid. Each places a player Start
 * and an Exit (and some place a handful of things) so the result is immediately playable, and
 * is authored with [GridBuilder] + [MapGridOps]. Sizes and themes vary per template.
 *
 * The sheet just iterates [all]/[byCategory]; adding a template is a one-line registration.
 */
object MapTemplates {

    // DoomEdNums used by the designed templates (see ThingCatalog).
    private const val IMP = 3001
    private const val DEMON = 3002
    private const val ZOMBIE = 3004
    private const val MEDIKIT = 2012
    private const val SHOTGUN = 2001
    private const val ARMOR = 2018

    val all: List<MapTemplate> = listOf(
        // Starter
        MapTemplate("Empty room", TemplateCategory.Starter, ::emptyRoom),
        MapTemplate("Single room", TemplateCategory.Starter, ::singleRoom),
        MapTemplate("Corridor start", TemplateCategory.Starter, ::corridorStart),
        MapTemplate("Two rooms", TemplateCategory.Starter, ::twoRooms),
        // Combat
        MapTemplate("Arena", TemplateCategory.Combat, ::arena),
        MapTemplate("Pillar hall", TemplateCategory.Combat, ::pillarHall),
        MapTemplate("Courtyard", TemplateCategory.Combat, ::courtyard),
        MapTemplate("Ambush", TemplateCategory.Combat, ::ambushRoom),
        // Layout
        MapTemplate("Four rooms", TemplateCategory.Layout, ::fourRooms),
        MapTemplate("Cross", TemplateCategory.Layout, ::cross),
        MapTemplate("Room grid", TemplateCategory.Layout, ::gridRooms),
        MapTemplate("Ring", TemplateCategory.Layout, ::ringDonut),
        // Maze
        MapTemplate("Simple maze", TemplateCategory.Maze, ::simpleMaze),
        MapTemplate("Tight maze", TemplateCategory.Maze, ::tightMaze),
        // Themed
        MapTemplate("Hell cavern", TemplateCategory.Themed, ::hellCavern),
        MapTemplate("Tech base", TemplateCategory.Themed, ::techBase),
        MapTemplate("Sky courtyard", TemplateCategory.Themed, ::skyCourtyard),
    )

    val byCategory: Map<TemplateCategory, List<MapTemplate>> = all.groupBy { it.category }

    // ---- Starter ----

    private fun emptyRoom(): MapDoc = GridBuilder(24, 24).apply {
        put(4, 12, TileType.Start)
        put(19, 12, TileType.Exit)
    }.doc(MapTheme.Tech)

    private fun singleRoom(): MapDoc = GridBuilder(24, 24).apply {
        rect(2, 2, 21, 21, TileType.Wall)
        put(5, 12, TileType.Start)
        put(18, 12, TileType.Exit)
    }.doc(MapTheme.Tech)

    private fun corridorStart(): MapDoc = GridBuilder(24, 24, TileType.Wall).apply {
        fill(2, 9, 6, 14, TileType.Room)   // start room
        fill(6, 11, 18, 12, TileType.Room) // corridor
        fill(17, 9, 21, 14, TileType.Room) // exit room
        put(3, 12, TileType.Start)
        put(20, 12, TileType.Exit)
    }.doc(
        MapTheme.Tech,
        listOf(MapThing(IMP, 10, 11), MapThing(IMP, 14, 12), MapThing(MEDIKIT, 4, 10)),
    )

    private fun twoRooms(): MapDoc = GridBuilder(24, 24).apply {
        rect(2, 2, 21, 21, TileType.Wall)
        line(12, 2, 12, 21, TileType.Wall)
        put(12, 11, TileType.Door)
        put(6, 11, TileType.Start)
        put(17, 11, TileType.Exit)
    }.doc(MapTheme.Tech, listOf(MapThing(IMP, 16, 8)))

    // ---- Combat ----

    private fun arena(): MapDoc = GridBuilder(24, 24).apply {
        var x = 4
        while (x <= 20) {
            var y = 4
            while (y <= 20) {
                put(x, y, TileType.Wall) // pillar
                y += 4
            }
            x += 4
        }
        put(2, 2, TileType.Start)
        put(21, 21, TileType.Exit)
    }.doc(
        MapTheme.Tech,
        listOf(MapThing(IMP, 2, 21), MapThing(IMP, 21, 2), MapThing(ZOMBIE, 11, 11)),
    )

    private fun pillarHall(): MapDoc = GridBuilder(32, 32).apply {
        rect(1, 1, 30, 30, TileType.Wall)
        var x = 4
        while (x <= 28) {
            var y = 4
            while (y <= 28) {
                put(x, y, TileType.Wall) // pillar
                y += 4
            }
            x += 4
        }
        put(2, 16, TileType.Start)
        put(29, 16, TileType.Exit)
    }.doc(
        MapTheme.Tech,
        listOf(MapThing(IMP, 6, 16), MapThing(IMP, 26, 16), MapThing(DEMON, 16, 6), MapThing(DEMON, 16, 26)),
    )

    private fun courtyard(): MapDoc = GridBuilder(32, 32, TileType.Wall).apply {
        fill(4, 4, 27, 27, TileType.Room)
        put(6, 16, TileType.Start)
        put(25, 16, TileType.Exit)
    }.doc(
        MapTheme.Tech,
        listOf(
            MapThing(DEMON, 12, 12), MapThing(DEMON, 20, 20),
            MapThing(DEMON, 12, 20), MapThing(DEMON, 20, 12),
            MapThing(MEDIKIT, 16, 16),
        ),
    )

    private fun ambushRoom(): MapDoc = GridBuilder(24, 24, TileType.Wall).apply {
        fill(2, 9, 6, 14, TileType.Room)   // start room
        fill(6, 11, 13, 11, TileType.Room) // corridor
        fill(14, 4, 21, 19, TileType.Room) // chamber
        put(13, 11, TileType.Door)
        put(3, 12, TileType.Start)
        put(20, 5, TileType.Exit)
    }.doc(
        MapTheme.Tech,
        listOf(
            MapThing(IMP, 16, 6), MapThing(IMP, 19, 16), MapThing(IMP, 16, 16),
            MapThing(MEDIKIT, 18, 10),
        ),
    )

    // ---- Layout ----

    private fun fourRooms(): MapDoc = GridBuilder(24, 24).apply {
        line(12, 2, 12, 21, TileType.Wall) // vertical divider
        line(2, 12, 21, 12, TileType.Wall) // horizontal divider
        put(12, 6, TileType.Door)
        put(12, 17, TileType.Door)
        put(6, 12, TileType.Door)
        put(17, 12, TileType.Door)
        put(5, 5, TileType.Start)
        put(18, 18, TileType.Exit)
    }.doc(MapTheme.Tech)

    private fun cross(): MapDoc = GridBuilder(24, 24, TileType.Wall).apply {
        fill(2, 10, 21, 13, TileType.Room) // horizontal corridor
        fill(10, 2, 13, 21, TileType.Room) // vertical corridor
        put(3, 11, TileType.Start)
        put(20, 12, TileType.Exit)
    }.doc(MapTheme.Tech)

    private fun gridRooms(): MapDoc = GridBuilder(32, 32).apply {
        rect(0, 0, 31, 31, TileType.Wall)
        line(10, 1, 10, 30, TileType.Wall)
        line(21, 1, 21, 30, TileType.Wall)
        line(1, 10, 30, 10, TileType.Wall)
        line(1, 21, 30, 21, TileType.Wall)
        // Door gaps so all nine rooms connect.
        for (d in listOf(5, 15, 26)) {
            put(10, d, TileType.Door); put(21, d, TileType.Door)
            put(d, 10, TileType.Door); put(d, 21, TileType.Door)
        }
        put(5, 5, TileType.Start)
        put(26, 26, TileType.Exit)
    }.doc(MapTheme.Tech, listOf(MapThing(IMP, 15, 15), MapThing(IMP, 26, 5), MapThing(IMP, 5, 26)))

    private fun ringDonut(): MapDoc = GridBuilder(28, 28, TileType.Wall).apply {
        fill(2, 2, 25, 25, TileType.Room) // outer area
        fill(9, 9, 18, 18, TileType.Wall) // central block
        put(4, 4, TileType.Start)
        put(23, 23, TileType.Exit)
    }.doc(
        MapTheme.Tech,
        listOf(MapThing(IMP, 13, 4), MapThing(IMP, 4, 13), MapThing(IMP, 23, 13), MapThing(IMP, 13, 23)),
    )

    // ---- Maze ----

    private fun simpleMaze(): MapDoc = maze(24, MapTheme.Tech, seed = 1L)
    private fun tightMaze(): MapDoc = maze(32, MapTheme.Cave, seed = 2L)

    /**
     * A deterministic recursive-backtracker maze (fixed [seed] so the thumbnail matches the
     * generated map). Walls everywhere; passages carved on the odd lattice. Start in the
     * top-left cell, Exit in the bottom-right carved cell.
     */
    private fun maze(size: Int, theme: MapTheme, seed: Long): MapDoc {
        val g = GridBuilder(size, size, TileType.Wall)
        val wall = TileType.Wall.ordinal
        val rnd = kotlin.random.Random(seed)
        val dirs = listOf(intArrayOf(0, -2), intArrayOf(0, 2), intArrayOf(-2, 0), intArrayOf(2, 0))

        fun carve(cx: Int, cy: Int) {
            g.put(cx, cy, TileType.Room)
            for (d in dirs.shuffled(rnd)) {
                val nx = cx + d[0]
                val ny = cy + d[1]
                if (nx in 1 until size - 1 && ny in 1 until size - 1 && g.tiles[ny * size + nx] == wall) {
                    g.put(cx + d[0] / 2, cy + d[1] / 2, TileType.Room)
                    carve(nx, ny)
                }
            }
        }
        carve(1, 1)

        val last = if ((size - 2) % 2 == 1) size - 2 else size - 3
        g.put(1, 1, TileType.Start)
        g.put(last, last, TileType.Exit)
        return g.doc(theme)
    }

    // ---- Themed ----

    private fun hellCavern(): MapDoc = GridBuilder(28, 28, TileType.Wall).apply {
        // Overlapping blobs make an organic cave.
        fill(3, 5, 12, 22, TileType.Room)
        fill(8, 3, 20, 12, TileType.Room)
        fill(14, 8, 24, 24, TileType.Room)
        fill(5, 14, 18, 25, TileType.Room)
        fill(15, 16, 20, 21, TileType.SpecialFloor) // lava pool
        put(5, 7, TileType.Start)
        put(22, 22, TileType.Exit)
    }.doc(
        MapTheme.Hell,
        listOf(
            MapThing(DEMON, 10, 6), MapThing(IMP, 18, 11), MapThing(IMP, 9, 18),
            MapThing(ARMOR, 6, 20),
        ),
    )

    private fun techBase(): MapDoc = GridBuilder(32, 32, TileType.Wall).apply {
        fill(2, 2, 10, 10, TileType.Room)   // A: start
        fill(20, 2, 29, 12, TileType.Room)  // B
        fill(20, 18, 29, 29, TileType.Room) // C: exit
        fill(2, 18, 12, 29, TileType.Room)  // D
        // Connecting corridors with doors.
        fill(11, 5, 19, 5, TileType.Room); put(15, 5, TileType.Door)
        fill(25, 13, 25, 17, TileType.Room); put(25, 15, TileType.Door)
        fill(13, 24, 19, 24, TileType.Room); put(16, 24, TileType.Door)
        fill(6, 11, 6, 17, TileType.Room); put(6, 14, TileType.Door)
        put(4, 4, TileType.Start)
        put(26, 26, TileType.Exit)
    }.doc(
        MapTheme.Tech,
        listOf(MapThing(IMP, 24, 6), MapThing(IMP, 24, 24), MapThing(SHOTGUN, 8, 8)),
    )

    private fun skyCourtyard(): MapDoc = GridBuilder(28, 28).apply {
        rect(0, 0, 27, 27, TileType.Wall)
        rect(2, 2, 25, 25, TileType.Sky) // open-air band just inside the walls
        fill(11, 11, 16, 16, TileType.Wall) // central structure
        put(4, 4, TileType.Start)
        put(23, 23, TileType.Exit)
    }.doc(MapTheme.City, listOf(MapThing(IMP, 6, 21), MapThing(IMP, 21, 6)))
}
