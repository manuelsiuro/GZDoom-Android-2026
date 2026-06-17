package com.msa.freedoom.ui.editor

import android.app.Activity
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.msa.freedoom.AppSettings
import com.msa.freedoom.ui.editor.data.ProjectStore
import com.msa.freedoom.ui.editor.generate.GenerateResult
import com.msa.freedoom.ui.editor.generate.generateWad
import com.msa.freedoom.ui.editor.launch.launchProject
import com.msa.freedoom.ui.editor.model.MapDoc
import com.msa.freedoom.ui.editor.model.MapProject
import com.msa.freedoom.ui.editor.model.MapTheme
import com.msa.freedoom.ui.editor.model.MapThing
import com.msa.freedoom.ui.editor.model.TextureRole
import com.msa.freedoom.ui.editor.model.ThingCatalog
import com.msa.freedoom.ui.editor.model.ThingType
import com.msa.freedoom.ui.editor.model.TileType
import com.msa.freedoom.ui.editor.model.Tuning
import com.msa.freedoom.ui.editor.model.WadFormat
import com.msa.freedoom.ui.launch.WadEntry
import com.msa.freedoom.ui.launch.scanIwads
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * The active editing tool. Pan lets a one-finger drag move the view (two fingers always
 * transform). Thing places the active [ThingType] (tap empty cell), while Select only edits
 * already-placed things (tap to select, drag to move, palette to retype, inspector to delete)
 * and never drops new ones.
 */
enum class EditorTool { Brush, Eraser, Bucket, Line, Rect, Eyedropper, Pan, Thing, Select }

/**
 * An in-progress line/rectangle the user is dragging out. Held in state so the canvas can draw a
 * live preview before it is committed to the grid on release. Coordinates are grid cells.
 */
data class ShapePreview(
    val sx: Int,
    val sy: Int,
    val ex: Int,
    val ey: Int,
    val tile: TileType,
    val filled: Boolean,
    val isLine: Boolean,
)

/** Preset square grid sizes offered as quick chips; custom W×H is also allowed. */
val GRID_PRESETS = listOf(16, 24, 32, 48, 64)

/** Hard bounds for a custom grid dimension. */
const val MIN_GRID = 4
const val MAX_GRID = 128

private const val AUTOSAVE_DEBOUNCE_MS = 500L
private const val EDITOR_LAST_IWAD = "editor_last_iwad"

/**
 * The editor's single source of truth: owns the multi-map [MapProject], the viewport, the
 * active tool, undo history, the available-IWAD list and a debounced auto-save. Hoisted in
 * `MainScreen` so it survives tab switches; persisted to disk so it survives process death.
 *
 * The [project] state uses [referentialEqualityPolicy] because painting mutates the current
 * map's tile array **in place** (cheap, no per-cell allocation) and bumps [strokeRevision]
 * to redraw the canvas; non-stroke edits publish a fresh [MapProject] instance to recompose
 * the rest of the UI. Undo snapshots are independent array copies, so in-place mutation never
 * corrupts history.
 */
@Stable
class MapEditorState(
    private val activity: Activity,
    private val scope: CoroutineScope,
) {
    var project by mutableStateOf(MapProject(), referentialEqualityPolicy())
        private set
    var currentMapIndex by mutableIntStateOf(0)
        private set

    var selectedTile by mutableStateOf(TileType.Wall)
    var activeTool by mutableStateOf(EditorTool.Brush)

    /** Whether the rectangle tool fills the box (vs drawing only its outline). */
    var rectFilled by mutableStateOf(false)

    /** Mirror painting/shapes across the grid centre. */
    var symmetry by mutableStateOf(MapGridOps.SymmetryMode.None)

    /** Brush tip size in cells (1, 2 or 3) for Brush/Eraser. */
    var brushSize by mutableStateOf(1)

    /** The thing type the Thing tool places. Defaults to the Imp. */
    var selectedThingType by mutableStateOf(ThingCatalog.byId(3001) ?: ThingCatalog.all.first())

    /** The thing palette category currently shown (kept here so it survives tool toggles). */
    var thingCategory by mutableStateOf(selectedThingType.category)

    /** Index into the current map's [MapDoc.things] of the selected thing, or null. */
    var selectedThingIndex by mutableStateOf<Int?>(null)
        private set

    /** The line/rectangle currently being dragged out, for the canvas to preview. */
    var shapePreview by mutableStateOf<ShapePreview?>(null)
        private set

    // Viewport (kept here so zoom/pan survive tab switches).
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    /** Bumped on every painted cell during a stroke; the canvas observes it to redraw. */
    var strokeRevision by mutableIntStateOf(0)
        private set

    var isBusy by mutableStateOf(false)
        private set
    var genStatus by mutableStateOf<String?>(null)
        private set

    val availableIwads = mutableStateListOf<WadEntry>()

    private val undo = UndoManager()
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private var saveJob: Job? = null
    private var strokeSnapshot: MapDoc? = null

    // ---- current-map accessors (for the canvas) ----

    val currentMap: MapDoc get() = project.maps[currentMapIndex.coerceIn(0, project.maps.lastIndex)]
    val gridWidth: Int get() = currentMap.width
    val gridHeight: Int get() = currentMap.height
    val theme: MapTheme get() = currentMap.theme

    // ---- painting ----

    /** Snapshot the current map so the whole drag collapses to one undo entry. */
    fun beginStroke() {
        strokeSnapshot = currentMap.copy(tiles = currentMap.tiles.copyOf())
    }

    /**
     * Paint at a cell with the active tool (Brush → selected tile, Eraser → Room), honouring the
     * brush size (a size×size tip) and symmetry (mirrored tips). In place; bumps [strokeRevision].
     */
    fun paintAt(x: Int, y: Int) {
        val m = currentMap
        val value = if (activeTool == EditorTool.Eraser) TileType.Room.ordinal else selectedTile.ordinal
        val tip = MapGridOps.blockCells(m.width, m.height, x, y, brushSize)
        val cells = MapGridOps.expandSymmetry(tip, m.width, m.height, symmetry)
        var changed = false
        for (idx in cells) {
            if (m.tiles[idx] != value) { m.tiles[idx] = value; changed = true }
        }
        if (changed) strokeRevision++
    }

    /** Commit a stroke: record undo + publish a new project instance if anything changed. */
    fun endStroke() {
        val snap = strokeSnapshot ?: return
        strokeSnapshot = null
        if (!snap.tiles.contentEquals(currentMap.tiles)) {
            undo.push(snap)
            refreshUndoFlags()
            republish()
            markDirty()
        }
    }

    /** Flood-fill from a cell (and its symmetry mirrors) with the selected tile. */
    fun bucketAt(x: Int, y: Int) {
        val m = currentMap
        val replacement = if (activeTool == EditorTool.Eraser) TileType.Room else selectedTile
        val seeds = MapGridOps.mirrorIndices(m.width, m.height, x, y, symmetry)
            .ifEmpty { listOf(y * m.width + x) }
        var tiles = m.tiles
        var changed = false
        for (seed in seeds) {
            val res = MapGridOps.floodFill(tiles, m.width, m.height, seed % m.width, seed / m.width, replacement)
            if (res != null) { tiles = res; changed = true }
        }
        if (changed) commitMapEdit(m.copy(tiles = tiles))
    }

    // ---- shape tools (line / rectangle) ----

    /** Begin dragging out a line/rectangle from a cell. */
    fun startShape(x: Int, y: Int) {
        val m = currentMap
        val cx = x.coerceIn(0, m.width - 1)
        val cy = y.coerceIn(0, m.height - 1)
        shapePreview = ShapePreview(cx, cy, cx, cy, selectedTile, rectFilled, activeTool == EditorTool.Line)
    }

    /** Update the end of the in-progress shape as the finger moves. */
    fun updateShape(x: Int, y: Int) {
        val m = currentMap
        shapePreview = shapePreview?.copy(
            ex = x.coerceIn(0, m.width - 1),
            ey = y.coerceIn(0, m.height - 1),
        )
    }

    /** Stamp the previewed shape (mirrored by symmetry) onto the grid as a single undo step. */
    fun commitShape() {
        val p = shapePreview ?: return
        shapePreview = null
        val m = currentMap
        val baseCells = if (p.isLine) {
            MapGridOps.lineCells(m.width, m.height, p.sx, p.sy, p.ex, p.ey)
        } else {
            MapGridOps.rectCells(m.width, m.height, p.sx, p.sy, p.ex, p.ey, p.filled)
        }
        val cells = MapGridOps.expandSymmetry(baseCells, m.width, m.height, symmetry)
        val v = p.tile.ordinal
        val newTiles = m.tiles.copyOf()
        var changed = false
        for (idx in cells) {
            if (newTiles[idx] != v) { newTiles[idx] = v; changed = true }
        }
        if (changed) pushUndoAndCommit(m.copy(tiles = newTiles))
    }

    /** Discard an in-progress shape (e.g. when a second finger starts a pinch). */
    fun cancelShape() {
        shapePreview = null
    }

    /** Eyedropper: adopt the tile under a cell as the active tile and return to the brush. */
    fun pickTileAt(x: Int, y: Int) {
        val m = currentMap
        if (x !in 0 until m.width || y !in 0 until m.height) return
        selectedTile = m.tileAt(x, y)
        activeTool = EditorTool.Brush
    }

    // ---- things (manual placement) ----

    fun selectThingType(type: ThingType) { selectedThingType = type }

    /**
     * Palette pick: always updates the active type; in [EditorTool.Select] with a thing
     * selected, also retypes that placed thing (edit-in-place after positioning).
     */
    fun pickThingType(type: ThingType) {
        selectedThingType = type
        if (activeTool == EditorTool.Select) retypeSelectedThing(type)
    }

    /** Change the type of the selected placed thing. One undo step. */
    fun retypeSelectedThing(type: ThingType) {
        val idx = selectedThingIndex ?: return
        val m = currentMap
        val t = m.things.getOrNull(idx) ?: return
        if (t.type == type.id) return
        val things = m.things.toMutableList().apply { this[idx] = t.copy(type = type.id) }
        pushUndoAndCommit(m.copy(things = things))
    }

    /** Topmost placed thing at a cell, or null. */
    fun thingIndexAt(x: Int, y: Int): Int? =
        currentMap.things.indexOfLast { it.cellX == x && it.cellY == y }.takeIf { it >= 0 }

    /** Select (or clear) the placed thing under a cell; returns true if one was found. */
    fun selectThingAt(x: Int, y: Int): Boolean {
        val idx = thingIndexAt(x, y)
        selectedThingIndex = idx
        return idx != null
    }

    fun clearThingSelection() { selectedThingIndex = null }

    /** Point the palette at the selected thing's type/category (so Select shows what's chosen). */
    fun syncPaletteToSelected() {
        val idx = selectedThingIndex ?: return
        val t = currentMap.things.getOrNull(idx) ?: return
        ThingCatalog.byId(t.type)?.let {
            selectedThingType = it
            thingCategory = it.category
        }
    }

    /**
     * Place the active [selectedThingType] at a cell. Rejected (returns false) if the cell is
     * out of bounds or not an open-floor tile (a thing in a wall would spawn stuck). One undo step.
     */
    fun placeThingAt(x: Int, y: Int): Boolean {
        val m = currentMap
        if (x !in 0 until m.width || y !in 0 until m.height) return false
        if (!m.tileAt(x, y).acceptsThing) return false
        val newThings = m.things + MapThing(type = selectedThingType.id, cellX = x, cellY = y)
        pushUndoAndCommit(m.copy(things = newThings))
        selectedThingIndex = newThings.lastIndex
        return true
    }

    /** Move the selected thing to a new open-floor cell. One undo step. Returns false if rejected. */
    fun moveSelectedThingTo(x: Int, y: Int): Boolean {
        val idx = selectedThingIndex ?: return false
        val m = currentMap
        if (x !in 0 until m.width || y !in 0 until m.height) return false
        if (!m.tileAt(x, y).acceptsThing) return false
        val t = m.things.getOrNull(idx) ?: return false
        if (t.cellX == x && t.cellY == y) return false
        val things = m.things.toMutableList().apply { this[idx] = t.copy(cellX = x, cellY = y) }
        pushUndoAndCommit(m.copy(things = things))
        return true
    }

    fun deleteSelectedThing() {
        val idx = selectedThingIndex ?: return
        val m = currentMap
        if (idx !in m.things.indices) return
        pushUndoAndCommit(m.copy(things = m.things.toMutableList().apply { removeAt(idx) }))
        selectedThingIndex = null
    }

    /** Delete the thing under a cell (e.g. from a long-press). Returns true if one was removed. */
    fun deleteThingAt(x: Int, y: Int): Boolean {
        val idx = thingIndexAt(x, y) ?: return false
        val m = currentMap
        pushUndoAndCommit(m.copy(things = m.things.toMutableList().apply { removeAt(idx) }))
        if (selectedThingIndex == idx) selectedThingIndex = null
        return true
    }

    /** Set the angle (degrees) of the selected thing, normalised to 0..359. One undo step. */
    fun setSelectedThingAngle(angle: Int) {
        val idx = selectedThingIndex ?: return
        val m = currentMap
        val t = m.things.getOrNull(idx) ?: return
        val a = ((angle % 360) + 360) % 360
        if (t.angle == a) return
        val things = m.things.toMutableList().apply { this[idx] = t.copy(angle = a) }
        pushUndoAndCommit(m.copy(things = things))
    }

    // ---- textures / manual-things toggles ----

    fun textureOverride(role: TextureRole): List<String> =
        project.textureOverrides[role.name].orEmpty()

    fun setTextureOverride(role: TextureRole, names: List<String>) {
        val map = project.textureOverrides.toMutableMap()
        if (names.isEmpty()) map.remove(role.name) else map[role.name] = names
        project = project.copy(textureOverrides = map)
        markDirty()
    }

    fun setManualThings(enabled: Boolean) {
        if (project.manualThings == enabled) return
        project = project.copy(manualThings = enabled)
        markDirty()
    }

    /** Absolute path of the project's IWAD (for the native texture reader). */
    fun iwadAbsolutePath(): String = File(AppSettings.getQuakeFullDir(), project.iwadFile).absolutePath

    /** Fill the whole current map with one tile (one undo step). */
    fun clearMap(tile: TileType) {
        val m = currentMap
        pushUndoAndCommit(m.copy(tiles = IntArray(m.width * m.height) { tile.ordinal }))
    }

    /** Reset zoom/pan — at scale 1 / offset 0 the grid is drawn to fit the viewport. */
    fun resetView() {
        scale = 1f
        offset = Offset.Zero
    }

    fun setGenerateThings(enabled: Boolean) {
        if (project.generateThings == enabled) return
        project = project.copy(generateThings = enabled)
        markDirty()
    }

    /** Replace every [from] tile in the current map with [to] (one undo step). */
    fun replaceTile(from: TileType, to: TileType) {
        if (from == to) return
        val m = currentMap
        val newTiles = MapGridOps.replaceTile(m.tiles, from.ordinal, to.ordinal)
        if (!newTiles.contentEquals(m.tiles)) pushUndoAndCommit(m.copy(tiles = newTiles))
    }

    /** Add a ready-made map (e.g. a template) as a new slot and switch to editing it. */
    fun addMapFromDoc(doc: MapDoc) {
        if (project.maps.size >= MapProject.MAX_MAPS) return
        project = project.copy(maps = project.maps + doc)
        selectMap(project.maps.lastIndex)
    }

    /** Set the test-launch skill (1 = easiest … 5 = Nightmare). */
    fun setSkill(skill: Int) {
        val s = skill.coerceIn(1, 5)
        if (project.skill == s) return
        project = project.copy(skill = s)
        markDirty()
    }

    // ---- undo / redo ----

    fun undo() {
        val restored = undo.undo(currentMap) ?: return
        setCurrentMap(restored)
        selectedThingIndex = null
        refreshUndoFlags()
        markDirty()
    }

    fun redo() {
        val restored = undo.redo(currentMap) ?: return
        setCurrentMap(restored)
        selectedThingIndex = null
        refreshUndoFlags()
        markDirty()
    }

    // ---- grid size / theme ----

    fun setSize(newW: Int, newH: Int, mode: ResizeMode) {
        val w = newW.coerceIn(MIN_GRID, MAX_GRID)
        val h = newH.coerceIn(MIN_GRID, MAX_GRID)
        val m = currentMap
        if (w == m.width && h == m.height) return
        val resized = MapGridOps.resizeGrid(m.tiles, m.width, m.height, w, h, mode)
        pushUndoAndCommit(m.copy(width = w, height = h, tiles = resized))
        // A different-sized grid invalidates the current pan/zoom.
        scale = 1f
        offset = Offset.Zero
    }

    fun setTheme(theme: MapTheme) {
        val m = currentMap
        if (m.theme == theme) return
        pushUndoAndCommit(m.copy(theme = theme))
    }

    // ---- multi-map ops ----

    fun addMap() {
        if (project.maps.size >= MapProject.MAX_MAPS) return
        val template = currentMap
        val blank = MapDoc(template.width, template.height, IntArray(template.width * template.height), template.theme)
        project = project.copy(maps = project.maps + blank)
        selectMap(project.maps.lastIndex)
    }

    fun duplicateMap(index: Int) {
        if (project.maps.size >= MapProject.MAX_MAPS) return
        val src = project.maps.getOrNull(index) ?: return
        val copy = src.copy(tiles = src.tiles.copyOf())
        val maps = project.maps.toMutableList().apply { add(index + 1, copy) }
        project = project.copy(maps = maps)
        selectMap(index + 1)
    }

    fun deleteMap(index: Int) {
        if (project.maps.size <= 1) return
        val maps = project.maps.toMutableList().apply { removeAt(index) }
        val newTest = project.testMapIndex.coerceAtMost(maps.lastIndex)
        project = project.copy(maps = maps, testMapIndex = newTest)
        selectMap(currentMapIndex.coerceAtMost(maps.lastIndex))
    }

    fun moveMap(from: Int, to: Int) {
        if (from == to) return
        val maps = project.maps.toMutableList()
        if (from !in maps.indices || to !in maps.indices) return
        maps.add(to, maps.removeAt(from))
        project = project.copy(maps = maps)
        selectMap(to)
    }

    fun selectMap(index: Int) {
        currentMapIndex = index.coerceIn(0, project.maps.lastIndex)
        selectedThingIndex = null
        undo.clear()
        refreshUndoFlags()
        scale = 1f
        offset = Offset.Zero
        markDirty()
    }

    fun setTestMap(index: Int) {
        project = project.copy(testMapIndex = index.coerceIn(0, project.maps.lastIndex))
        markDirty()
    }

    // ---- tuning / format / naming / iwad ----

    fun setMonsterDensity(v: Float) = setTuning(project.tuning.copy(monsterDensity = v))
    fun setItemDensity(v: Float) = setTuning(project.tuning.copy(itemDensity = v))
    fun setAmmoDensity(v: Float) = setTuning(project.tuning.copy(ammoDensity = v))

    private fun setTuning(tuning: Tuning) {
        project = project.copy(tuning = tuning)
        markDirty()
    }

    fun setFormat(format: WadFormat) {
        project = project.copy(format = format)
        markDirty()
    }

    fun rename(name: String) {
        project = project.copy(name = name)
        markDirty()
    }

    fun setIwad(file: String) {
        project = project.copy(iwadFile = file)
        AppSettings.setStringOption(activity, EDITOR_LAST_IWAD, file)
        markDirty()
    }

    fun loadIwads() {
        scope.launch {
            val found = scanIwads(AppSettings.getQuakeFullDir()).sortedBy { it.file.lowercase() }
            availableIwads.clear()
            availableIwads.addAll(found)
            // Keep the project's IWAD valid; fall back to freedoom2, then the first found.
            if (found.none { it.file == project.iwadFile }) {
                val fallback = found.firstOrNull { it.file == "freedoom2.wad" }?.file
                    ?: found.firstOrNull()?.file
                if (fallback != null) project = project.copy(iwadFile = fallback)
            }
        }
    }

    // ---- project persistence ----

    suspend fun loadCurrentOrNew() {
        val loaded = ProjectStore.loadCurrent()
        if (loaded != null && loaded.maps.isNotEmpty()) {
            project = loaded
        } else {
            val lastIwad = AppSettings.getStringOption(activity, EDITOR_LAST_IWAD, null) ?: "freedoom2.wad"
            project = MapProject(iwadFile = lastIwad)
        }
        currentMapIndex = 0
        undo.clear()
        refreshUndoFlags()
    }

    suspend fun saveNamedAs(name: String): Boolean {
        rename(name)
        flushSave()
        ProjectStore.saveNamed(project)
        return true
    }

    suspend fun listProjects() = ProjectStore.list()

    suspend fun openProject(file: File) {
        val p = ProjectStore.load(file) ?: return
        project = p
        currentMapIndex = 0
        undo.clear()
        refreshUndoFlags()
        scale = 1f
        offset = Offset.Zero
        markDirty()
    }

    suspend fun deleteProject(file: File) {
        ProjectStore.delete(file)
    }

    // ---- generation / launch ----

    /** Generates the WAD; returns it (and map count) or null. Sets [isBusy]/[genStatus]. */
    suspend fun generate(): GenerateResult? {
        if (isBusy) return null
        isBusy = true
        flushSave()
        val result = try {
            generateWad(activity, project) { done, total -> genStatus = "Rendering $done/$total" }
        } finally {
            genStatus = null
            isBusy = false
        }
        return result
    }

    /** Generates then boots the engine on the chosen test map. Returns false on failure. */
    suspend fun generateAndLaunch(): Boolean {
        val result = generate() ?: return false
        return try {
            launchProject(activity, project, result)
            true
        } catch (e: java.io.IOException) {
            false
        }
    }

    /** Generates the WAD and returns its file for sharing, or null on failure. */
    suspend fun generateForShare(): File? = generate()?.wadFile

    // ---- internals ----

    private fun commitMapEdit(newMap: MapDoc) = pushUndoAndCommit(newMap)

    private fun pushUndoAndCommit(newMap: MapDoc) {
        undo.push(currentMap.copy(tiles = currentMap.tiles.copyOf()))
        refreshUndoFlags()
        setCurrentMap(newMap)
        markDirty()
    }

    private fun setCurrentMap(map: MapDoc) {
        val maps = project.maps.toMutableList()
        maps[currentMapIndex.coerceIn(0, maps.lastIndex)] = map
        project = project.copy(maps = maps)
    }

    /** Publish a new project instance (referential change) without altering content. */
    private fun republish() {
        project = project.copy()
    }

    private fun refreshUndoFlags() {
        canUndo = undo.canUndo
        canRedo = undo.canRedo
    }

    private fun markDirty() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS.milliseconds)
            ProjectStore.autosave(project)
        }
    }

    private suspend fun flushSave() {
        saveJob?.cancel()
        ProjectStore.autosave(project)
    }
}
