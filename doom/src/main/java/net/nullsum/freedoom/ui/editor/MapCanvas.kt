package net.nullsum.freedoom.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(CANVAS_BG)
            .clipToBounds()
            .graphicsLayer {
                scaleX = state.scale
                scaleY = state.scale
                translationX = state.offset.x
                translationY = state.offset.y
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val tool = state.activeTool
                    var painting = false
                    var bucketDone = false
                    var transforming = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        if (pressed.size >= 2) {
                            // Two fingers → pinch-zoom + pan. Abandon any in-progress stroke.
                            if (painting) { state.endStroke(); painting = false }
                            transforming = true
                            applyTransform(
                                state,
                                event.calculateZoom(),
                                event.calculatePan(),
                                event.calculateCentroid(),
                            )
                            event.changes.forEach { it.consume() }
                        } else {
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
                                tool == EditorTool.Bucket -> {
                                    if (!bucketDone) {
                                        screenToCell(state, change.position, size.width, size.height)
                                            ?.let { state.bucketAt(it.x, it.y) }
                                        bucketDone = true
                                    }
                                    change.consume()
                                }
                                else -> { // Brush / Eraser
                                    if (!painting) { state.beginStroke(); painting = true }
                                    screenToCell(state, change.position, size.width, size.height)
                                        ?.let { state.paintAt(it.x, it.y) }
                                    change.consume()
                                }
                            }
                        }
                    }
                    if (painting) state.endStroke()
                }
            },
    ) {
        // Subscribe to edits: project identity changes on committed edits, strokeRevision
        // bumps during a live stroke.
        @Suppress("UNUSED_EXPRESSION") state.project
        @Suppress("UNUSED_EXPRESSION") state.strokeRevision
        drawGrid(state, size)
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
