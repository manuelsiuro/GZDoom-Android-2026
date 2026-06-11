package net.nullsum.freedoom.ui.editor

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
import net.nullsum.freedoom.AppSettings
import net.nullsum.freedoom.ui.editor.data.ProjectStore
import net.nullsum.freedoom.ui.editor.generate.GenerateResult
import net.nullsum.freedoom.ui.editor.generate.generateWad
import net.nullsum.freedoom.ui.editor.launch.launchProject
import net.nullsum.freedoom.ui.editor.model.MapDoc
import net.nullsum.freedoom.ui.editor.model.MapProject
import net.nullsum.freedoom.ui.editor.model.MapTheme
import net.nullsum.freedoom.ui.editor.model.TileType
import net.nullsum.freedoom.ui.editor.model.Tuning
import net.nullsum.freedoom.ui.editor.model.WadFormat
import net.nullsum.freedoom.ui.launch.WadEntry
import net.nullsum.freedoom.ui.launch.scanIwads
import java.io.File

/** The active editing tool. Pan lets a one-finger drag move the view (two fingers always transform). */
enum class EditorTool { Brush, Eraser, Bucket, Pan }

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

    /** Paint one cell with the active tool (Brush → selected tile, Eraser → Room). In place. */
    fun paintAt(x: Int, y: Int) {
        val m = currentMap
        if (x !in 0 until m.width || y !in 0 until m.height) return
        val value = if (activeTool == EditorTool.Eraser) TileType.Room.ordinal else selectedTile.ordinal
        val idx = y * m.width + x
        if (m.tiles[idx] == value) return
        m.tiles[idx] = value
        strokeRevision++
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

    /** Flood-fill from a cell with the selected tile (no-op for the Pan/Eraser-less paths). */
    fun bucketAt(x: Int, y: Int) {
        val m = currentMap
        val replacement = if (activeTool == EditorTool.Eraser) TileType.Room else selectedTile
        val filled = MapGridOps.floodFill(m.tiles, m.width, m.height, x, y, replacement) ?: return
        commitMapEdit(m.copy(tiles = filled))
    }

    // ---- undo / redo ----

    fun undo() {
        val restored = undo.undo(currentMap) ?: return
        setCurrentMap(restored)
        refreshUndoFlags()
        markDirty()
    }

    fun redo() {
        val restored = undo.redo(currentMap) ?: return
        setCurrentMap(restored)
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
        launchProject(activity, project, result)
        return true
    }

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
            delay(AUTOSAVE_DEBOUNCE_MS)
            ProjectStore.autosave(project)
        }
    }

    private suspend fun flushSave() {
        saveJob?.cancel()
        ProjectStore.autosave(project)
    }
}
