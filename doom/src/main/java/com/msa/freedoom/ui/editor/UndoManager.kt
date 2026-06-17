package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.MapDoc

/**
 * Bounded undo/redo history of whole-[MapDoc] snapshots (so it survives grid resizes
 * and theme changes, not just per-cell edits).
 *
 * Stroke coalescing is the caller's responsibility: snapshot once on pointer-down via
 * [push] and let the drag mutate freely, so a multi-cell stroke is a single undo entry.
 * Single taps, fills and erases [push] their pre-state immediately.
 */
class UndoManager(private val capacity: Int = 50) {
    private val undoStack = ArrayDeque<MapDoc>()
    private val redoStack = ArrayDeque<MapDoc>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Record [before] as a restore point. Clears the redo stack and drops the oldest entry past [capacity]. */
    fun push(before: MapDoc) {
        undoStack.addLast(before)
        if (undoStack.size > capacity) undoStack.removeFirst()
        redoStack.clear()
    }

    /** Returns the previous snapshot, pushing [current] onto the redo stack, or null if empty. */
    fun undo(current: MapDoc): MapDoc? {
        val prev = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return prev
    }

    /** Returns the next snapshot, pushing [current] back onto the undo stack, or null if empty. */
    fun redo(current: MapDoc): MapDoc? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }

    /** Drops all history (e.g. when opening a different project). */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
