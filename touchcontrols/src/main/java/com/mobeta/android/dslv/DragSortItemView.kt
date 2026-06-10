@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.mobeta.android.dslv

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.AbsListView

/**
 * Lightweight ViewGroup that wraps list items obtained from user's
 * ListAdapter. ItemView expects a single child that has a definite
 * height (i.e. the child's layout height is not MATCH_PARENT).
 * The width of
 * ItemView will always match the width of its child (that is,
 * the width MeasureSpec given to ItemView is passed directly
 * to the child, and the ItemView measured width is set to the
 * child's measured width). The height of ItemView can be anything;
 * the
 *
 *
 * The purpose of this class is to optimize slide
 * shuffle animations.
 */
open class DragSortItemView(context: Context) : ViewGroup(context) {

    private var mGravity = Gravity.TOP

    init {
        // always init with standard ListView layout params
        layoutParams = AbsListView.LayoutParams(
            LayoutParams.FILL_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        //setClipChildren(true);
    }

    fun getGravity(): Int {
        return mGravity
    }

    fun setGravity(gravity: Int) {
        mGravity = gravity
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child = getChildAt(0) ?: return

        if (mGravity == Gravity.TOP) {
            child.layout(0, 0, measuredWidth, child.measuredHeight)
        } else {
            child.layout(0, measuredHeight - child.measuredHeight, measuredWidth, measuredHeight)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var height = MeasureSpec.getSize(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val child = getChildAt(0)
        if (child == null) {
            setMeasuredDimension(0, width)
            return
        }

        if (child.isLayoutRequested) {
            // Always let child be as tall as it wants.
            measureChild(
                child, widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
        }

        if (heightMode == MeasureSpec.UNSPECIFIED) {
            val lp = layoutParams

            height = if (lp.height > 0) {
                lp.height
            } else {
                child.measuredHeight
            }
        }

        setMeasuredDimension(width, height)
    }
}
