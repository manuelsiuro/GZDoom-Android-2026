package com.mobeta.android.dslv

import android.graphics.Point
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.AdapterView
import kotlin.math.abs

/**
 * Class that starts and stops item drags on a [DragSortListView]
 * based on touch gestures. This class also inherits from
 * [SimpleFloatViewManager], which provides basic float View
 * creation.
 *
 * An instance of this class is meant to be passed to the methods
 * and
 * of your
 * [DragSortListView] instance.
 */
open class DragSortController @JvmOverloads constructor(
    private val mDslv: DragSortListView,
    private var mDragHandleId: Int = 0,
    dragInitMode: Int = ON_DOWN,
    removeMode: Int = FLING_REMOVE,
    private var mClickRemoveId: Int = 0,
    private var mFlingHandleId: Int = 0
) : SimpleFloatViewManager(mDslv), View.OnTouchListener, GestureDetector.OnGestureListener {

    private var mDragInitMode = ON_DOWN
    private var mSortEnabled = true

    /**
     * The current remove mode.
     */
    private var mRemoveMode = 0
    private var mRemoveEnabled = false
    private var mIsRemoving = false
    private val mDetector: GestureDetector
    private val mFlingRemoveDetector: GestureDetector
    private val mTouchSlop: Int
    private var mHitPos = MISS
    private var mFlingHitPos = MISS

    private var mClickRemoveHitPos = MISS

    private val mTempLoc = IntArray(2)

    private var mItemX = 0
    private var mItemY = 0

    private var mCurrX = 0
    private var mCurrY = 0

    private var mDragging = false

    private var mCanDrag = false

    private var mPositionX = 0

    init {
        mDetector = GestureDetector(mDslv.context, this)
        // Log.d("mobeta", "on fling remove called");
        val mFlingRemoveListener: GestureDetector.OnGestureListener =
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent, velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    // Log.d("mobeta", "on fling remove called");
                    if (mRemoveEnabled && mIsRemoving) {
                        val w = mDslv.width
                        val minPos = w / 5
                        val mFlingSpeed = 500f
                        if (velocityX > mFlingSpeed) {
                            if (mPositionX > -minPos) {
                                mDslv.stopDragWithVelocity(true, velocityX)
                            }
                        } else if (velocityX < -mFlingSpeed) {
                            if (mPositionX < minPos) {
                                mDslv.stopDragWithVelocity(true, velocityX)
                            }
                        }
                        mIsRemoving = false
                    }
                    return false
                }
            }
        mFlingRemoveDetector = GestureDetector(mDslv.context, mFlingRemoveListener)
        mFlingRemoveDetector.setIsLongpressEnabled(false)
        mTouchSlop = ViewConfiguration.get(mDslv.context).scaledTouchSlop
        setRemoveMode(removeMode)
        setDragInitMode(dragInitMode)
    }

    fun getDragInitMode(): Int {
        return mDragInitMode
    }

    /**
     * Set how a drag is initiated. Needs to be one of
     * [ON_DOWN], [ON_DRAG], or [ON_LONG_PRESS].
     *
     * @param mode The drag init mode.
     */
    fun setDragInitMode(mode: Int) {
        mDragInitMode = mode
    }

    fun isSortEnabled(): Boolean {
        return mSortEnabled
    }

    /**
     * Enable/Disable list item sorting. Disabling is useful if only item
     * removal is desired. Prevents drags in the vertical direction.
     *
     * @param enabled Set `true` to enable list
     * item sorting.
     */
    fun setSortEnabled(enabled: Boolean) {
        mSortEnabled = enabled
    }

    fun getRemoveMode(): Int {
        return mRemoveMode
    }

    /**
     * One of [CLICK_REMOVE] or [FLING_REMOVE].
     */
    fun setRemoveMode(mode: Int) {
        mRemoveMode = mode
    }

    fun isRemoveEnabled(): Boolean {
        return mRemoveEnabled
    }

    /**
     * Enable/Disable item removal without affecting remove mode.
     */
    fun setRemoveEnabled(enabled: Boolean) {
        mRemoveEnabled = enabled
    }

    /**
     * Set the resource id for the View that represents the drag
     * handle in a list item.
     *
     * @param id An android resource id.
     */
    fun setDragHandleId(id: Int) {
        mDragHandleId = id
    }

    /**
     * Set the resource id for the View that represents the fling
     * handle in a list item.
     *
     * @param id An android resource id.
     */
    fun setFlingHandleId(id: Int) {
        mFlingHandleId = id
    }

    /**
     * Set the resource id for the View that represents click
     * removal button.
     *
     * @param id An android resource id.
     */
    fun setClickRemoveId(id: Int) {
        mClickRemoveId = id
    }

    /**
     * Sets flags to restrict certain motions of the floating View
     * based on DragSortController settings (such as remove mode).
     * Starts the drag on the DragSortListView.
     *
     * @param position The list item position (includes headers).
     * @param deltaX   Touch x-coord minus left edge of floating View.
     * @param deltaY   Touch y-coord minus top edge of floating View.
     * @return True if drag started, false otherwise.
     */
    fun startDrag(position: Int, deltaX: Int, deltaY: Int): Boolean {
        var dragFlags = 0
        if (mSortEnabled && !mIsRemoving) {
            dragFlags = dragFlags or DragSortListView.DRAG_POS_Y or DragSortListView.DRAG_NEG_Y
        }
        if (mRemoveEnabled && mIsRemoving) {
            dragFlags = dragFlags or DragSortListView.DRAG_POS_X
            dragFlags = dragFlags or DragSortListView.DRAG_NEG_X
        }

        mDragging = mDslv.startDrag(
            position - mDslv.headerViewsCount, dragFlags, deltaX,
            deltaY
        )
        return mDragging
    }

    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        if (!mDslv.isDragEnabled() || mDslv.listViewIntercepted()) {
            return false
        }

        mDetector.onTouchEvent(ev)
        if (mRemoveEnabled && mDragging && mRemoveMode == FLING_REMOVE) {
            mFlingRemoveDetector.onTouchEvent(ev)
        }

        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mCurrX = ev.x.toInt()
                mCurrY = ev.y.toInt()
            }

            MotionEvent.ACTION_UP -> {
                if (mRemoveEnabled && mIsRemoving) {
                    val x = if (mPositionX >= 0) mPositionX else -mPositionX
                    val removePoint = mDslv.width / 2
                    if (x > removePoint) {
                        mDslv.stopDragWithVelocity(true, 0f)
                    }
                }
                mIsRemoving = false
                mDragging = false
            }

            MotionEvent.ACTION_CANCEL -> {
                mIsRemoving = false
                mDragging = false
            }
        }

        return false
    }

    /**
     * Overrides to provide fading when slide removal is enabled.
     */
    override fun onDragFloatView(floatView: View, position: Point, touch: Point) {
        if (mRemoveEnabled && mIsRemoving) {
            mPositionX = position.x
        }
    }

    /**
     * Get the position to start dragging based on the ACTION_DOWN
     * MotionEvent. This function simply calls
     * [dragHandleHitPosition]. Override
     * to change drag handle behavior;
     * this function is called internally when an ACTION_DOWN
     * event is detected.
     *
     * @param ev The ACTION_DOWN MotionEvent.
     * @return The list position to drag if a drag-init gesture is
     * detected; MISS if unsuccessful.
     */
    open fun startDragPosition(ev: MotionEvent): Int {
        return dragHandleHitPosition(ev)
    }

    fun startFlingPosition(ev: MotionEvent): Int {
        return if (mRemoveMode == FLING_REMOVE) flingHandleHitPosition(ev) else MISS
    }

    /**
     * Checks for the touch of an item's drag handle (specified by
     * [setDragHandleId]), and returns that item's position
     * if a drag handle touch was detected.
     *
     * @param ev The ACTION_DOWN MotionEvent.
     * @return The list position of the item whose drag handle was
     * touched; MISS if unsuccessful.
     */
    fun dragHandleHitPosition(ev: MotionEvent): Int {
        return viewIdHitPosition(ev, mDragHandleId)
    }

    fun flingHandleHitPosition(ev: MotionEvent): Int {
        return viewIdHitPosition(ev, mFlingHandleId)
    }

    fun viewIdHitPosition(ev: MotionEvent, id: Int): Int {
        val x = ev.x.toInt()
        val y = ev.y.toInt()

        val touchPos = mDslv.pointToPosition(x, y) // includes headers/footers

        val numHeaders = mDslv.headerViewsCount
        val numFooters = mDslv.footerViewsCount
        val count = mDslv.count

        // Log.d("mobeta", "touch down on position " + itemnum);
        // We're only interested if the touch was on an
        // item that's not a header or footer.
        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders &&
            touchPos < count - numFooters
        ) {
            val item = mDslv.getChildAt(touchPos - mDslv.firstVisiblePosition)
            val rawX = ev.rawX.toInt()
            val rawY = ev.rawY.toInt()

            val dragBox = if (id == 0) item else item.findViewById<View>(id)
            if (dragBox != null) {
                dragBox.getLocationOnScreen(mTempLoc)

                if (rawX > mTempLoc[0] && rawY > mTempLoc[1] &&
                    rawX < mTempLoc[0] + dragBox.width &&
                    rawY < mTempLoc[1] + dragBox.height
                ) {
                    mItemX = item.left
                    mItemY = item.top

                    return touchPos
                }
            }
        }

        return MISS
    }

    override fun onDown(ev: MotionEvent): Boolean {
        if (mRemoveEnabled && mRemoveMode == CLICK_REMOVE) {
            mClickRemoveHitPos = viewIdHitPosition(ev, mClickRemoveId)
        }

        mHitPos = startDragPosition(ev)
        if (mHitPos != MISS && mDragInitMode == ON_DOWN) {
            startDrag(mHitPos, ev.x.toInt() - mItemX, ev.y.toInt() - mItemY)
        }

        mIsRemoving = false
        mCanDrag = true
        mPositionX = 0
        mFlingHitPos = startFlingPosition(ev)

        return true
    }

    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
    ): Boolean {
        val x1 = e1!!.x.toInt()
        val y1 = e1.y.toInt()
        val x2 = e2.x.toInt()
        val y2 = e2.y.toInt()
        val deltaX = x2 - mItemX
        val deltaY = y2 - mItemY

        if (mCanDrag && !mDragging && (mHitPos != MISS || mFlingHitPos != MISS)) {
            if (mHitPos != MISS) {
                if (mDragInitMode == ON_DRAG && abs(y2 - y1) > mTouchSlop && mSortEnabled) {
                    startDrag(mHitPos, deltaX, deltaY)
                } else if (mDragInitMode != ON_DOWN && abs(x2 - x1) > mTouchSlop && mRemoveEnabled) {
                    mIsRemoving = true
                    startDrag(mFlingHitPos, deltaX, deltaY)
                }
            } else {
                if (abs(x2 - x1) > mTouchSlop && mRemoveEnabled) {
                    mIsRemoving = true
                    startDrag(mFlingHitPos, deltaX, deltaY)
                } else if (abs(y2 - y1) > mTouchSlop) {
                    mCanDrag = false // if started to scroll the list then
                    // don't allow sorting nor fling-removing
                }
            }
        }
        // return whatever
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        // Log.d("mobeta", "lift listener long pressed");
        if (mHitPos != MISS && mDragInitMode == ON_LONG_PRESS) {
            mDslv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            startDrag(mHitPos, mCurrX - mItemX, mCurrY - mItemY)
        }
    }

    // complete the OnGestureListener interface
    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
    ): Boolean {
        return false
    }

    // complete the OnGestureListener interface
    override fun onSingleTapUp(ev: MotionEvent): Boolean {
        if (mRemoveEnabled && mRemoveMode == CLICK_REMOVE) {
            if (mClickRemoveHitPos != MISS) {
                mDslv.removeItem(mClickRemoveHitPos - mDslv.headerViewsCount)
            }
        }
        return true
    }

    // complete the OnGestureListener interface
    override fun onShowPress(ev: MotionEvent) {
        // do nothing
    }

    companion object {
        /**
         * Drag init mode enum.
         */
        const val ON_DOWN = 0
        const val ON_DRAG = 1
        const val ON_LONG_PRESS = 2

        /**
         * Remove mode enum.
         */
        const val CLICK_REMOVE = 0
        const val FLING_REMOVE = 1
        const val MISS = -1
    }
}
