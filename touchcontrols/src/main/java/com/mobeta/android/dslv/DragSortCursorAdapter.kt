@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.mobeta.android.dslv

import android.content.Context
import android.database.Cursor
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter

/**
 * A subclass of [CursorAdapter] that provides
 * reordering of the elements in the Cursor based on completed
 * drag-sort operations. The reordering is a simple mapping of
 * list positions into Cursor positions (the Cursor is unchanged).
 * To persist changes made by drag-sorts, one can retrieve the
 * mapping with the [getCursorPositions] method, which
 * returns the reordered list of Cursor positions.
 *
 * An instance of this class is passed
 * to [DragSortListView.setAdapter] and, since
 * this class implements the [DragSortListView.DragSortListener]
 * interface, it is automatically set as the DragSortListener for
 * the DragSortListView instance.
 */
abstract class DragSortCursorAdapter : CursorAdapter, DragSortListView.DragSortListener {

    /**
     * Key is ListView position, value is Cursor position
     */
    private val mListMapping = SparseIntArray()

    private val mRemovedCursorPositions = ArrayList<Int>()

    constructor(context: Context, c: Cursor?) : super(context, c)

    constructor(context: Context, c: Cursor?, autoRequery: Boolean) : super(
        context,
        c,
        autoRequery
    )

    constructor(context: Context, c: Cursor?, flags: Int) : super(context, c, flags)

    /**
     * Swaps Cursor and clears list-Cursor mapping.
     *
     * @see CursorAdapter.swapCursor
     */
    override fun swapCursor(newCursor: Cursor?): Cursor? {
        val old = super.swapCursor(newCursor)
        resetMappings()
        return old
    }

    /**
     * Changes Cursor and clears list-Cursor mapping.
     *
     * @see CursorAdapter.changeCursor
     */
    override fun changeCursor(cursor: Cursor?) {
        super.changeCursor(cursor)
        resetMappings()
    }

    /**
     * Resets list-cursor mapping.
     */
    fun reset() {
        resetMappings()
        notifyDataSetChanged()
    }

    private fun resetMappings() {
        mListMapping.clear()
        mRemovedCursorPositions.clear()
    }

    override fun getItem(position: Int): Any {
        return super.getItem(mListMapping.get(position, position))
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(mListMapping.get(position, position))
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(mListMapping.get(position, position), convertView, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(mListMapping.get(position, position), convertView, parent)
    }

    /**
     * On drop, this updates the mapping between Cursor positions
     * and ListView positions. The Cursor is unchanged. Retrieve
     * the current mapping with [getCursorPositions].
     *
     * @see DragSortListView.DropListener.drop
     */
    override fun drop(from: Int, to: Int) {
        if (from != to) {
            val cursorFrom = mListMapping.get(from, from)

            if (from > to) {
                var i = from
                while (i > to) {
                    mListMapping.put(i, mListMapping.get(i - 1, i - 1))
                    --i
                }
            } else {
                var i = from
                while (i < to) {
                    mListMapping.put(i, mListMapping.get(i + 1, i + 1))
                    ++i
                }
            }
            mListMapping.put(to, cursorFrom)

            cleanMapping()
            notifyDataSetChanged()
        }
    }

    /**
     * On remove, this updates the mapping between Cursor positions
     * and ListView positions. The Cursor is unchanged. Retrieve
     * the current mapping with [getCursorPositions].
     *
     * @see DragSortListView.RemoveListener.remove
     */
    override fun remove(which: Int) {
        val cursorPos = mListMapping.get(which, which)
        if (!mRemovedCursorPositions.contains(cursorPos)) {
            mRemovedCursorPositions.add(cursorPos)
        }

        val newCount = count
        var i = which
        while (i < newCount) {
            mListMapping.put(i, mListMapping.get(i + 1, i + 1))
            ++i
        }

        mListMapping.delete(newCount)

        cleanMapping()
        notifyDataSetChanged()
    }

    /**
     * Does nothing. Just completes DragSortListener interface.
     */
    override fun drag(from: Int, to: Int) {
        // do nothing
    }

    /**
     * Remove unnecessary mappings from sparse array.
     */
    private fun cleanMapping() {
        val toRemove = ArrayList<Int>()

        var size = mListMapping.size()
        for (i in 0 until size) {
            if (mListMapping.keyAt(i) == mListMapping.valueAt(i)) {
                toRemove.add(mListMapping.keyAt(i))
            }
        }

        size = toRemove.size
        for (i in 0 until size) {
            mListMapping.delete(toRemove[i])
        }
    }

    override fun getCount(): Int {
        return super.getCount() - mRemovedCursorPositions.size
    }

    /**
     * Get the Cursor position mapped to by the provided list position
     * (given all previously handled drag-sort
     * operations).
     *
     * @param position List position
     * @return The mapped-to Cursor position
     */
    fun getCursorPosition(position: Int): Int {
        return mListMapping.get(position, position)
    }

    /**
     * Get the current order of Cursor positions presented by the
     * list.
     */
    fun getCursorPositions(): ArrayList<Int> {
        val result = ArrayList<Int>()

        for (i in 0 until count) {
            result.add(mListMapping.get(i, i))
        }

        return result
    }

    /**
     * Get the list position mapped to by the provided Cursor position.
     * If the provided Cursor position has been removed by a drag-sort,
     * this returns [REMOVED].
     *
     * @param cursorPosition A Cursor position
     * @return The mapped-to list position or REMOVED
     */
    fun getListPosition(cursorPosition: Int): Int {
        if (mRemovedCursorPositions.contains(cursorPosition)) {
            return REMOVED
        }

        val index = mListMapping.indexOfValue(cursorPosition)
        return if (index < 0) {
            cursorPosition
        } else {
            mListMapping.keyAt(index)
        }
    }

    companion object {
        const val REMOVED = -1
    }
}
