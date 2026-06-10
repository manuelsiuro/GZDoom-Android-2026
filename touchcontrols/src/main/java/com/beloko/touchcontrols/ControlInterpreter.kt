package com.beloko.touchcontrols

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import com.bda.controller.Controller
import com.beloko.touchcontrols.ControlConfig.Type
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

class ControlInterpreter(
    private val quakeIf: ControlInterface,
    gamepadActions: ArrayList<ActionInput>?,
    controlfile: String?,
    ctrlEn: Boolean,
) {
    private val LOG = "ControlInterpreter"

    private val config: ControlConfig
    private val gamePadEnabled: Boolean

    private var screenWidth = 0f
    private var screenHeight = 0f

    // Saves current state of analog buttons so all sent each time
    private val analogButtonState = HashMap<Int, Boolean>()
    private val deadRegion = 0.2f
    private val genericAxisValues = GenericAxisValues()

    init {
        if (TouchSettings.DEBUG) Log.d(LOG, "file = $controlfile")

        gamePadEnabled = ctrlEn

        config = ControlConfig(controlfile, gamepadActions)
        try {
            config.loadControls()
        } catch (e: IOException) {
            // ignored
        } catch (e: ClassNotFoundException) {
            Log.e(LOG, "Error loading gamepad file: $e")
        }

        for (ai in config.actions) {
            if (ai.sourceType == Type.ANALOG && (ai.actionType == Type.MENU || ai.actionType == Type.BUTTON)) {
                analogButtonState[ai.actionCode] = false
            }
        }
    }

    fun setScreenSize(w: Int, h: Int) {
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val actionCode = action and MotionEvent.ACTION_MASK

        if (actionCode == MotionEvent.ACTION_MOVE) {
            for (i in 0 until event.pointerCount) {
                val x = event.getX(i) / screenWidth
                val y = event.getY(i) / screenHeight
                val pid = event.getPointerId(i)
                quakeIf.touchEvent_if(3, pid, x, y)
            }
        } else if (actionCode == MotionEvent.ACTION_DOWN) {
            val x = event.x / screenWidth
            val y = event.y / screenHeight
            quakeIf.touchEvent_if(1, 0, x, y)
        } else if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
            val index = event.actionIndex
            if (index != -1) {
                val x = event.getX(index) / screenWidth
                val y = event.getY(index) / screenHeight
                val pid = event.getPointerId(index)
                quakeIf.touchEvent_if(1, pid, x, y)
            }
        } else if (actionCode == MotionEvent.ACTION_POINTER_UP) {
            val index = event.actionIndex
            if (index != -1) {
                val x = event.getX(index) / screenWidth
                val y = event.getY(index) / screenHeight
                val pid = event.getPointerId(index)
                quakeIf.touchEvent_if(2, pid, x, y)
            }
        } else if (actionCode == MotionEvent.ACTION_UP) {
            val x = event.x / screenWidth
            val y = event.y / screenHeight
            val index = event.actionIndex
            val pid = event.getPointerId(index)
            quakeIf.touchEvent_if(2, pid, x, y)
        }

        return true
    }

    fun onMogaKeyEvent(event: com.bda.controller.KeyEvent, pad_version: Int) {
        val keycode = event.keyCode

        if (pad_version == Controller.ACTION_VERSION_MOGA) {
            if (keycode == com.bda.controller.KeyEvent.KEYCODE_DPAD_DOWN ||
                keycode == com.bda.controller.KeyEvent.KEYCODE_DPAD_UP ||
                keycode == com.bda.controller.KeyEvent.KEYCODE_DPAD_LEFT ||
                keycode == com.bda.controller.KeyEvent.KEYCODE_DPAD_RIGHT
            ) {
                return
            }
        }

        if (event.action == com.bda.controller.KeyEvent.ACTION_DOWN) {
            onKeyDown(keycode, null)
        } else if (event.action == com.bda.controller.KeyEvent.ACTION_UP) {
            onKeyUp(keycode, null)
        }
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        var used = false
        if (gamePadEnabled) {
            for (ai in config.actions) {
                if ((ai.sourceType == Type.BUTTON || ai.sourceType == Type.MENU) && ai.source == keyCode) {
                    quakeIf.doAction_if(1, ai.actionCode)
                    Log.d(LOG, "key down intercept")
                    used = true
                }
            }
        }

        if (used) return true

        return if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            false
        } else {
            val uc = event?.unicodeChar ?: 0
            quakeIf.keyPress_if(1, quakeIf.mapKey(keyCode, uc), uc)
            true
        }
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        var used = false
        if (gamePadEnabled) {
            for (ai in config.actions) {
                if ((ai.sourceType == Type.BUTTON || ai.sourceType == Type.MENU) && ai.source == keyCode) {
                    quakeIf.doAction_if(0, ai.actionCode)
                    used = true
                }
            }
        }

        if (used) return true

        return if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            false
        } else {
            val uc = event?.unicodeChar ?: 0
            quakeIf.keyPress_if(0, quakeIf.mapKey(keyCode, uc), uc)
            true
        }
    }

    private fun analogCalibrate(v: Float): Float {
        return if (v < deadRegion && v > -deadRegion) {
            0f
        } else {
            if (v > 0) {
                (v - deadRegion) / (1 - deadRegion)
            } else {
                (v + deadRegion) / (1 - deadRegion)
            }
        }
    }

    // This is for normal Android motion event
    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        genericAxisValues.setAndroidValues(event)
        return onGenericMotionEvent(genericAxisValues)
    }

    // This is for Moga event
    fun onGenericMotionEvent(event: com.bda.controller.MotionEvent): Boolean {
        genericAxisValues.setMogaValues(event)
        return onGenericMotionEvent(genericAxisValues)
    }

    fun onGenericMotionEvent(event: GenericAxisValues): Boolean {
        if (TouchSettings.DEBUG) Log.d(LOG, "onGenericMotionEvent")

        var used = false
        if (gamePadEnabled) {
            for (ai in config.actions) {
                if (ai.sourceType == Type.ANALOG && ai.source != -1) {
                    val invert = if (ai.invert) -1 else 1
                    when (ai.actionCode) {
                        ControlConfig.ACTION_ANALOG_PITCH ->
                            quakeIf.analogPitch_if(
                                ControlConfig.LOOK_MODE_JOYSTICK,
                                analogCalibrate(event.getAxisValue(ai.source)) * invert * ai.scale,
                            )
                        ControlConfig.ACTION_ANALOG_YAW ->
                            quakeIf.analogYaw_if(
                                ControlConfig.LOOK_MODE_JOYSTICK,
                                -analogCalibrate(event.getAxisValue(ai.source)) * invert * ai.scale,
                            )
                        ControlConfig.ACTION_ANALOG_FWD ->
                            quakeIf.analogFwd_if(
                                -analogCalibrate(event.getAxisValue(ai.source)) * invert * ai.scale,
                            )
                        ControlConfig.ACTION_ANALOG_STRAFE ->
                            quakeIf.analogSide_if(
                                analogCalibrate(event.getAxisValue(ai.source)) * invert * ai.scale,
                            )
                        else -> {
                            // Must be using analog as a button
                            if (TouchSettings.DEBUG) Log.d(LOG, "Analog as button")
                            if (TouchSettings.DEBUG) Log.d(LOG, ai.toString())

                            if ((ai.sourcePositive && event.getAxisValue(ai.source) > 0.5) ||
                                (!ai.sourcePositive && event.getAxisValue(ai.source) < -0.5)
                            ) {
                                if (!analogButtonState[ai.actionCode]!!) { // only send if different
                                    quakeIf.doAction_if(1, ai.actionCode) // press
                                    analogButtonState[ai.actionCode] = true
                                }
                            } else {
                                if (analogButtonState[ai.actionCode]!!) { // only send if different
                                    quakeIf.doAction_if(0, ai.actionCode) // un-press
                                    analogButtonState[ai.actionCode] = false
                                }
                            }
                        }
                    }
                    used = true
                }
            }
        }

        return used
    }
}
