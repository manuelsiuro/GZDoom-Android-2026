package net.nullsum.freedoom.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import net.nullsum.freedoom.ui.editor.model.TileType
import kotlin.math.min

private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 8f
private val GRID_LINE_COLOR = Color(0xFF3A3A3A)
private val CANVAS_BG = Color(0xFF101014)

/**
 * The interactive painting surface. The grid is drawn in unscaled "content" space and a
 * single [graphicsLayer] applies zoom/pan (the GPU does the transform, so the draw loop
 * never recomputes for pan/zoom). A single [awaitEachGesture] state machine routes input:
 * **one finger paints** with the active tool, **two fingers pinch-zoom + pan**, and the
 * explicit **Pan** tool lets one finger pan for one-handed use. All changes are consumed so
 * the host HorizontalPager never steals a horizontal paint drag.
 */
@Composable
fun MapCanvas(state: MapEditorState, modifier: Modifier = Modifier) {
    // IMPORTANT: the gesture node (Box) must NOT carry the graphicsLayer transform, and the
    // draw node (Canvas) must NOT carry the pointerInput. If both lived on one node, Compose
    // hit-testing would report pointer positions already mapped into the post-transform
    // (content) space, and screenToCell would invert the zoom/pan a second time — so after any
    // pan/zoom, taps would hit the wrong cell or fall out of bounds. Keeping input in screen
    // space (outer Box) and the transform on the inner Canvas keeps both consistent.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CANVAS_BG)
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val tool = state.activeTool
                    var painting = false
                    var bucketDone = false
                    var transforming = false
                    var shaping = false
                    // The initial down is acted on lazily, on the first follow-up event, so we
                    // can still tell a tap/drag (1 finger) from a pinch (2 fingers) and avoid a
                    // stray dot when the user starts a two-finger zoom. `seeded` guards it.
                    var seeded = false

                    fun seedDown() {
                        if (seeded) return
                        seeded = true
                        val cell = screenToCell(state, down.position, size.width, size.height)
                        when (tool) {
                            EditorTool.Pan -> {} // pan acts on movement, not the down
                            EditorTool.Eyedropper -> cell?.let { state.pickTileAt(it.x, it.y) }
                            EditorTool.Bucket -> {
                                cell?.let { state.bucketAt(it.x, it.y) }
                                bucketDone = true
                            }
                            EditorTool.Line, EditorTool.Rect -> cell?.let {
                                state.startShape(it.x, it.y)
                                shaping = true
                            }
                            else -> { // Brush / Eraser
                                state.beginStroke()
                                painting = true
                                cell?.let { state.paintAt(it.x, it.y) }
                            }
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.size >= 2) {
                            // Two fingers → pinch-zoom + pan. Commit to transform: never seed a
                            // paint, and abandon any in-progress stroke or shape.
                            if (painting) { state.endStroke(); painting = false }
                            if (shaping) { state.cancelShape(); shaping = false }
                            seeded = true
                            transforming = true
                            applyTransform(
                                state,
                                event.calculateZoom(),
                                event.calculatePan(),
                                event.calculateCentroid(),
                            )
                            event.changes.forEach { it.consume() }
                            continue
                        }

                        // Zero or one pointer: first single-pointer event means this is a
                        // tap/drag (not a pinch), so it's now safe to act on the initial down.
                        if (!transforming) seedDown()

                        if (pressed.isEmpty()) break // gesture ended

                        val change = pressed.first()
                        when {
                            transforming -> {
                                // Winding down from a pinch: keep panning, don't paint a jump.
                                state.offset += change.positionChange()
                                change.consume()
                            }
                            tool == EditorTool.Pan -> {
                                state.offset += change.positionChange()
                                change.consume()
                            }
                            tool == EditorTool.Bucket || tool == EditorTool.Eyedropper -> {
                                change.consume() // these fire once on the seeded down
                            }
                            tool == EditorTool.Line || tool == EditorTool.Rect -> {
                                if (shaping) {
                                    screenToCell(state, change.position, size.width, size.height)
                                        ?.let { state.updateShape(it.x, it.y) }
                                }
                                change.consume()
                            }
                            else -> { // Brush / Eraser: paint each dragged-over cell
                                screenToCell(state, change.position, size.width, size.height)
                                    ?.let { state.paintAt(it.x, it.y) }
                                change.consume()
                            }
                        }
                    }
                    if (painting) state.endStroke()
                    if (shaping) state.commitShape()
                }
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            // Subscribe to edits: project identity changes on committed edits, strokeRevision
            // bumps during a live stroke.
            @Suppress("UNUSED_EXPRESSION") state.project
            @Suppress("UNUSED_EXPRESSION") state.strokeRevision
            @Suppress("UNUSED_EXPRESSION") state.shapePreview
            drawGrid(state, size)
        }
    }
}

private fun DrawScope.drawGrid(state: MapEditorState, size: Size) {
    val map = state.currentMap
    val gw = map.width
    val gh = map.height
    val cell = min(size.width / gw, size.height / gh)
    if (cell <= 0f) return

    for (y in 0 until gh) {
        for (x in 0 until gw) {
            val tile = TileType.fromOrdinal(map.tiles[y * gw + x])
            drawRect(tile.composeColor, Offset(x * cell, y * cell), Size(cell, cell))
        }
    }

    // Grid lines only when each cell is large enough on screen to be worth drawing.
    if (cell * state.scale >= 6f) {
        val w = cell * gw
        val h = cell * gh
        val stroke = (1f / state.scale).coerceAtLeast(0.5f)
        for (i in 0..gw) {
            drawLine(GRID_LINE_COLOR, Offset(i * cell, 0f), Offset(i * cell, h), stroke)
        }
        for (i in 0..gh) {
            drawLine(GRID_LINE_COLOR, Offset(0f, i * cell), Offset(w, i * cell), stroke)
        }
    }

    // Live overlay of the line/rectangle being dragged out (drawn translucent on top), mirrored
    // to match the active symmetry so the preview shows exactly what will be committed.
    state.shapePreview?.let { p ->
        val base = if (p.isLine) {
            MapGridOps.lineCells(gw, gh, p.sx, p.sy, p.ex, p.ey)
        } else {
            MapGridOps.rectCells(gw, gh, p.sx, p.sy, p.ex, p.ey, p.filled)
        }
        val cells = MapGridOps.expandSymmetry(base, gw, gh, state.symmetry)
        val color = p.tile.composeColor.copy(alpha = 0.7f)
        for (idx in cells) {
            val cx = idx % gw
            val cy = idx / gw
            drawRect(color, Offset(cx * cell, cy * cell), Size(cell, cell))
        }
    }
}

/** Applies a pinch (zoom around the centroid) plus pan, clamping scale. */
private fun applyTransform(state: MapEditorState, zoom: Float, pan: Offset, centroid: Offset) {
    val newScale = (state.scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
    val k = newScale / state.scale
    // Keep the content point under the centroid stationary, then add the pan.
    state.offset = centroid + pan - (centroid - state.offset) * k
    state.scale = newScale
}

/** Inverts the zoom/pan transform to find the grid cell under a screen-local position. */
private fun screenToCell(state: MapEditorState, pos: Offset, viewW: Int, viewH: Int): IntOffset? {
    val map = state.currentMap
    val cell = min(viewW.toFloat() / map.width, viewH.toFloat() / map.height)
    if (cell <= 0f) return null
    val contentX = (pos.x - state.offset.x) / state.scale
    val contentY = (pos.y - state.offset.y) / state.scale
    val col = (contentX / cell).toInt()
    val row = (contentY / cell).toInt()
    if (col < 0 || row < 0 || col >= map.width || row >= map.height) return null
    // Guard the fractional-truncation edge (negative coords already filtered above).
    if (contentX < 0f || contentY < 0f) return null
    return IntOffset(col, row)
}
