@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

/*
 * DragSortListView.
 *
 * A subclass of the Android ListView component that enables drag
 * and drop re-ordering of list items.
 *
 * Copyright 2012 Carl Bauer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobeta.android.dslv

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.util.SparseBooleanArray
import android.util.SparseIntArray
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Checkable
import android.widget.ListAdapter
import android.widget.ListView
import com.beloko.touchcontrols.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ListView subclass that mediates drag and drop resorting of items.
 *
 * @author heycosmo
 */
class DragSortListView(context: Context, attrs: AttributeSet?) : ListView(context, attrs) {

    /**
     * The View that floats above the ListView and represents
     * the dragged item.
     */
    private var mFloatView: View? = null

    /**
     * The float View location. First based on touch location
     * and given deltaX and deltaY. Then restricted by callback
     * to FloatViewManager.onDragFloatView(). Finally restricted
     * by bounds of DSLV.
     */
    private val mFloatLoc = Point()
    private val mTouchLoc = Point()

    /**
     * The middle (in the y-direction) of the floating View.
     */
    private var mFloatViewMid = 0

    /**
     * Flag to make sure float View isn't measured twice
     */
    private var mFloatViewOnMeasured = false

    /**
     * Watch the Adapter for data changes. Cancel a drag if
     * coincident with a change.
     */
    private var mObserver: DataSetObserver

    /**
     * Transparency for the floating View (XML attribute).
     */
    private var mFloatAlpha = 1.0f
    private var mCurrFloatAlpha = 1.0f

    /**
     * While drag-sorting, the current position of the floating
     * View. If dropped, the dragged item will land in this position.
     */
    private var mFloatPos = 0

    /**
     * The first expanded ListView position that helps represent
     * the drop slot tracking the floating View.
     */
    private var mFirstExpPos = 0

    /**
     * The second expanded ListView position that helps represent
     * the drop slot tracking the floating View. This can equal
     * mFirstExpPos if there is no slide shuffle occurring; otherwise
     * it is equal to mFirstExpPos + 1.
     */
    private var mSecondExpPos = 0

    /**
     * Flag set if slide shuffling is enabled.
     */
    private var mAnimate = false

    /**
     * The user dragged from this position.
     */
    private var mSrcPos = 0

    /**
     * Offset (in x) within the dragged item at which the user
     * picked it up (or first touched down with the digitalis).
     */
    private var mDragDeltaX = 0

    /**
     * Offset (in y) within the dragged item at which the user
     * picked it up (or first touched down with the digitalis).
     */
    private var mDragDeltaY = 0

    /**
     * The difference (in x) between screen coordinates and coordinates
     * in this view.
     */
    private var mOffsetX = 0

    /**
     * The difference (in y) between screen coordinates and coordinates
     * in this view.
     */
    private var mOffsetY = 0

    /**
     * A listener that receives callbacks whenever the floating View
     * hovers over a new position.
     */
    private var mDragListener: DragListener? = null

    /**
     * A listener that receives a callback when the floating View
     * is dropped.
     */
    private var mDropListener: DropListener? = null

    /**
     * A listener that receives a callback when the floating View
     * (or more precisely the originally dragged item) is removed
     * by one of the provided gestures.
     */
    private var mRemoveListener: RemoveListener? = null

    /**
     * Enable/Disable item dragging
     */
    private var mDragEnabled = true
    private var mDragState = IDLE

    /**
     * Height in pixels to which the originally dragged item
     * is collapsed during a drag-sort. Currently, this value
     * must be greater than zero.
     */
    private var mItemHeightCollapsed = 1

    /**
     * Height of the floating View. Stored for the purpose of
     * providing the tracking drop slot.
     */
    private var mFloatViewHeight = 0

    /**
     * Convenience member. See above.
     */
    private var mFloatViewHeightHalf = 0

    /**
     * Save the given width spec for use in measuring children
     */
    private var mWidthMeasureSpec = 0

    /**
     * Sample Views ultimately used for calculating the height
     * of ListView items that are off-screen.
     */
    private var mSampleViewTypes = arrayOfNulls<View>(1)

    /**
     * Drag-scroll encapsulator!
     */
    private val mDragScroller: DragScroller

    /**
     * Determines the start of the upward drag-scroll region
     * at the top of the ListView. Specified by a fraction
     * of the ListView height, thus screen resolution agnostic.
     */
    private var mDragUpScrollStartFrac = 1.0f / 3.0f

    /**
     * Determines the start of the downward drag-scroll region
     * at the bottom of the ListView. Specified by a fraction
     * of the ListView height, thus screen resolution agnostic.
     */
    private var mDragDownScrollStartFrac = 1.0f / 3.0f

    /**
     * The following are calculated from the above fracs.
     */
    private var mUpScrollStartY = 0
    private var mDownScrollStartY = 0
    private var mDownScrollStartYF = 0f
    private var mUpScrollStartYF = 0f

    /**
     * Calculated from above above and current ListView height.
     */
    private var mDragUpScrollHeight = 0f

    /**
     * Calculated from above above and current ListView height.
     */
    private var mDragDownScrollHeight = 0f

    /**
     * Maximum drag-scroll speed in pixels per ms. Only used with
     * default linear drag-scroll profile.
     */
    private var mMaxScrollSpeed = 0.5f

    /**
     * Defines the scroll speed during a drag-scroll. User can
     * provide their own; this default is a simple linear profile
     * where scroll speed increases linearly as the floating View
     * nears the top/bottom of the ListView.
     */
    private var mScrollProfile = DragScrollProfile { w, _ -> mMaxScrollSpeed * w }

    /**
     * Current touch x.
     */
    private var mX = 0

    /**
     * Current touch y.
     */
    private var mY = 0

    /**
     * Last touch x.
     */
    private var mLastX = 0

    /**
     * Last touch y.
     */
    private var mLastY = 0

    /**
     * The touch y-coord at which drag started
     */
    private var mDragStartY = 0

    /**
     * Flags that determine limits on the motion of the
     * floating View. See flags above.
     */
    private var mDragFlags = 0

    /**
     * Last call to an on*TouchEvent was a call to
     * onInterceptTouchEvent.
     */
    private var mLastCallWasIntercept = false

    /**
     * A touch event is in progress.
     */
    private var mInTouchEvent = false

    /**
     * Let the user customize the floating View.
     */
    private var mFloatViewManager: FloatViewManager? = null

    /**
     * Given to ListView to cancel its action when a drag-sort
     * begins.
     */
    private var mCancelEvent: MotionEvent

    /**
     * Where to cancel the ListView action when a
     * drag-sort begins
     */
    private var mCancelMethod = NO_CANCEL

    /**
     * Determines when a slide shuffle animation starts. That is,
     * defines how close to the edge of the drop slot the floating
     * View must be to initiate the slide.
     */
    private var mSlideRegionFrac = 0.25f

    /**
     * Number between 0 and 1 indicating the relative location of
     * a sliding item (only used if drag-sort animations
     * are turned on). Nearly 1 means the item is
     * at the top of the slide region (nearly full blank item
     * is directly below).
     */
    private var mSlideFrac = 0.0f

    /**
     * Wraps the user-provided ListAdapter. This is used to wrap each
     * item View given by the user inside another View (currently
     * a RelativeLayout) which
     * expands and collapses to simulate the item shuffling.
     */
    private var mAdapterWrapper: AdapterWrapper? = null

    /**
     * Turn on custom debugger.
     */
    private var mTrackDragSort = false

    /**
     * Debugging class.
     */
    private var mDragSortTracker: DragSortTracker? = null

    /**
     * Needed for adjusting item heights from within layoutChildren
     */
    private var mBlockLayoutRequests = false

    /**
     * Set to true when a down event happens during drag sort;
     * for example, when drag finish animations are
     * playing.
     */
    private var mIgnoreTouchEvent = false
    private val mChildHeightCache = HeightCache(sCacheSize)

    private var mRemoveAnimator: RemoveAnimator? = null

    private var mLiftAnimator: LiftAnimator? = null

    private var mDropAnimator: DropAnimator? = null

    private var mUseRemoveVelocity = false
    private var mRemoveVelocityX = 0f
    private var mListViewIntercepted = false
    private var mFloatViewInvalidated = false

    init {
        val defaultDuration = 150
        var removeAnimDuration = defaultDuration // ms
        var dropAnimDuration = defaultDuration // ms

        if (attrs != null) {
            val a = getContext().obtainStyledAttributes(
                attrs,
                R.styleable.DragSortListView, 0, 0
            )

            mItemHeightCollapsed = max(
                1,
                a.getDimensionPixelSize(R.styleable.DragSortListView_collapsed_height, 1)
            )

            mTrackDragSort = a.getBoolean(R.styleable.DragSortListView_track_drag_sort, false)

            if (mTrackDragSort) {
                mDragSortTracker = DragSortTracker()
            }

            // alpha between 0 and 255, 0=transparent, 255=opaque
            mFloatAlpha = a.getFloat(R.styleable.DragSortListView_float_alpha, mFloatAlpha)
            mCurrFloatAlpha = mFloatAlpha

            mDragEnabled = a.getBoolean(R.styleable.DragSortListView_drag_enabled, mDragEnabled)

            mSlideRegionFrac = max(
                0.0f,
                min(
                    1.0f,
                    1.0f - a.getFloat(R.styleable.DragSortListView_slide_shuffle_speed, 0.75f)
                )
            )

            mAnimate = mSlideRegionFrac > 0.0f

            val frac = a.getFloat(
                R.styleable.DragSortListView_drag_scroll_start,
                mDragUpScrollStartFrac
            )

            setDragScrollStart(frac)

            mMaxScrollSpeed = a.getFloat(
                R.styleable.DragSortListView_max_drag_scroll_speed,
                mMaxScrollSpeed
            )

            removeAnimDuration = a.getInt(
                R.styleable.DragSortListView_remove_animation_duration,
                removeAnimDuration
            )

            dropAnimDuration = a.getInt(
                R.styleable.DragSortListView_drop_animation_duration,
                dropAnimDuration
            )

            val useDefault = a.getBoolean(
                R.styleable.DragSortListView_use_default_controller,
                true
            )

            if (useDefault) {
                val removeEnabled = a.getBoolean(
                    R.styleable.DragSortListView_remove_enabled,
                    false
                )
                val removeMode = a.getInt(
                    R.styleable.DragSortListView_remove_mode,
                    DragSortController.FLING_REMOVE
                )
                val sortEnabled = a.getBoolean(
                    R.styleable.DragSortListView_sort_enabled,
                    true
                )
                val dragInitMode = a.getInt(
                    R.styleable.DragSortListView_drag_start_mode,
                    DragSortController.ON_DOWN
                )
                val dragHandleId = a.getResourceId(
                    R.styleable.DragSortListView_drag_handle_id,
                    0
                )
                val flingHandleId = a.getResourceId(
                    R.styleable.DragSortListView_fling_handle_id,
                    0
                )
                val clickRemoveId = a.getResourceId(
                    R.styleable.DragSortListView_click_remove_id,
                    0
                )
                val bgColor = a.getColor(
                    R.styleable.DragSortListView_float_background_color,
                    Color.BLACK
                )

                val controller = DragSortController(
                    this, dragHandleId, dragInitMode, removeMode,
                    clickRemoveId, flingHandleId
                )
                controller.setRemoveEnabled(removeEnabled)
                controller.setSortEnabled(sortEnabled)
                controller.setBackgroundColor(bgColor)

                mFloatViewManager = controller
                setOnTouchListener(controller)
            }

            a.recycle()
        }

        mDragScroller = DragScroller()

        val smoothness = 0.5f
        if (removeAnimDuration > 0) {
            mRemoveAnimator = RemoveAnimator(smoothness, removeAnimDuration)
        }
        // mLiftAnimator = new LiftAnimator(smoothness, 100);
        if (dropAnimDuration > 0) {
            mDropAnimator = DropAnimator(smoothness, dropAnimDuration)
        }

        mCancelEvent = MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0f, 0f, 0, 0f,
            0f, 0, 0
        )

        // construct the dataset observer
        mObserver = object : DataSetObserver() {
            private fun cancel() {
                if (mDragState == DRAGGING) {
                    cancelDrag()
                }
            }

            override fun onChanged() {
                cancel()
            }

            override fun onInvalidated() {
                cancel()
            }
        }
    }

    fun getFloatAlpha(): Float {
        return mCurrFloatAlpha
    }

    /**
     * Usually called from a FloatViewManager. The float alpha
     * will be reset to the xml-defined value every time a drag
     * is stopped.
     */
    fun setFloatAlpha(alpha: Float) {
        mCurrFloatAlpha = alpha
    }

    /**
     * Set maximum drag scroll speed in positions/second. Only applies
     * if using default ScrollSpeedProfile.
     *
     * @param max Maximum scroll speed.
     */
    fun setMaxScrollSpeed(max: Float) {
        mMaxScrollSpeed = max
    }

    /**
     * For each DragSortListView Listener interface implemented by
     * `adapter`, this method calls the appropriate
     * set*Listener method with `adapter` as the argument.
     *
     * @param adapter The ListAdapter providing data to back
     * DragSortListView.
     * @see android.widget.ListView.setAdapter
     */
    override fun setAdapter(adapter: ListAdapter?) {
        if (adapter != null) {
            mAdapterWrapper = AdapterWrapper(adapter)
            adapter.registerDataSetObserver(mObserver)

            if (adapter is DropListener) {
                setDropListener(adapter as DropListener)
            }
            if (adapter is DragListener) {
                setDragListener(adapter as DragListener)
            }
            if (adapter is RemoveListener) {
                setRemoveListener(adapter as RemoveListener)
            }
        } else {
            mAdapterWrapper = null
        }

        super.setAdapter(mAdapterWrapper)
    }

    /**
     * As opposed to [ListView.getAdapter], which returns
     * a heavily wrapped ListAdapter (DragSortListView wraps the
     * input ListAdapter and ListView wraps the wrapped one).
     *
     * @return The ListAdapter set as the argument of setAdapter()
     */
    fun getInputAdapter(): ListAdapter? {
        return mAdapterWrapper?.getAdapter()
    }

    private fun drawDivider(expPosition: Int, canvas: Canvas) {
        val divider = divider
        val dividerHeight = dividerHeight
        // Log.d("mobeta", "div=" + divider + " divH=" + dividerHeight);

        if (divider != null && dividerHeight != 0) {
            val expItem = getChildAt(expPosition - firstVisiblePosition) as ViewGroup?
            if (expItem != null) {
                val l = paddingLeft
                val r = width - paddingRight
                val t: Int
                val b: Int

                val childHeight = expItem.getChildAt(0).height

                if (expPosition > mSrcPos) {
                    t = expItem.top + childHeight
                    b = t + dividerHeight
                } else {
                    b = expItem.bottom - childHeight
                    t = b - dividerHeight
                }

                // Have to clip to support ColorDrawable on <= Gingerbread
                canvas.save()
                canvas.clipRect(l, t, r, b)
                divider.setBounds(l, t, r, b)
                divider.draw(canvas)
                canvas.restore()
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (mDragState != IDLE) {
            // draw the divider over the expanded item
            if (mFirstExpPos != mSrcPos) {
                drawDivider(mFirstExpPos, canvas)
            }
            if (mSecondExpPos != mFirstExpPos && mSecondExpPos != mSrcPos) {
                drawDivider(mSecondExpPos, canvas)
            }
        }

        val floatView = mFloatView
        if (floatView != null) {
            // draw the float view over everything
            val w = floatView.width
            val h = floatView.height

            var x = mFloatLoc.x

            val width = width
            if (x < 0) {
                x = -x
            }
            var alphaMod: Float
            if (x < width) {
                alphaMod = (width - x).toFloat() / width.toFloat()
                alphaMod *= alphaMod
            } else {
                alphaMod = 0f
            }

            val alpha = (255f * mCurrFloatAlpha * alphaMod).toInt()

            canvas.save()
            canvas.translate(mFloatLoc.x.toFloat(), mFloatLoc.y.toFloat())
            canvas.clipRect(0, 0, w, h)

            canvas.saveLayerAlpha(0f, 0f, w.toFloat(), h.toFloat(), alpha)
            floatView.draw(canvas)
            canvas.restore()
            canvas.restore()
        }
    }

    private fun getItemHeight(position: Int): Int {
        val v = getChildAt(position - firstVisiblePosition)

        return if (v != null) {
            // item is onscreen, just get the height of the View
            v.height
        } else {
            // item is offscreen. get child height and calculate
            // item height based on current shuffle state
            calcItemHeight(position, getChildHeight(position))
        }
    }

    private fun printPosData() {
        Log.d(
            "mobeta",
            "mSrcPos=$mSrcPos mFirstExpPos=$mFirstExpPos mSecondExpPos=$mSecondExpPos"
        )
    }

    /**
     * Get the shuffle edge for item at position when top of
     * item is at y-coord top.
     */
    private fun getShuffleEdge(position: Int, top: Int): Int {
        val numHeaders = headerViewsCount
        val numFooters = footerViewsCount

        // shuffle edges are defined between items that can be
        // dragged; there are N-1 of them if there are N draggable
        // items.

        if (position <= numHeaders || position >= count - numFooters) {
            return top
        }

        val divHeight = dividerHeight

        val edge: Int

        val maxBlankHeight = mFloatViewHeight - mItemHeightCollapsed
        val childHeight = getChildHeight(position)
        val itemHeight = getItemHeight(position)

        // first calculate top of item given that floating View is
        // centered over src position
        var otop = top
        if (mSecondExpPos <= mSrcPos) {
            // items are expanded on and/or above the source position

            if (position == mSecondExpPos && mFirstExpPos != mSecondExpPos) {
                if (position == mSrcPos) {
                    otop = top + itemHeight - mFloatViewHeight
                } else {
                    val blankHeight = itemHeight - childHeight
                    otop = top + blankHeight - maxBlankHeight
                }
            } else if (position > mSecondExpPos && position <= mSrcPos) {
                otop = top - maxBlankHeight
            }
        } else {
            // items are expanded on and/or below the source position

            if (position > mSrcPos && position <= mFirstExpPos) {
                otop = top + maxBlankHeight
            } else if (position == mSecondExpPos) {
                val blankHeight = itemHeight - childHeight
                otop = top + blankHeight
            }
        }

        // otop is set
        edge = if (position <= mSrcPos) {
            otop + (mFloatViewHeight - divHeight - getChildHeight(position - 1)) / 2
        } else {
            otop + (childHeight - divHeight - mFloatViewHeight) / 2
        }

        return edge
    }

    private fun updatePositions(): Boolean {
        val first = firstVisiblePosition
        var startPos = mFirstExpPos
        var startView = getChildAt(startPos - first)

        if (startView == null) {
            startPos = first + childCount / 2
            startView = getChildAt(startPos - first)
        }
        val startTop = startView.top

        var itemHeight = startView.height

        var edge = getShuffleEdge(startPos, startTop)
        var lastEdge = edge

        val divHeight = dividerHeight

        var itemPos = startPos
        var itemTop = startTop
        if (mFloatViewMid < edge) {
            // scanning up for float position
            while (itemPos >= 0) {
                itemPos--
                itemHeight = getItemHeight(itemPos)

                if (itemPos == 0) {
                    edge = itemTop - divHeight - itemHeight
                    break
                }

                itemTop -= itemHeight + divHeight
                edge = getShuffleEdge(itemPos, itemTop)

                if (mFloatViewMid >= edge) {
                    break
                }

                lastEdge = edge
            }
        } else {
            // scanning down for float position
            val count = count
            while (itemPos < count) {
                if (itemPos == count - 1) {
                    edge = itemTop + divHeight + itemHeight
                    break
                }

                itemTop += divHeight + itemHeight
                itemHeight = getItemHeight(itemPos + 1)
                edge = getShuffleEdge(itemPos + 1, itemTop)

                // test for hit
                if (mFloatViewMid < edge) {
                    break
                }

                lastEdge = edge
                itemPos++
            }
        }

        val numHeaders = headerViewsCount
        val numFooters = footerViewsCount

        var updated = false

        val oldFirstExpPos = mFirstExpPos
        val oldSecondExpPos = mSecondExpPos
        val oldSlideFrac = mSlideFrac

        if (mAnimate) {
            val edgeToEdge = abs(edge - lastEdge)

            val edgeTop: Int
            val edgeBottom: Int
            if (mFloatViewMid < edge) {
                edgeBottom = edge
                edgeTop = lastEdge
            } else {
                edgeTop = edge
                edgeBottom = lastEdge
            }

            val slideRgnHeight = (0.5f * mSlideRegionFrac * edgeToEdge).toInt()
            val slideRgnHeightF = slideRgnHeight.toFloat()
            val slideEdgeTop = edgeTop + slideRgnHeight
            val slideEdgeBottom = edgeBottom - slideRgnHeight

            // Three regions
            if (mFloatViewMid < slideEdgeTop) {
                mFirstExpPos = itemPos - 1
                mSecondExpPos = itemPos
                mSlideFrac = 0.5f * (slideEdgeTop - mFloatViewMid).toFloat() / slideRgnHeightF
            } else if (mFloatViewMid < slideEdgeBottom) {
                mFirstExpPos = itemPos
                mSecondExpPos = itemPos
            } else {
                mFirstExpPos = itemPos
                mSecondExpPos = itemPos + 1
                mSlideFrac = 0.5f * (1.0f + (edgeBottom - mFloatViewMid).toFloat() / slideRgnHeightF)
            }
        } else {
            mFirstExpPos = itemPos
            mSecondExpPos = itemPos
        }

        // correct for headers and footers
        if (mFirstExpPos < numHeaders) {
            itemPos = numHeaders
            mFirstExpPos = itemPos
            mSecondExpPos = itemPos
        } else if (mSecondExpPos >= count - numFooters) {
            itemPos = count - numFooters - 1
            mFirstExpPos = itemPos
            mSecondExpPos = itemPos
        }

        if (mFirstExpPos != oldFirstExpPos || mSecondExpPos != oldSecondExpPos ||
            mSlideFrac != oldSlideFrac
        ) {
            updated = true
        }

        if (itemPos != mFloatPos) {
            mDragListener?.drag(mFloatPos - numHeaders, itemPos - numHeaders)

            mFloatPos = itemPos
            updated = true
        }

        return updated
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mTrackDragSort) {
            mDragSortTracker!!.appendState()
        }
    }

    fun removeItem(which: Int) {
        mUseRemoveVelocity = false
        removeItem(which, 0f)
    }

    /**
     * Removes an item from the list and animates the removal.
     *
     * @param which     Position to remove (NOTE: headers/footers ignored!
     * this is a position in your input ListAdapter).
     * @param velocityX
     */
    fun removeItem(which: Int, velocityX: Float) {
        if (mDragState == IDLE || mDragState == DRAGGING) {

            if (mDragState == IDLE) {
                // called from outside drag-sort
                mSrcPos = headerViewsCount + which
                mFirstExpPos = mSrcPos
                mSecondExpPos = mSrcPos
                mFloatPos = mSrcPos
                val v = getChildAt(mSrcPos - firstVisiblePosition)
                if (v != null) {
                    v.visibility = INVISIBLE
                }
            }

            mDragState = REMOVING
            mRemoveVelocityX = velocityX

            if (mInTouchEvent) {
                when (mCancelMethod) {
                    ON_TOUCH_EVENT -> super.onTouchEvent(mCancelEvent)
                    ON_INTERCEPT_TOUCH_EVENT -> super.onInterceptTouchEvent(mCancelEvent)
                }
            }

            if (mRemoveAnimator != null) {
                mRemoveAnimator!!.start()
            } else {
                doRemoveItem(which)
            }
        }
    }

    /**
     * Move an item, bypassing the drag-sort process. Simply calls
     * through to [DropListener.drop].
     *
     * @param from Position to move (NOTE: headers/footers ignored!
     * this is a position in your input ListAdapter).
     * @param to   Target position (NOTE: headers/footers ignored!
     * this is a position in your input ListAdapter).
     */
    fun moveItem(from: Int, to: Int) {
        if (mDropListener != null) {
            val count = getInputAdapter()!!.count
            if (from in 0 until count && to in 0 until count) {
                mDropListener!!.drop(from, to)
            }
        }
    }

    /**
     * Cancel a drag. Calls with `true` as the first argument.
     */
    fun cancelDrag() {
        if (mDragState == DRAGGING) {
            mDragScroller.stopScrolling(true)
            destroyFloatView()
            clearPositions()
            adjustAllItems()

            mDragState = if (mInTouchEvent) {
                STOPPED
            } else {
                IDLE
            }
        }
    }

    private fun clearPositions() {
        mSrcPos = -1
        mFirstExpPos = -1
        mSecondExpPos = -1
        mFloatPos = -1
    }

    private fun dropFloatView() {
        // must set to avoid cancelDrag being called from the
        // DataSetObserver
        mDragState = DROPPING

        if (mDropListener != null && mFloatPos >= 0 && mFloatPos < count) {
            val numHeaders = headerViewsCount
            mDropListener!!.drop(mSrcPos - numHeaders, mFloatPos - numHeaders)
        }

        destroyFloatView()

        adjustOnReorder()
        clearPositions()
        adjustAllItems()

        // now the drag is done
        mDragState = if (mInTouchEvent) {
            STOPPED
        } else {
            IDLE
        }
    }

    private fun doRemoveItem() {
        doRemoveItem(mSrcPos - headerViewsCount)
    }

    /**
     * Removes dragged item from the list. Calls RemoveListener.
     */
    private fun doRemoveItem(which: Int) {
        // must set to avoid cancelDrag being called from the
        // DataSetObserver
        mDragState = REMOVING

        // end it
        mRemoveListener?.remove(which)

        destroyFloatView()

        adjustOnReorder()
        clearPositions()

        // now the drag is done
        mDragState = if (mInTouchEvent) {
            STOPPED
        } else {
            IDLE
        }
    }

    private fun adjustOnReorder() {
        val firstPos = firstVisiblePosition
        if (mSrcPos < firstPos) {
            // collapsed src item is off screen;
            // adjust the scroll after item heights have been fixed
            val v = getChildAt(0)
            var top = 0
            if (v != null) {
                top = v.top
            }
            setSelectionFromTop(firstPos - 1, top - paddingTop)
        }
    }

    /**
     * Stop a drag in progress. Pass `true` if you would
     * like to remove the dragged item from the list.
     *
     * @param remove Remove the dragged item from the list. Calls
     * a registered RemoveListener, if one exists. Otherwise, calls
     * the DropListener, if one exists.
     * @return True if the stop was successful. False if there is
     * no floating View.
     */
    fun stopDrag(remove: Boolean): Boolean {
        mUseRemoveVelocity = false
        return stopDrag(remove, 0f)
    }

    fun stopDragWithVelocity(remove: Boolean, velocityX: Float): Boolean {
        mUseRemoveVelocity = true
        return stopDrag(remove, velocityX)
    }

    fun stopDrag(remove: Boolean, velocityX: Float): Boolean {
        return if (mFloatView != null) {
            mDragScroller.stopScrolling(true)

            if (remove) {
                removeItem(mSrcPos - headerViewsCount, velocityX)
            } else {
                if (mDropAnimator != null) {
                    mDropAnimator!!.start()
                } else {
                    dropFloatView()
                }
            }

            if (mTrackDragSort) {
                mDragSortTracker!!.stopTracking()
            }

            true
        } else {
            // stop failed
            false
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (mIgnoreTouchEvent) {
            mIgnoreTouchEvent = false
            return false
        }

        if (!mDragEnabled) {
            return super.onTouchEvent(ev)
        }

        var more = false

        val lastCallWasIntercept = mLastCallWasIntercept
        mLastCallWasIntercept = false

        if (!lastCallWasIntercept) {
            saveTouchCoords(ev)
        }

        if (mDragState == DRAGGING) {
            onDragTouchEvent(ev)
            more = true // give us more!
        } else {
            // what if float view is null b/c we dropped in middle
            // of drag touch event?

            if (mDragState == IDLE) {
                if (super.onTouchEvent(ev)) {
                    more = true
                }
            }

            when (ev.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> doActionUpOrCancel()
                else -> if (more) {
                    mCancelMethod = ON_TOUCH_EVENT
                }
            }
        }

        return more
    }

    private fun doActionUpOrCancel() {
        mCancelMethod = NO_CANCEL
        mInTouchEvent = false
        if (mDragState == STOPPED) {
            mDragState = IDLE
        }
        mCurrFloatAlpha = mFloatAlpha
        mListViewIntercepted = false
        mChildHeightCache.clear()
    }

    private fun saveTouchCoords(ev: MotionEvent) {
        val action = ev.action and MotionEvent.ACTION_MASK
        if (action != MotionEvent.ACTION_DOWN) {
            mLastX = mX
            mLastY = mY
        }
        mX = ev.x.toInt()
        mY = ev.y.toInt()
        if (action == MotionEvent.ACTION_DOWN) {
            mLastX = mX
            mLastY = mY
        }
        mOffsetX = ev.rawX.toInt() - mX
        mOffsetY = ev.rawY.toInt() - mY
    }

    fun listViewIntercepted(): Boolean {
        return mListViewIntercepted
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!mDragEnabled) {
            return super.onInterceptTouchEvent(ev)
        }

        saveTouchCoords(ev)
        mLastCallWasIntercept = true

        val action = ev.action and MotionEvent.ACTION_MASK

        if (action == MotionEvent.ACTION_DOWN) {
            if (mDragState != IDLE) {
                // intercept and ignore
                mIgnoreTouchEvent = true
                return true
            }
            mInTouchEvent = true
        }

        var intercept = false

        // the following deals with calls to super.onInterceptTouchEvent
        if (mFloatView != null) {
            // super's touch event canceled in startDrag
            intercept = true
        } else {
            if (super.onInterceptTouchEvent(ev)) {
                mListViewIntercepted = true
                intercept = true
            }

            when (action) {
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> doActionUpOrCancel()
                else -> mCancelMethod = if (intercept) {
                    ON_TOUCH_EVENT
                } else {
                    ON_INTERCEPT_TOUCH_EVENT
                }
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mInTouchEvent = false
        }

        return intercept
    }

    /**
     * Set the width of each drag scroll region by specifying
     * a fraction of the ListView height.
     *
     * @param heightFraction Fraction of ListView height. Capped at 0.5f.
     */
    fun setDragScrollStart(heightFraction: Float) {
        setDragScrollStarts(heightFraction, heightFraction)
    }

    /**
     * Set the width of each drag scroll region by specifying
     * a fraction of the ListView height.
     *
     * @param upperFrac Fraction of ListView height for up-scroll bound. Capped at 0.5f.
     * @param lowerFrac Fraction of ListView height for down-scroll bound. Capped at 0.5f.
     */
    fun setDragScrollStarts(upperFrac: Float, lowerFrac: Float) {
        mDragDownScrollStartFrac = if (lowerFrac > 0.5f) {
            0.5f
        } else {
            lowerFrac
        }

        mDragUpScrollStartFrac = if (upperFrac > 0.5f) {
            0.5f
        } else {
            upperFrac
        }

        if (height != 0) {
            updateScrollStarts()
        }
    }

    private fun continueDrag(x: Int, y: Int) {
        // proposed position
        mFloatLoc.x = x - mDragDeltaX
        mFloatLoc.y = y - mDragDeltaY

        doDragFloatView()

        val minY = min(y, mFloatViewMid + mFloatViewHeightHalf)
        val maxY = max(y, mFloatViewMid - mFloatViewHeightHalf)

        // get the current scroll direction
        val currentScrollDir = mDragScroller.getScrollDir()

        if (minY > mLastY && minY > mDownScrollStartY && currentScrollDir != DOWN) {
            // dragged down, it is below the down scroll start and it is not
            // scrolling up

            if (currentScrollDir != STOP) {
                // moved directly from up scroll to down scroll
                mDragScroller.stopScrolling(true)
            }

            // start scrolling down
            mDragScroller.startScrolling(DOWN)
        } else if (maxY < mLastY && maxY < mUpScrollStartY && currentScrollDir != UP) {
            // dragged up, it is above the up scroll start and it is not
            // scrolling up

            if (currentScrollDir != STOP) {
                // moved directly from down scroll to up scroll
                mDragScroller.stopScrolling(true)
            }

            // start scrolling up
            mDragScroller.startScrolling(UP)
        } else if (maxY >= mUpScrollStartY && minY <= mDownScrollStartY &&
            mDragScroller.isScrolling()
        ) {
            // not in the upper nor in the lower drag-scroll regions but it is
            // still scrolling

            mDragScroller.stopScrolling(true)
        }
    }

    private fun updateScrollStarts() {
        val padTop = paddingTop
        val listHeight = height - padTop - paddingBottom
        val heightF = listHeight.toFloat()

        mUpScrollStartYF = padTop + mDragUpScrollStartFrac * heightF
        mDownScrollStartYF = padTop + (1.0f - mDragDownScrollStartFrac) * heightF

        mUpScrollStartY = mUpScrollStartYF.toInt()
        mDownScrollStartY = mDownScrollStartYF.toInt()

        mDragUpScrollHeight = mUpScrollStartYF - padTop
        mDragDownScrollHeight = padTop + listHeight - mDownScrollStartYF
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateScrollStarts()
    }

    private fun adjustAllItems() {
        val first = firstVisiblePosition
        val last = lastVisiblePosition

        val begin = max(0, headerViewsCount - first)
        val end = min(last - first, count - 1 - footerViewsCount - first)

        for (i in begin..end) {
            val v = getChildAt(i)
            if (v != null) {
                adjustItem(first + i, v, false)
            }
        }
    }

    private fun adjustItem(position: Int) {
        val v = getChildAt(position - firstVisiblePosition)

        if (v != null) {
            adjustItem(position, v, false)
        }
    }

    /**
     * Sets layout param height, gravity, and visibility  on
     * wrapped item.
     */
    private fun adjustItem(position: Int, v: View, invalidChildHeight: Boolean) {
        // Adjust item height
        val lp = v.layoutParams
        val height: Int
        if (position != mSrcPos && position != mFirstExpPos && position != mSecondExpPos) {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            height = calcItemHeight(position, v, invalidChildHeight)
        }

        if (height != lp.height) {
            lp.height = height
            v.layoutParams = lp
        }

        // Adjust item gravity
        if (position == mFirstExpPos || position == mSecondExpPos) {
            if (position < mSrcPos) {
                (v as DragSortItemView).setGravity(Gravity.BOTTOM)
            } else if (position > mSrcPos) {
                (v as DragSortItemView).setGravity(Gravity.TOP)
            }
        }

        // Finally adjust item visibility

        val oldVis = v.visibility
        var vis = VISIBLE

        if (position == mSrcPos && mFloatView != null) {
            vis = INVISIBLE
        }

        if (vis != oldVis) {
            v.visibility = vis
        }
    }

    private fun getChildHeight(position: Int): Int {
        if (position == mSrcPos) {
            return 0
        }

        var v = getChildAt(position - firstVisiblePosition)

        if (v != null) {
            // item is onscreen, therefore child height is valid,
            // hence the "true"
            return getChildHeight(position, v, false)
        } else {
            // item is offscreen
            // first check cache for child height at this position
            var childHeight = mChildHeightCache.get(position)
            if (childHeight != -1) {
                return childHeight
            }

            val adapter = adapter
            val type = adapter.getItemViewType(position)

            // There might be a better place for checking for the following
            val typeCount = adapter.viewTypeCount
            if (typeCount != mSampleViewTypes.size) {
                mSampleViewTypes = arrayOfNulls(typeCount)
            }

            if (type >= 0) {
                if (mSampleViewTypes[type] == null) {
                    v = adapter.getView(position, null, this)
                    mSampleViewTypes[type] = v
                } else {
                    v = adapter.getView(position, mSampleViewTypes[type], this)
                }
            } else {
                // type is HEADER_OR_FOOTER or IGNORE
                v = adapter.getView(position, null, this)
            }

            // current child height is invalid, hence "true" below
            childHeight = getChildHeight(position, v, true)

            // cache it because this could have been expensive
            mChildHeightCache.add(position, childHeight)

            return childHeight
        }
    }

    private fun getChildHeight(position: Int, item: View, invalidChildHeight: Boolean): Int {
        if (position == mSrcPos) {
            return 0
        }

        val child: View = if (position < headerViewsCount || position >= count - footerViewsCount) {
            item
        } else {
            (item as ViewGroup).getChildAt(0)
        }

        val lp = child.layoutParams

        if (lp != null) {
            if (lp.height > 0) {
                return lp.height
            }
        }

        var childHeight = child.height

        if (childHeight == 0 || invalidChildHeight) {
            measureItem(child)
            childHeight = child.measuredHeight
        }

        return childHeight
    }

    private fun calcItemHeight(position: Int, item: View, invalidChildHeight: Boolean): Int {
        return calcItemHeight(position, getChildHeight(position, item, invalidChildHeight))
    }

    private fun calcItemHeight(position: Int, childHeight: Int): Int {
        val isSliding = mAnimate && mFirstExpPos != mSecondExpPos
        val maxNonSrcBlankHeight = mFloatViewHeight - mItemHeightCollapsed
        val slideHeight = (mSlideFrac * maxNonSrcBlankHeight).toInt()

        val height: Int

        if (position == mSrcPos) {
            height = if (mSrcPos == mFirstExpPos) {
                if (isSliding) {
                    slideHeight + mItemHeightCollapsed
                } else {
                    mFloatViewHeight
                }
            } else if (mSrcPos == mSecondExpPos) {
                // if gets here, we know an item is sliding
                mFloatViewHeight - slideHeight
            } else {
                mItemHeightCollapsed
            }
        } else if (position == mFirstExpPos) {
            height = if (isSliding) {
                childHeight + slideHeight
            } else {
                childHeight + maxNonSrcBlankHeight
            }
        } else if (position == mSecondExpPos) {
            // we know an item is sliding (b/c 2ndPos != 1stPos)
            height = childHeight + maxNonSrcBlankHeight - slideHeight
        } else {
            height = childHeight
        }

        return height
    }

    override fun requestLayout() {
        if (!mBlockLayoutRequests) {
            super.requestLayout()
        }
    }

    private fun adjustScroll(
        movePos: Int,
        moveItem: View,
        oldFirstExpPos: Int,
        oldSecondExpPos: Int
    ): Int {
        var adjust = 0

        val childHeight = getChildHeight(movePos)

        val moveHeightBefore = moveItem.height
        val moveHeightAfter = calcItemHeight(movePos, childHeight)

        var moveBlankBefore = moveHeightBefore
        var moveBlankAfter = moveHeightAfter
        if (movePos != mSrcPos) {
            moveBlankBefore -= childHeight
            moveBlankAfter -= childHeight
        }

        var maxBlank = mFloatViewHeight
        if (mSrcPos != mFirstExpPos && mSrcPos != mSecondExpPos) {
            maxBlank -= mItemHeightCollapsed
        }

        if (movePos <= oldFirstExpPos) {
            if (movePos > mFirstExpPos) {
                adjust += maxBlank - moveBlankAfter
            }
        } else if (movePos == oldSecondExpPos) {
            if (movePos <= mFirstExpPos) {
                adjust += moveBlankBefore - maxBlank
            } else if (movePos == mSecondExpPos) {
                adjust += moveHeightBefore - moveHeightAfter
            } else {
                adjust += moveBlankBefore
            }
        } else {
            if (movePos <= mFirstExpPos) {
                adjust -= maxBlank
            } else if (movePos == mSecondExpPos) {
                adjust -= moveBlankAfter
            }
        }

        return adjust
    }

    private fun measureItem(item: View) {
        var lp = item.layoutParams
        if (lp == null) {
            lp = AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            item.layoutParams = lp
        }
        val wspec = ViewGroup.getChildMeasureSpec(
            mWidthMeasureSpec, listPaddingLeft + listPaddingRight, lp.width
        )
        val hspec: Int = if (lp.height > 0) {
            MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY)
        } else {
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        }
        item.measure(wspec, hspec)
    }

    private fun measureFloatView() {
        val floatView = mFloatView
        if (floatView != null) {
            measureItem(floatView)
            mFloatViewHeight = floatView.measuredHeight
            mFloatViewHeightHalf = mFloatViewHeight / 2
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mFloatView != null) {
            if (mFloatView!!.isLayoutRequested) {
                measureFloatView()
            }
            mFloatViewOnMeasured = true // set to false after layout
        }
        mWidthMeasureSpec = widthMeasureSpec
    }

    override fun layoutChildren() {
        super.layoutChildren()

        val floatView = mFloatView
        if (floatView != null) {
            if (floatView.isLayoutRequested && !mFloatViewOnMeasured) {
                // Have to measure here when usual android measure
                // pass is skipped. This happens during a drag-sort
                // when layoutChildren is called directly.
                measureFloatView()
            }
            floatView.layout(0, 0, floatView.measuredWidth, floatView.measuredHeight)
            mFloatViewOnMeasured = false
        }
    }

    private fun onDragTouchEvent(ev: MotionEvent): Boolean {
        // we are in a drag
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_CANCEL -> {
                if (mDragState == DRAGGING) {
                    cancelDrag()
                }
                doActionUpOrCancel()
            }

            MotionEvent.ACTION_UP -> {
                if (mDragState == DRAGGING) {
                    stopDrag(false)
                }
                doActionUpOrCancel()
            }

            MotionEvent.ACTION_MOVE -> continueDrag(ev.x.toInt(), ev.y.toInt())
        }

        return true
    }

    private fun invalidateFloatView() {
        mFloatViewInvalidated = true
    }

    /**
     * Start a drag of item at `position` using the
     * registered FloatViewManager. Calls through
     * to [startDrag] after obtaining
     * the floating View from the FloatViewManager.
     *
     * @param position  Item to drag.
     * @param dragFlags Flags that restrict some movements of the floating View.
     * @param deltaX    Offset in x of the touch coordinate from the left edge of the floating View.
     * @param deltaY    Offset in y of the touch coordinate from the top edge of the floating View.
     * @return True if the drag was started, false otherwise.
     */
    fun startDrag(position: Int, dragFlags: Int, deltaX: Int, deltaY: Int): Boolean {
        if (!mInTouchEvent || mFloatViewManager == null) {
            return false
        }

        val v = mFloatViewManager!!.onCreateFloatView(position)

        return if (v == null) {
            false
        } else {
            startDrag(position, v, dragFlags, deltaX, deltaY)
        }
    }

    /**
     * Start a drag of item at `position` without using
     * a FloatViewManager.
     *
     * @param position  Item to drag.
     * @param floatView Floating View.
     * @param dragFlags Flags that restrict some movements of the floating View.
     * @param deltaX    Offset in x of the touch coordinate from the left edge of the floating View.
     * @param deltaY    Offset in y of the touch coordinate from the top edge of the floating View.
     * @return True if the drag was started, false otherwise.
     */
    fun startDrag(
        position: Int,
        floatView: View?,
        dragFlags: Int,
        deltaX: Int,
        deltaY: Int
    ): Boolean {
        if (mDragState != IDLE || !mInTouchEvent || mFloatView != null || floatView == null ||
            !mDragEnabled
        ) {
            return false
        }

        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        val pos = position + headerViewsCount
        mFirstExpPos = pos
        mSecondExpPos = pos
        mSrcPos = pos
        mFloatPos = pos

        mDragState = DRAGGING
        mDragFlags = 0
        mDragFlags = mDragFlags or dragFlags

        mFloatView = floatView
        measureFloatView() // sets mFloatViewHeight

        mDragDeltaX = deltaX
        mDragDeltaY = deltaY
        mDragStartY = mY

        // updateFloatView(mX - mDragDeltaX, mY - mDragDeltaY);
        mFloatLoc.x = mX - mDragDeltaX
        mFloatLoc.y = mY - mDragDeltaY

        // set src item invisible
        val srcItem = getChildAt(mSrcPos - firstVisiblePosition)

        if (srcItem != null) {
            srcItem.visibility = INVISIBLE
        }

        if (mTrackDragSort) {
            mDragSortTracker!!.startTracking()
        }

        // once float view is created, events are no longer passed
        // to ListView
        when (mCancelMethod) {
            ON_TOUCH_EVENT -> super.onTouchEvent(mCancelEvent)
            ON_INTERCEPT_TOUCH_EVENT -> super.onInterceptTouchEvent(mCancelEvent)
        }

        requestLayout()

        mLiftAnimator?.start()

        return true
    }

    private fun doDragFloatView() {
        val movePos = firstVisiblePosition + childCount / 2
        val moveItem = getChildAt(childCount / 2) ?: return

        doDragFloatView(movePos, moveItem, true)
    }

    private fun doDragFloatView(movePos: Int, moveItem: View, forceInvalidate: Boolean) {
        mBlockLayoutRequests = true

        updateFloatView()

        val oldFirstExpPos = mFirstExpPos
        val oldSecondExpPos = mSecondExpPos

        val updated = updatePositions()

        if (updated) {
            adjustAllItems()
            val scroll = adjustScroll(movePos, moveItem, oldFirstExpPos, oldSecondExpPos)

            setSelectionFromTop(movePos, moveItem.top + scroll - paddingTop)
            layoutChildren()
        }

        if (updated || forceInvalidate) {
            invalidate()
        }

        mBlockLayoutRequests = false
    }

    /**
     * Sets float View location based on suggested values and
     * constraints set in mDragFlags.
     */
    private fun updateFloatView() {
        if (mFloatViewManager != null) {
            mTouchLoc.set(mX, mY)
            mFloatViewManager!!.onDragFloatView(mFloatView!!, mFloatLoc, mTouchLoc)
        }

        val floatX = mFloatLoc.x
        val floatY = mFloatLoc.y

        // restrict x motion
        val padLeft = paddingLeft
        if ((mDragFlags and DRAG_POS_X) == 0 && floatX > padLeft) {
            mFloatLoc.x = padLeft
        } else if ((mDragFlags and DRAG_NEG_X) == 0 && floatX < padLeft) {
            mFloatLoc.x = padLeft
        }

        // keep floating view from going past bottom of last header view
        val numHeaders = headerViewsCount
        val numFooters = footerViewsCount
        val firstPos = firstVisiblePosition
        val lastPos = lastVisiblePosition

        var topLimit = paddingTop
        if (firstPos < numHeaders) {
            topLimit = getChildAt(numHeaders - firstPos - 1).bottom
        }
        if ((mDragFlags and DRAG_NEG_Y) == 0) {
            if (firstPos <= mSrcPos) {
                topLimit = max(getChildAt(mSrcPos - firstPos).top, topLimit)
            }
        }
        // bottom limit is top of first footer View or
        // bottom of last item in list
        var bottomLimit = height - paddingBottom
        if (lastPos >= count - numFooters - 1) {
            bottomLimit = getChildAt(count - numFooters - 1 - firstPos).bottom
        }
        if ((mDragFlags and DRAG_POS_Y) == 0) {
            if (lastPos >= mSrcPos) {
                bottomLimit = min(getChildAt(mSrcPos - firstPos).bottom, bottomLimit)
            }
        }

        if (floatY < topLimit) {
            mFloatLoc.y = topLimit
        } else if (floatY + mFloatViewHeight > bottomLimit) {
            mFloatLoc.y = bottomLimit - mFloatViewHeight
        }

        // get y-midpoint of floating view (constrained to ListView bounds)
        mFloatViewMid = mFloatLoc.y + mFloatViewHeightHalf
    }

    private fun destroyFloatView() {
        val floatView = mFloatView
        if (floatView != null) {
            floatView.visibility = GONE
            mFloatViewManager?.onDestroyFloatView(floatView)
            mFloatView = null
            invalidate()
        }
    }

    fun setFloatViewManager(manager: FloatViewManager?) {
        mFloatViewManager = manager
    }

    fun setDragListener(l: DragListener?) {
        mDragListener = l
    }

    fun isDragEnabled(): Boolean {
        return mDragEnabled
    }

    /**
     * Allows for easy toggling between a DragSortListView
     * and a regular old ListView. If enabled, items are
     * draggable, where the drag init mode determines how
     * items are lifted. If disabled, items cannot be dragged.
     *
     * @param enabled Set `true` to enable list item dragging
     */
    fun setDragEnabled(enabled: Boolean) {
        mDragEnabled = enabled
    }

    /**
     * This better reorder your ListAdapter! DragSortListView does not do this
     * for you; doesn't make sense to. Make sure
     * [BaseAdapter.notifyDataSetChanged] or something like it is called
     * in your implementation.
     *
     * @param l
     */
    fun setDropListener(l: DropListener?) {
        mDropListener = l
    }

    /**
     * Probably a no-brainer, but make sure that your remove listener
     * calls [BaseAdapter.notifyDataSetChanged] or something like it.
     *
     * @param l
     */
    fun setRemoveListener(l: RemoveListener?) {
        mRemoveListener = l
    }

    fun setDragSortListener(l: DragSortListener) {
        setDropListener(l)
        setDragListener(l)
        setRemoveListener(l)
    }

    /**
     * Completely custom scroll speed profile. Default increases linearly
     * with position and is constant in time. Create your own by implementing
     * [DragSortListView.DragScrollProfile].
     *
     * @param ssp
     */
    fun setDragScrollProfile(ssp: DragScrollProfile?) {
        if (ssp != null) {
            mScrollProfile = ssp
        }
    }

    /**
     * Use this to move the check state of an item from one position to another
     * in a drop operation.
     *
     * @param from
     * @param to
     */
    fun moveCheckState(from: Int, to: Int) {
        val cip = checkedItemPositions
        var rangeStart = from
        var rangeEnd = to
        if (to < from) {
            rangeStart = to
            rangeEnd = from
        }
        rangeEnd += 1

        val runStart = IntArray(cip.size())
        val runEnd = IntArray(cip.size())
        val runCount = buildRunList(cip, rangeStart, rangeEnd, runStart, runEnd)
        if (runCount == 1 && (runStart[0] == runEnd[0])) {
            // Special case where all items are checked, we can never set any
            // item to false like we do below.
            return
        }

        if (from < to) {
            for (i in 0 until runCount) {
                setItemChecked(rotate(runStart[i], -1, rangeStart, rangeEnd), true)
                setItemChecked(rotate(runEnd[i], -1, rangeStart, rangeEnd), false)
            }
        } else {
            for (i in 0 until runCount) {
                setItemChecked(runStart[i], false)
                setItemChecked(runEnd[i], true)
            }
        }
    }

    /**
     * Use this when an item has been deleted, to move the check state of all
     * following items up one step.
     *
     * @param position
     */
    fun removeCheckState(position: Int) {
        val cip = checkedItemPositions

        if (cip.size() == 0) {
            return
        }
        val runStart = IntArray(cip.size())
        val runEnd = IntArray(cip.size())
        val rangeEnd = cip.keyAt(cip.size() - 1) + 1
        val runCount = buildRunList(cip, position, rangeEnd, runStart, runEnd)
        for (i in 0 until runCount) {
            if (!(runStart[i] == position || (runEnd[i] < runStart[i] && runEnd[i] > position))) {
                // Only set a new check mark in front of this run if it does
                // not contain the deleted position. If it does, we only need
                // to make it one check mark shorter at the end.
                setItemChecked(rotate(runStart[i], -1, position, rangeEnd), true)
            }
            setItemChecked(rotate(runEnd[i], -1, position, rangeEnd), false)
        }
    }

    /**
     * Interface for customization of the floating View appearance
     * and dragging behavior. Implement
     * your own and pass it to [setFloatViewManager]. If
     * your own is not passed, the default [SimpleFloatViewManager]
     * implementation is used.
     */
    interface FloatViewManager {
        /**
         * Return the floating View for item at `position`.
         */
        fun onCreateFloatView(position: Int): View?

        /**
         * Called whenever the floating View is dragged. Float View
         * properties can be changed here.
         */
        fun onDragFloatView(floatView: View, location: Point, touch: Point)

        /**
         * Called when the float View is dropped; lets you perform
         * any necessary cleanup.
         */
        fun onDestroyFloatView(floatView: View)
    }

    interface DragListener {
        fun drag(from: Int, to: Int)
    }

    /**
     * Your implementation of this has to reorder your ListAdapter!
     * Make sure to call
     * [BaseAdapter.notifyDataSetChanged] or something like it
     * in your implementation.
     *
     * @author heycosmo
     */
    fun interface DropListener {
        fun drop(from: Int, to: Int)
    }

    /**
     * Make sure to call
     * [BaseAdapter.notifyDataSetChanged] or something like it
     * in your implementation.
     *
     * @author heycosmo
     */
    interface RemoveListener {
        fun remove(which: Int)
    }

    interface DragSortListener : DropListener, DragListener, RemoveListener

    /**
     * Interface for controlling
     * scroll speed as a function of touch position and time.
     *
     * @author heycosmo
     */
    fun interface DragScrollProfile {
        /**
         * Return a scroll speed in pixels/millisecond. Always return a
         * positive number.
         */
        fun getSpeed(w: Float, t: Long): Float
    }

    private inner class AdapterWrapper(private val mAdapter: ListAdapter) : BaseAdapter() {

        init {
            mAdapter.registerDataSetObserver(object : DataSetObserver() {
                override fun onChanged() {
                    notifyDataSetChanged()
                }

                override fun onInvalidated() {
                    notifyDataSetInvalidated()
                }
            })
        }

        fun getAdapter(): ListAdapter {
            return mAdapter
        }

        override fun getItemId(position: Int): Long {
            return mAdapter.getItemId(position)
        }

        override fun getItem(position: Int): Any {
            return mAdapter.getItem(position)
        }

        override fun getCount(): Int {
            return mAdapter.count
        }

        override fun areAllItemsEnabled(): Boolean {
            return mAdapter.areAllItemsEnabled()
        }

        override fun isEnabled(position: Int): Boolean {
            return mAdapter.isEnabled(position)
        }

        override fun getItemViewType(position: Int): Int {
            return mAdapter.getItemViewType(position)
        }

        override fun getViewTypeCount(): Int {
            return mAdapter.viewTypeCount
        }

        override fun hasStableIds(): Boolean {
            return mAdapter.hasStableIds()
        }

        override fun isEmpty(): Boolean {
            return mAdapter.isEmpty
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v: DragSortItemView
            val child: View
            if (convertView != null) {
                v = convertView as DragSortItemView
                val oldChild = v.getChildAt(0)

                child = mAdapter.getView(position, oldChild, this@DragSortListView)
                if (child !== oldChild) {
                    // shouldn't get here if user is reusing convertViews
                    // properly
                    if (oldChild != null) {
                        v.removeViewAt(0)
                    }
                    v.addView(child)
                }
            } else {
                child = mAdapter.getView(position, null, this@DragSortListView)
                v = if (child is Checkable) {
                    DragSortItemViewCheckable(context)
                } else {
                    DragSortItemView(context)
                }
                v.layoutParams = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                v.addView(child)
            }

            // Set the correct item height given drag state; passed
            // View needs to be measured if measurement is required.
            adjustItem(position + headerViewsCount, v, true)

            return v
        }
    }

    private inner class HeightCache(size: Int) {

        private val mMap = SparseIntArray(size)
        private val mOrder = ArrayList<Int>(size)
        private val mMaxSize = size

        /**
         * Add item height at position if doesn't already exist.
         */
        fun add(position: Int, height: Int) {
            val currHeight = mMap.get(position, -1)
            if (currHeight != height) {
                if (currHeight == -1) {
                    if (mMap.size() == mMaxSize) {
                        // remove oldest entry
                        mMap.delete(mOrder.removeAt(0))
                    }
                } else {
                    // move position to newest slot
                    mOrder.remove(Integer.valueOf(position))
                }
                mMap.put(position, height)
                mOrder.add(position)
            }
        }

        fun get(position: Int): Int {
            return mMap.get(position, -1)
        }

        fun clear() {
            mMap.clear()
            mOrder.clear()
        }
    }

    private open inner class SmoothAnimator(smoothness: Float, duration: Int) : Runnable {
        @JvmField
        protected var mStartTime: Long = 0

        private val mDurationF: Float = duration.toFloat()

        private val mAlpha: Float = smoothness
        private val mA: Float
        private val mB: Float
        private val mC: Float
        private val mD: Float

        private var mCanceled = false

        init {
            mA = 1f / (2f * mAlpha * (1f - mAlpha))
            mD = mA
            mB = mAlpha / (2f * (mAlpha - 1f))
            mC = 1f / (1f - mAlpha)
        }

        fun transform(frac: Float): Float {
            return if (frac < mAlpha) {
                mA * frac * frac
            } else if (frac < 1f - mAlpha) {
                mB + mC * frac
            } else {
                1f - mD * (frac - 1f) * (frac - 1f)
            }
        }

        fun start() {
            mStartTime = SystemClock.uptimeMillis()
            mCanceled = false
            onStart()
            post(this)
        }

        fun cancel() {
            mCanceled = true
        }

        open fun onStart() {
            // stub
        }

        open fun onUpdate(frac: Float, smoothFrac: Float) {
            // stub
        }

        open fun onStop() {
            // stub
        }

        override fun run() {
            if (mCanceled) {
                return
            }

            val fraction = (SystemClock.uptimeMillis() - mStartTime).toFloat() / mDurationF

            if (fraction >= 1f) {
                onUpdate(1f, 1f)
                onStop()
            } else {
                onUpdate(fraction, transform(fraction))
                post(this)
            }
        }
    }

    /**
     * Centers floating View under touch point.
     */
    private inner class LiftAnimator(smoothness: Float, duration: Int) :
        SmoothAnimator(smoothness, duration) {

        private var mInitDragDeltaY = 0f
        private var mFinalDragDeltaY = 0f

        override fun onStart() {
            mInitDragDeltaY = mDragDeltaY.toFloat()
            mFinalDragDeltaY = mFloatViewHeightHalf.toFloat()
        }

        override fun onUpdate(frac: Float, smoothFrac: Float) {
            if (mDragState != DRAGGING) {
                cancel()
            } else {
                mDragDeltaY = (smoothFrac * mFinalDragDeltaY + (1f - smoothFrac) *
                    mInitDragDeltaY).toInt()
                mFloatLoc.y = mY - mDragDeltaY
                doDragFloatView()
            }
        }
    }

    /**
     * Centers floating View over drop slot before destroying.
     */
    private inner class DropAnimator(smoothness: Float, duration: Int) :
        SmoothAnimator(smoothness, duration) {

        private var mDropPos = 0
        private var srcPos = 0
        private var mInitDeltaY = 0f
        private var mInitDeltaX = 0f

        override fun onStart() {
            mDropPos = mFloatPos
            srcPos = mSrcPos
            mDragState = DROPPING
            mInitDeltaY = (mFloatLoc.y - getTargetY()).toFloat()
            mInitDeltaX = (mFloatLoc.x - paddingLeft).toFloat()
        }

        private fun getTargetY(): Int {
            val first = firstVisiblePosition
            val otherAdjust = (mItemHeightCollapsed + dividerHeight) / 2
            val v = getChildAt(mDropPos - first)
            var targetY = -1
            if (v != null) {
                targetY = if (mDropPos == srcPos) {
                    v.top
                } else if (mDropPos < srcPos) {
                    // expanded down
                    v.top - otherAdjust
                } else {
                    // expanded up
                    v.bottom + otherAdjust - mFloatViewHeight
                }
            } else {
                // drop position is not on screen?? no animation
                cancel()
            }

            return targetY
        }

        override fun onUpdate(frac: Float, smoothFrac: Float) {
            val targetY = getTargetY()
            val targetX = paddingLeft
            val deltaY = (mFloatLoc.y - targetY).toFloat()
            val deltaX = (mFloatLoc.x - targetX).toFloat()
            val f = 1f - smoothFrac
            if (f < abs(deltaY / mInitDeltaY) || f < abs(deltaX / mInitDeltaX)) {
                mFloatLoc.y = targetY + (mInitDeltaY * f).toInt()
                mFloatLoc.x = paddingLeft + (mInitDeltaX * f).toInt()
                doDragFloatView()
            }
        }

        override fun onStop() {
            dropFloatView()
        }
    }

    /**
     * Collapses expanded items.
     */
    private inner class RemoveAnimator(smoothness: Float, duration: Int) :
        SmoothAnimator(smoothness, duration) {

        private var mFloatLocX = 0f
        private var mFirstStartBlank = 0f
        private var mSecondStartBlank = 0f

        private var mFirstChildHeight = -1
        private var mSecondChildHeight = -1

        private var mFirstPos = 0
        private var mSecondPos = 0
        private var srcPos = 0

        override fun onStart() {
            mFirstChildHeight = -1
            mSecondChildHeight = -1
            mFirstPos = mFirstExpPos
            mSecondPos = mSecondExpPos
            srcPos = mSrcPos
            mDragState = REMOVING

            mFloatLocX = mFloatLoc.x.toFloat()
            if (mUseRemoveVelocity) {
                var minVelocity = 2f * width
                if (mRemoveVelocityX == 0f) {
                    mRemoveVelocityX = (if (mFloatLocX < 0) -1 else 1) * minVelocity
                } else {
                    minVelocity *= 2
                    if (mRemoveVelocityX < 0 && mRemoveVelocityX > -minVelocity) {
                        mRemoveVelocityX = -minVelocity
                    } else if (mRemoveVelocityX > 0 && mRemoveVelocityX < minVelocity) {
                        mRemoveVelocityX = minVelocity
                    }
                }
            } else {
                destroyFloatView()
            }
        }

        override fun onUpdate(frac: Float, smoothFrac: Float) {
            val f = 1f - smoothFrac

            val firstVis = firstVisiblePosition
            var item = getChildAt(mFirstPos - firstVis)
            var lp: ViewGroup.LayoutParams
            var blank: Int

            if (mUseRemoveVelocity) {
                val dt = (SystemClock.uptimeMillis() - mStartTime).toFloat() / 1000
                if (dt == 0f) {
                    return
                }
                val dx = mRemoveVelocityX * dt
                val w = width
                mRemoveVelocityX += (if (mRemoveVelocityX > 0) 1 else -1) * dt * w
                mFloatLocX += dx
                mFloatLoc.x = mFloatLocX.toInt()
                if (mFloatLocX < w && mFloatLocX > -w) {
                    mStartTime = SystemClock.uptimeMillis()
                    doDragFloatView()
                    return
                }
            }

            if (item != null) {
                if (mFirstChildHeight == -1) {
                    mFirstChildHeight = getChildHeight(mFirstPos, item, false)
                    mFirstStartBlank = (item.height - mFirstChildHeight).toFloat()
                }
                blank = max((f * mFirstStartBlank).toInt(), 1)
                lp = item.layoutParams
                lp.height = mFirstChildHeight + blank
                item.layoutParams = lp
            }
            if (mSecondPos != mFirstPos) {
                item = getChildAt(mSecondPos - firstVis)
                if (item != null) {
                    if (mSecondChildHeight == -1) {
                        mSecondChildHeight = getChildHeight(mSecondPos, item, false)
                        mSecondStartBlank = (item.height - mSecondChildHeight).toFloat()
                    }
                    blank = max((f * mSecondStartBlank).toInt(), 1)
                    lp = item.layoutParams
                    lp.height = mSecondChildHeight + blank
                    item.layoutParams = lp
                }
            }
        }

        override fun onStop() {
            doRemoveItem()
        }
    }

    private inner class DragScroller : Runnable {

        private var mAbort = false
        private var mPrevTime: Long = 0
        private var mCurrTime: Long = 0
        private var dy = 0
        private var dt = 0f
        private var tStart: Long = 0
        private var scrollDir = 0
        private var mScrollSpeed = 0f // pixels per ms

        private var mScrolling = false

        private var mLastHeader = 0
        private var mFirstFooter = 0

        fun isScrolling(): Boolean {
            return mScrolling
        }

        fun getScrollDir(): Int {
            return if (mScrolling) scrollDir else STOP
        }

        fun startScrolling(dir: Int) {
            if (!mScrolling) {
                mAbort = false
                mScrolling = true
                tStart = SystemClock.uptimeMillis()
                mPrevTime = tStart
                scrollDir = dir
                post(this)
            }
        }

        fun stopScrolling(now: Boolean) {
            if (now) {
                this@DragSortListView.removeCallbacks(this)
                mScrolling = false
            } else {
                mAbort = true
            }
        }

        override fun run() {
            if (mAbort) {
                mScrolling = false
                return
            }

            val first = firstVisiblePosition
            val last = lastVisiblePosition
            val count = count
            val padTop = paddingTop
            val listHeight = height - padTop - paddingBottom

            val minY = min(mY, mFloatViewMid + mFloatViewHeightHalf)
            val maxY = max(mY, mFloatViewMid - mFloatViewHeightHalf)

            if (scrollDir == UP) {
                val v = getChildAt(0)
                if (v == null) {
                    mScrolling = false
                    return
                } else {
                    if (first == 0 && v.top == padTop) {
                        mScrolling = false
                        return
                    }
                }
                mScrollSpeed = mScrollProfile.getSpeed(
                    (mUpScrollStartYF - maxY) / mDragUpScrollHeight, mPrevTime
                )
            } else {
                val v = getChildAt(last - first)
                if (v == null) {
                    mScrolling = false
                    return
                } else {
                    if (last == count - 1 && v.bottom <= listHeight + padTop) {
                        mScrolling = false
                        return
                    }
                }
                mScrollSpeed = -mScrollProfile.getSpeed(
                    (minY - mDownScrollStartYF) / mDragDownScrollHeight, mPrevTime
                )
            }

            mCurrTime = SystemClock.uptimeMillis()
            dt = (mCurrTime - mPrevTime).toFloat()

            // dy is change in View position of a list item; i.e. positive dy
            // means user is scrolling up (list item moves down the screen,
            // remember y=0 is at top of View).
            dy = Math.round(mScrollSpeed * dt)

            val movePos: Int
            if (dy >= 0) {
                dy = min(listHeight, dy)
                movePos = first
            } else {
                dy = max(-listHeight, dy)
                movePos = last
            }

            val moveItem = getChildAt(movePos - first)
            var top = moveItem.top + dy

            if (movePos == 0 && top > padTop) {
                top = padTop
            }

            // always do scroll
            mBlockLayoutRequests = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setSelectionFromTop(movePos, top - padTop)
            }
            this@DragSortListView.layoutChildren()
            invalidate()

            mBlockLayoutRequests = false

            // scroll means relative float View movement
            doDragFloatView(movePos, moveItem, false)

            mPrevTime = mCurrTime

            post(this)
        }
    }

    private inner class DragSortTracker {
        private val mBuilder = StringBuilder()

        private var mNumInBuffer = 0
        private var mTracking = false

        fun startTracking() {
            mBuilder.append("<DSLVStates>\n")
            mTracking = true
        }

        fun appendState() {
            if (!mTracking) {
                return
            }

            mBuilder.append("<DSLVState>\n")
            val children = childCount
            val first = firstVisiblePosition
            mBuilder.append("    <Positions>")
            for (i in 0 until children) {
                mBuilder.append(first + i).append(",")
            }
            mBuilder.append("</Positions>\n")

            mBuilder.append("    <Tops>")
            for (i in 0 until children) {
                mBuilder.append(getChildAt(i).top).append(",")
            }
            mBuilder.append("</Tops>\n")
            mBuilder.append("    <Bottoms>")
            for (i in 0 until children) {
                mBuilder.append(getChildAt(i).bottom).append(",")
            }
            mBuilder.append("</Bottoms>\n")

            mBuilder.append("    <FirstExpPos>").append(mFirstExpPos).append("</FirstExpPos>\n")
            mBuilder.append("    <FirstExpBlankHeight>")
                .append(getItemHeight(mFirstExpPos) - getChildHeight(mFirstExpPos))
                .append("</FirstExpBlankHeight>\n")
            mBuilder.append("    <SecondExpPos>").append(mSecondExpPos).append("</SecondExpPos>\n")
            mBuilder.append("    <SecondExpBlankHeight>")
                .append(getItemHeight(mSecondExpPos) - getChildHeight(mSecondExpPos))
                .append("</SecondExpBlankHeight>\n")
            mBuilder.append("    <SrcPos>").append(mSrcPos).append("</SrcPos>\n")
            mBuilder.append("    <SrcHeight>").append(mFloatViewHeight + dividerHeight)
                .append("</SrcHeight>\n")
            mBuilder.append("    <ViewHeight>").append(height).append("</ViewHeight>\n")
            mBuilder.append("    <LastY>").append(mLastY).append("</LastY>\n")
            mBuilder.append("    <FloatY>").append(mFloatViewMid).append("</FloatY>\n")
            mBuilder.append("    <ShuffleEdges>")
            for (i in 0 until children) {
                mBuilder.append(getShuffleEdge(first + i, getChildAt(i).top)).append(",")
            }
            mBuilder.append("</ShuffleEdges>\n")

            mBuilder.append("</DSLVState>\n")
            mNumInBuffer++

            if (mNumInBuffer > 1000) {
                flush()
                mNumInBuffer = 0
            }
        }

        fun flush() {
            if (!mTracking) {
                return
            }
            // debug-only trace; file writing removed.
            mBuilder.delete(0, mBuilder.length)
        }

        fun stopTracking() {
            if (mTracking) {
                mBuilder.append("</DSLVStates>\n")
                flush()
                mTracking = false
            }
        }
    }

    companion object {
        /**
         * Drag flag bit. Floating View can move in the positive x direction.
         */
        const val DRAG_POS_X = 0x1

        /**
         * Drag flag bit. Floating View can move in the negative x direction.
         */
        const val DRAG_NEG_X = 0x2

        /**
         * Drag flag bit. Floating View can move in the positive y direction.
         */
        const val DRAG_POS_Y = 0x4

        /**
         * Drag flag bit. Floating View can move in the negative y direction.
         */
        const val DRAG_NEG_Y = 0x8

        /**
         * Drag state enum.
         */
        private const val IDLE = 0
        private const val REMOVING = 1
        private const val DROPPING = 2
        private const val STOPPED = 3
        private const val DRAGGING = 4

        // DragScroller scroll directions (DragScroller is an inner class and cannot hold a companion).
        private const val STOP = -1
        private const val UP = 0
        private const val DOWN = 1

        /**
         * Enum telling where to cancel the ListView action when a
         * drag-sort begins
         */
        private const val NO_CANCEL = 0
        private const val ON_TOUCH_EVENT = 1
        private const val ON_INTERCEPT_TOUCH_EVENT = 2

        /**
         * Caches DragSortItemView child heights.
         */
        private const val sCacheSize = 3

        private fun buildRunList(
            cip: SparseBooleanArray, rangeStart: Int,
            rangeEnd: Int, runStart: IntArray, runEnd: IntArray
        ): Int {
            var runCount = 0

            var i = findFirstSetIndex(cip, rangeStart, rangeEnd)
            if (i == -1) {
                return 0
            }

            var position = cip.keyAt(i)
            var currentRunStart = position
            var currentRunEnd = currentRunStart + 1
            i++
            while (i < cip.size() && cip.keyAt(i).also { position = it } < rangeEnd) {
                if (!cip.valueAt(i)) { // not checked => not interesting
                    i++
                    continue
                }
                if (position == currentRunEnd) {
                    currentRunEnd++
                } else {
                    runStart[runCount] = currentRunStart
                    runEnd[runCount] = currentRunEnd
                    runCount++
                    currentRunStart = position
                    currentRunEnd = position + 1
                }
                i++
            }

            if (currentRunEnd == rangeEnd) {
                // rangeStart and rangeEnd are equivalent positions so to be
                // consistent we translate them to the same integer value.
                currentRunEnd = rangeStart
            }
            runStart[runCount] = currentRunStart
            runEnd[runCount] = currentRunEnd
            runCount++

            if (runCount > 1) {
                if (runStart[0] == rangeStart && runEnd[runCount - 1] == rangeStart) {
                    // The last run ends at the end of the range, and the first run
                    // starts at the beginning of the range. So they are actually
                    // part of the same run, except they wrap around the end of the
                    // range. To avoid adjacent runs, we need to merge them.
                    runStart[0] = runStart[runCount - 1]
                    runCount--
                }
            }
            return runCount
        }

        private fun rotate(value: Int, offset: Int, lowerBound: Int, upperBound: Int): Int {
            val windowSize = upperBound - lowerBound

            var v = value + offset
            if (v < lowerBound) {
                v += windowSize
            } else if (v >= upperBound) {
                v -= windowSize
            }
            return v
        }

        private fun findFirstSetIndex(
            sba: SparseBooleanArray,
            rangeStart: Int,
            rangeEnd: Int
        ): Int {
            val size = sba.size()
            var i = insertionIndexForKey(sba, rangeStart)
            while (i < size && sba.keyAt(i) < rangeEnd && !sba.valueAt(i)) {
                i++
            }
            if (i == size || sba.keyAt(i) >= rangeEnd) {
                return -1
            }
            return i
        }

        private fun insertionIndexForKey(sba: SparseBooleanArray, key: Int): Int {
            var low = 0
            var high = sba.size()
            while (high - low > 0) {
                val middle = (low + high) shr 1
                if (sba.keyAt(middle) < key) {
                    low = middle + 1
                } else {
                    high = middle
                }
            }
            return low
        }
    }
}
