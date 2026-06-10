package com.beloko.touchcontrols

import android.view.MotionEvent

class GenericAxisValues {
    private val values = FloatArray(64)

    fun getAxisValue(a: Int): Float = values[a]

    fun setAxisValue(a: Int, v: Float) {
        values[a] = v
    }

    fun setAndroidValues(event: MotionEvent) {
        for (n in 0 until 64) {
            values[n] = event.getAxisValue(n)
        }
    }

    fun setMogaValues(event: com.bda.controller.MotionEvent) {
        values[MotionEvent.AXIS_X] = event.getAxisValue(MotionEvent.AXIS_X)
        values[MotionEvent.AXIS_Y] = event.getAxisValue(MotionEvent.AXIS_Y)
        values[MotionEvent.AXIS_Z] = event.getAxisValue(MotionEvent.AXIS_Z)
        values[MotionEvent.AXIS_RZ] = event.getAxisValue(MotionEvent.AXIS_RZ)
    }
}
