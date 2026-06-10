package com.beloko.touchcontrols

import com.beloko.touchcontrols.ControlConfig.Type
import java.io.Serializable

class ActionInput @JvmOverloads constructor(
    @JvmField var tag: String?,
    @JvmField var description: String?,
    @JvmField var actionCode: Int,
    @JvmField var actionType: Type?,
    @JvmField var sourceType: Type? = null,
    @JvmField var source: Int = -1,
    @JvmField var sourcePositive: Boolean = true, // Used when using analog as a button
) : Serializable {

    @JvmField
    var invert: Boolean = false

    @JvmField
    var scale: Float = 1f // sensitivity for analog

    override fun toString(): String =
        "$description:$sourceType source: $source sourcePositive: $sourcePositive"

    companion object {
        private const val serialVersionUID = 1L
    }
}
