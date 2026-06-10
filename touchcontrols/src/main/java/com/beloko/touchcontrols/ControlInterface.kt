package com.beloko.touchcontrols

interface ControlInterface {

    fun initTouchControls_if(pngPath: String?, width: Int, height: Int)

    fun touchEvent_if(action: Int, pid: Int, x: Float, y: Float): Boolean

    fun keyPress_if(down: Int, qkey: Int, unicode: Int)

    fun doAction_if(state: Int, action: Int)

    fun analogFwd_if(v: Float)

    fun analogSide_if(v: Float)

    fun analogPitch_if(mode: Int, v: Float)

    fun analogYaw_if(mode: Int, v: Float)

    fun setTouchSettings_if(alpha: Float, strafe: Float, fwd: Float, pitch: Float, yaw: Float, other: Int)

    fun quickCommand_if(command: String)

    fun mapKey(acode: Int, unicode: Int): Int
}
