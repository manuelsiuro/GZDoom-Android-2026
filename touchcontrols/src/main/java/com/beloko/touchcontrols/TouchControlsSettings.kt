package com.beloko.touchcontrols

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.Spinner

object TouchControlsSettings {

    @JvmField
    var activity: Activity? = null

    @JvmField
    var quakeIf: ControlInterface? = null

    @JvmField var alpha = 0
    @JvmField var fwdSens = 0
    @JvmField var strafeSens = 0
    @JvmField var pitchSens = 0
    @JvmField var yawSens = 0

    @JvmField var mouseMode = false
    @JvmField var showSticks = false
    @JvmField var enableWeaponWheel = false
    @JvmField var invertLook = false
    @JvmField var precisionShoot = false

    @JvmField var doubleTapMove = 0
    @JvmField var doubleTapLook = 0

    @JvmStatic
    fun setup(a: Activity, qif: ControlInterface) {
        activity = a
        quakeIf = qif
    }

    @JvmStatic
    fun showSettings() {
        Log.d("settings", "showSettings")

        activity!!.runOnUiThread {
            val dialog = Dialog(activity!!)
            dialog.setContentView(R.layout.touch_controls_settings)
            dialog.setTitle("Touch Control Sensitivity Settings")
            dialog.setCancelable(true)

            val alphaSeek = dialog.findViewById<SeekBar>(R.id.alpha_seekbar)
            val fwdSeek = dialog.findViewById<SeekBar>(R.id.fwd_seekbar)
            val strafeSeek = dialog.findViewById<SeekBar>(R.id.strafe_seekbar)
            val pitchSeek = dialog.findViewById<SeekBar>(R.id.pitch_seekbar)
            val yawSeek = dialog.findViewById<SeekBar>(R.id.yaw_seekbar)

            val mouseModeCheck = dialog.findViewById<CheckBox>(R.id.mouse_turn_checkbox)
            val invertLookCheckBox = dialog.findViewById<CheckBox>(R.id.invert_loop_checkbox)
            val precisionShootCheckBox = dialog.findViewById<CheckBox>(R.id.precision_shoot_checkbox)
            val showSticksCheckBox = dialog.findViewById<CheckBox>(R.id.show_sticks_checkbox)
            val enableWeaponWheelCheckBox = dialog.findViewById<CheckBox>(R.id.enable_weapon_wheel_checkbox)

            val addRemButton = dialog.findViewById<Button>(R.id.add_remove_button)
            addRemButton.setOnClickListener { TouchControlsEditing.show(activity) }

            alphaSeek.progress = alpha
            fwdSeek.progress = fwdSens
            strafeSeek.progress = strafeSens
            pitchSeek.progress = pitchSens
            yawSeek.progress = yawSens

            mouseModeCheck.isChecked = mouseMode
            invertLookCheckBox.isChecked = invertLook
            precisionShootCheckBox.isChecked = precisionShoot
            showSticksCheckBox.isChecked = showSticks
            enableWeaponWheelCheckBox.isChecked = enableWeaponWheel

            val moveSpinner = dialog.findViewById<Spinner>(R.id.move_dbl_tap_spinner)
            val adapterm = ArrayAdapter.createFromResource(
                activity!!,
                R.array.double_tap_actions,
                android.R.layout.simple_spinner_item,
            )
            adapterm.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            moveSpinner.adapter = adapterm
            moveSpinner.setSelection(doubleTapMove)

            moveSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    doubleTapMove = pos
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }

            val lookSpinner = dialog.findViewById<Spinner>(R.id.look_dbl_tap_spinner)
            val adapterl = ArrayAdapter.createFromResource(
                activity!!,
                R.array.double_tap_actions,
                android.R.layout.simple_spinner_item,
            )
            adapterl.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            lookSpinner.adapter = adapterl
            lookSpinner.setSelection(doubleTapLook)

            lookSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    doubleTapLook = pos
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }

            dialog.setOnDismissListener {
                alpha = alphaSeek.progress
                fwdSens = fwdSeek.progress
                strafeSens = strafeSeek.progress
                pitchSens = pitchSeek.progress
                yawSens = yawSeek.progress

                mouseMode = mouseModeCheck.isChecked
                invertLook = invertLookCheckBox.isChecked
                precisionShoot = precisionShootCheckBox.isChecked
                showSticks = showSticksCheckBox.isChecked
                enableWeaponWheel = enableWeaponWheelCheckBox.isChecked

                saveSettings(activity!!)
                sendToQuake()
            }

            val save = dialog.findViewById<Button>(R.id.save_button)
            save.setOnClickListener {
                alpha = alphaSeek.progress
                fwdSens = fwdSeek.progress
                strafeSens = strafeSeek.progress
                pitchSens = pitchSeek.progress
                yawSens = yawSeek.progress

                mouseMode = mouseModeCheck.isChecked
                invertLook = invertLookCheckBox.isChecked
                precisionShoot = precisionShootCheckBox.isChecked
                showSticks = showSticksCheckBox.isChecked
                enableWeaponWheel = enableWeaponWheelCheckBox.isChecked

                saveSettings(activity!!)
                sendToQuake()
                dialog.dismiss()
            }

            val cancel = dialog.findViewById<Button>(R.id.cancel_button)
            cancel.setOnClickListener { dialog.dismiss() }

            dialog.show()
        }
    }

    @JvmStatic
    fun sendToQuake() {
        var other = 0
        other += if (mouseMode) 0x2 else 0
        other += if (invertLook) 0x4 else 0
        other += if (precisionShoot) 0x8 else 0

        other += (doubleTapMove shl 4) and 0xF0
        other += (doubleTapLook shl 8) and 0xF00

        other += if (showSticks) 0x1000 else 0
        other += if (enableWeaponWheel) 0x2000 else 0

        other += if (TouchSettings.hideTouchControls) 0x80000000.toInt() else 0

        quakeIf!!.setTouchSettings_if(
            alpha.toFloat() / 100f,
            strafeSens / 50f,
            fwdSens / 50f,
            pitchSens / 50f,
            yawSens / 50f,
            other,
        )
    }

    @JvmStatic
    fun loadSettings(ctx: Context) {
        alpha = TouchSettings.getIntOption(ctx, "alpha", 50)
        fwdSens = TouchSettings.getIntOption(ctx, "fwdSens", 50)
        strafeSens = TouchSettings.getIntOption(ctx, "strafeSens", 50)
        pitchSens = TouchSettings.getIntOption(ctx, "pitchSens", 50)
        yawSens = TouchSettings.getIntOption(ctx, "yawSens", 50)

        mouseMode = TouchSettings.getBoolOption(ctx, "mouse_mode", true)
        invertLook = TouchSettings.getBoolOption(ctx, "invert_look", false)
        precisionShoot = TouchSettings.getBoolOption(ctx, "precision_shoot", false)
        showSticks = TouchSettings.getBoolOption(ctx, "show_sticks", false)
        enableWeaponWheel = TouchSettings.getBoolOption(ctx, "enable_ww", true)

        doubleTapMove = TouchSettings.getIntOption(ctx, "double_tap_move", 0)
        doubleTapLook = TouchSettings.getIntOption(ctx, "double_tap_look", 0)
    }

    @JvmStatic
    fun saveSettings(ctx: Context) {
        TouchSettings.setIntOption(ctx, "alpha", alpha)
        TouchSettings.setIntOption(ctx, "fwdSens", fwdSens)
        TouchSettings.setIntOption(ctx, "strafeSens", strafeSens)
        TouchSettings.setIntOption(ctx, "pitchSens", pitchSens)
        TouchSettings.setIntOption(ctx, "yawSens", yawSens)

        TouchSettings.setBoolOption(ctx, "invert_look", invertLook)
        TouchSettings.setBoolOption(ctx, "precision_shoot", precisionShoot)
        TouchSettings.setBoolOption(ctx, "show_sticks", showSticks)
        TouchSettings.setBoolOption(ctx, "enable_ww", enableWeaponWheel)

        TouchSettings.setIntOption(ctx, "double_tap_move", doubleTapMove)
        TouchSettings.setIntOption(ctx, "double_tap_look", doubleTapLook)
    }
}
