package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.MapThing
import com.msa.freedoom.ui.editor.model.TileType

/** Library categories shown as chips in the Prefabs palette. */
enum class PrefabCategory(val displayName: String) {
    Rooms("Rooms"),
    Corridors("Corridors"),
    Junctions("Junctions"),
    Combat("Combat"),
    Decor("Decor"),
}

/**
 * A reusable building block the user stamps onto the current map with the Prefabs tool.
 * [cells] is a [width]×[height] row-major tile-ordinal grid (same convention as
 * `MapDoc.tiles`); [things] are optional pre-placed things addressed in cell coordinates
 * *relative to the prefab's own top-left corner* (translated to the map when stamped).
 *
 * Stamping overwrites every covered destination cell (the prefab fully defines its
 * footprint) and is clipped to the map bounds — see `MapGridOps.blitCells` and
 * `MapEditorState.stampPrefabAt`.
 */
data class MapPrefab(
    val name: String,
    val category: PrefabCategory,
    val width: Int,
    val height: Int,
    val cells: IntArray,
    val things: List<MapThing> = emptyList(),
) {
    // Content-based equality so identical prefabs compare equal regardless of array identity.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapPrefab) return false
        return name == other.name && category == other.category &&
            width == other.width && height == other.height &&
            cells.contentEquals(other.cells) && things == other.things
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + cells.contentHashCode()
        result = 31 * result + things.hashCode()
        return result
    }
}

/**
 * Returns this prefab rotated clockwise by [rotation]×90° and then (optionally) mirrored
 * left↔right. Both the [MapPrefab.cells] grid and every [MapThing] (its cell position and
 * Doom angle) are transformed consistently, so a stamp matches the on-screen preview.
 *
 * Doom angles: 0°=east, 90°=north, increasing counter-clockwise. A clockwise map turn
 * subtracts 90° per turn; a left↔right mirror maps an angle to `180 - angle`.
 */
fun MapPrefab.transformed(rotation: Int, mirror: Boolean): MapPrefab {
    val turns = ((rotation % 4) + 4) % 4
    if (turns == 0 && !mirror) return this

    var cells = MapGridOps.rotateCells(this.cells, width, height, turns)
    val rotW = if (turns % 2 == 0) width else height
    val rotH = if (turns % 2 == 0) height else width
    if (mirror) cells = MapGridOps.mirrorCells(cells, rotW, rotH)

    val newThings = things.map { t ->
        var tx = t.cellX
        var ty = t.cellY
        var angle = t.angle
        var curW = width
        var curH = height
        repeat(turns) {
            val nx = curH - 1 - ty
            val ny = tx
            tx = nx
            ty = ny
            angle = ((angle - 90) % 360 + 360) % 360
            val tmp = curW; curW = curH; curH = tmp
        }
        if (mirror) {
            tx = rotW - 1 - tx
            angle = ((180 - angle) % 360 + 360) % 360
        }
        t.copy(cellX = tx, cellY = ty, angle = angle)
    }

    return copy(width = rotW, height = rotH, cells = cells, things = newThings)
}

/**
 * The built-in prefab library. Each entry is authored with [GridBuilder] and exposes a
 * concrete cell grid (+ optional things) so it can be rotated/mirrored and stamped. Pure
 * data — no Android types — so it is unit-testable.
 */
object MapPrefabs {

    // DoomEdNums used by combat prefabs (see ThingCatalog).
    private const val IMP = 3001
    private const val DEMON = 3002
    private const val ZOMBIE = 3004
    private const val SHOTGUNNER = 9
    private const val CACO = 3005
    private const val MEDIKIT = 2012
    private const val STIMPACK = 2011
    private const val ARMOR = 2018
    private const val SHOTGUN = 2001
    private const val SHELLS = 2008

    private fun GridBuilder.toPrefab(
        name: String,
        category: PrefabCategory,
        things: List<MapThing> = emptyList(),
    ) = MapPrefab(name, category, w, h, tiles, things)

    // ---- Rooms (walled boxes the user joins with doors/corridors) ----

    private fun walledRoom(name: String, size: Int): MapPrefab =
        GridBuilder(size, size).apply { rect(0, 0, size - 1, size - 1, TileType.Wall) }
            .toPrefab(name, PrefabCategory.Rooms)

    private fun roundRoom(): MapPrefab = GridBuilder(7, 7, TileType.Wall).apply {
        // An octagon: open interior with clipped corners.
        fill(1, 1, 5, 5, TileType.Room)
        put(1, 1, TileType.Wall); put(5, 1, TileType.Wall)
        put(1, 5, TileType.Wall); put(5, 5, TileType.Wall)
    }.toPrefab("Round room", PrefabCategory.Rooms)

    private fun pillarRoom(): MapPrefab = GridBuilder(7, 7).apply {
        rect(0, 0, 6, 6, TileType.Wall)
        put(2, 2, TileType.Wall); put(4, 2, TileType.Wall)
        put(2, 4, TileType.Wall); put(4, 4, TileType.Wall)
    }.toPrefab("Pillar room", PrefabCategory.Rooms)

    private fun exitRoom(): MapPrefab = GridBuilder(5, 5).apply {
        rect(0, 0, 4, 4, TileType.Wall)
        put(2, 2, TileType.Exit)
    }.toPrefab("Exit room", PrefabCategory.Rooms)

    // ---- Corridors (open passages stamped through walls) ----

    private fun corridorH(name: String, len: Int, thickness: Int): MapPrefab =
        GridBuilder(len, thickness).toPrefab(name, PrefabCategory.Corridors) // all Room

    private fun corridorV(name: String, len: Int, thickness: Int): MapPrefab =
        GridBuilder(thickness, len).toPrefab(name, PrefabCategory.Corridors)

    private fun doorwayH(): MapPrefab = GridBuilder(1, 3, TileType.Wall).apply {
        put(0, 1, TileType.Door)
    }.toPrefab("Door (V wall)", PrefabCategory.Corridors)

    private fun doorwayV(): MapPrefab = GridBuilder(3, 1, TileType.Wall).apply {
        put(1, 0, TileType.Door)
    }.toPrefab("Door (H wall)", PrefabCategory.Corridors)

    // ---- Junctions (open intersections) ----

    private fun crossJunction(): MapPrefab = GridBuilder(5, 5, TileType.Wall).apply {
        fill(2, 0, 2, 4, TileType.Room)
        fill(0, 2, 4, 2, TileType.Room)
    }.toPrefab("Cross junction", PrefabCategory.Junctions)

    private fun tJunction(): MapPrefab = GridBuilder(5, 5, TileType.Wall).apply {
        fill(0, 2, 4, 2, TileType.Room)
        fill(2, 2, 2, 4, TileType.Room)
    }.toPrefab("T junction", PrefabCategory.Junctions)

    private fun lJunction(): MapPrefab = GridBuilder(4, 4, TileType.Wall).apply {
        fill(1, 0, 1, 3, TileType.Room)
        fill(1, 3, 3, 3, TileType.Room)
    }.toPrefab("L junction", PrefabCategory.Junctions)

    // ---- Combat (rooms with pre-placed encounters) ----

    private fun impNook(): MapPrefab = GridBuilder(3, 3)
        .toPrefab("Imp nook", PrefabCategory.Combat, listOf(MapThing(IMP, 1, 1)))

    private fun ambushPair(): MapPrefab = GridBuilder(5, 5).apply {
        rect(0, 0, 4, 4, TileType.Wall)
    }.toPrefab(
        "Ambush room",
        PrefabCategory.Combat,
        listOf(MapThing(IMP, 1, 1), MapThing(IMP, 3, 1), MapThing(MEDIKIT, 2, 3)),
    )

    private fun demonPit(): MapPrefab = GridBuilder(5, 5).apply {
        fill(1, 1, 3, 3, TileType.SpecialFloor)
    }.toPrefab(
        "Demon pit",
        PrefabCategory.Combat,
        listOf(MapThing(DEMON, 1, 1), MapThing(DEMON, 3, 3)),
    )

    private fun guardPost(): MapPrefab = GridBuilder(3, 3)
        .toPrefab(
            "Guard post",
            PrefabCategory.Combat,
            listOf(MapThing(SHOTGUNNER, 1, 1), MapThing(SHELLS, 1, 2)),
        )

    private fun cacoLair(): MapPrefab = GridBuilder(7, 7).apply {
        rect(0, 0, 6, 6, TileType.Wall)
    }.toPrefab(
        "Caco lair",
        PrefabCategory.Combat,
        listOf(MapThing(CACO, 3, 2), MapThing(ZOMBIE, 1, 4), MapThing(ZOMBIE, 5, 4), MapThing(ARMOR, 3, 5)),
    )

    private fun supplyCache(): MapPrefab = GridBuilder(3, 3)
        .toPrefab(
            "Supply cache",
            PrefabCategory.Combat,
            listOf(MapThing(SHOTGUN, 1, 1), MapThing(STIMPACK, 0, 2), MapThing(STIMPACK, 2, 2)),
        )

    // ---- Decor (cosmetic / structural details) ----

    private fun pillar(): MapPrefab = GridBuilder(1, 1, TileType.Wall).toPrefab("Pillar", PrefabCategory.Decor)

    private fun column(): MapPrefab = GridBuilder(3, 3, TileType.Wall).toPrefab("Column", PrefabCategory.Decor)

    private fun fourPillars(): MapPrefab = GridBuilder(5, 5).apply {
        put(1, 1, TileType.Wall); put(3, 1, TileType.Wall)
        put(1, 3, TileType.Wall); put(3, 3, TileType.Wall)
    }.toPrefab("Four pillars", PrefabCategory.Decor)

    private fun lavaPatch(): MapPrefab = GridBuilder(3, 3, TileType.SpecialFloor)
        .toPrefab("Lava patch", PrefabCategory.Decor)

    private fun skyLight(): MapPrefab = GridBuilder(3, 3, TileType.Sky)
        .toPrefab("Sky light", PrefabCategory.Decor)

    private fun secretNook(): MapPrefab = GridBuilder(3, 3).apply {
        put(1, 1, TileType.Secret)
    }.toPrefab("Secret nook", PrefabCategory.Decor)

    val all: List<MapPrefab> = listOf(
        // Rooms
        walledRoom("Small room", 5),
        walledRoom("Medium room", 7),
        walledRoom("Large room", 9),
        roundRoom(),
        pillarRoom(),
        exitRoom(),
        // Corridors
        corridorH("Corridor H", 5, 1),
        corridorV("Corridor V", 5, 1),
        corridorH("Wide corridor H", 6, 2),
        corridorV("Wide corridor V", 6, 2),
        doorwayH(),
        doorwayV(),
        // Junctions
        crossJunction(),
        tJunction(),
        lJunction(),
        // Combat
        impNook(),
        ambushPair(),
        demonPit(),
        guardPost(),
        cacoLair(),
        supplyCache(),
        // Decor
        pillar(),
        column(),
        fourPillars(),
        lavaPatch(),
        skyLight(),
        secretNook(),
    )

    val byCategory: Map<PrefabCategory, List<MapPrefab>> = all.groupBy { it.category }
}
