package com.beloko.touchcontrols

import java.io.Serializable

class QuickCommand(
    var title: String?,
    var command: String?,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
