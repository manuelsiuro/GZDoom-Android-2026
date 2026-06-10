@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.mobeta.android.dslv

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView

/**
 * Simple implementation of the FloatViewManager class. Uses list
 * items as they appear in the ListView to create the floating View.
 */
open class SimpleFloatViewManager(private val mListView: ListView) :
    DragSortListView.FloatViewManager {

    private var mFloatBitmap: Bitmap? = null

    private var mImageView: ImageView? = null

    private var mFloatBGColor = Color.BLACK

    fun setBackgroundColor(color: Int) {
        mFloatBGColor = color
    }

    /**
     * This simple implementation creates a Bitmap copy of the
     * list item currently shown at ListView `position`.
     */
    override fun onCreateFloatView(position: Int): View? {
        // Guaranteed that this will not be null? I think so. Nope, got
        // a NullPointerException once...
        val v = mListView.getChildAt(
            position + mListView.headerViewsCount - mListView.firstVisiblePosition
        ) ?: return null

        v.isPressed = false

        // Create a copy of the drawing cache so that it does not get
        // recycled by the framework when the list tries to clean up memory
        //v.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        v.isDrawingCacheEnabled = true
        mFloatBitmap = Bitmap.createBitmap(v.drawingCache)
        v.isDrawingCacheEnabled = false

        if (mImageView == null) {
            mImageView = ImageView(mListView.context)
        }
        mImageView!!.setBackgroundColor(mFloatBGColor)
        mImageView!!.setPadding(0, 0, 0, 0)
        mImageView!!.setImageBitmap(mFloatBitmap)
        mImageView!!.layoutParams = ViewGroup.LayoutParams(v.width, v.height)

        return mImageView
    }

    /**
     * This does nothing
     */
    override fun onDragFloatView(floatView: View, position: Point, touch: Point) {
        // do nothing
    }

    /**
     * Removes the Bitmap from the ImageView created in
     * onCreateFloatView() and tells the system to recycle it.
     */
    override fun onDestroyFloatView(floatView: View) {
        (floatView as ImageView).setImageDrawable(null)

        mFloatBitmap?.recycle()
        mFloatBitmap = null
    }
}
