@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.beloko.touchcontrols

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager

object ShowKeyboard {
    @JvmField
    var activity: Activity? = null

    @JvmField
    var view: View? = null

    @JvmStatic
    fun setup(a: Activity, v: View?) {
        activity = a
        view = v
    }

    @JvmStatic
    fun toggleKeyboard() {
        Log.d("ShowKeyboard", "toggleKeyboard")

        val im = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (im != null) {
            Log.d("ShowKeyboard", "toggleKeyboard...")
            im.toggleSoftInput(0, 0)
        }
    }

    @JvmStatic
    fun showKeyboard(show: Int) {
        Log.d("ShowKeyboard", "showKeyboard $show")

        val im = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (im != null) {
            if (show == 0) {
                im.hideSoftInputFromWindow(activity!!.currentFocus!!.windowToken, 0)
            }
            if (show == 1) {
                if (!im.isAcceptingText) toggleKeyboard()
            }
            if (show == 2) {
                toggleKeyboard()
            }
        }
    }

    @JvmStatic
    fun hasHardwareKeyboard(): Boolean {
        if (activity == null) return false

        return activity!!.applicationContext.resources.configuration.keyboard == Configuration.KEYBOARD_QWERTY
    }
}
