package com.opentouchgaming.androidcore.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.R
import com.opentouchgaming.androidcore.databinding.DialogMainOptionsNewBinding
import com.opentouchgaming.androidcore.ui.widgets.SpinnerWidget
import com.opentouchgaming.androidcore.ui.widgets.SwitchWidget

class OptionsDialogKt(
    activity: Activity, private val extraOptions: View?, private val update: Runnable
)
{
    var binding: DialogMainOptionsNewBinding = DialogMainOptionsNewBinding.inflate(activity.layoutInflater)

    companion object
    {
        const val SYSTEM_RESOLUTION_OVERRIDE = "system_resolution_override"
        const val HIDE_NAV_BAR = "hide_nav_bar"
        const val EXPAND_INTO_NOTCH = "expand_into_notch"
        const val LAUNCHER_HIDE_NAV_BAR = "launcher_hide_nav_bar"
        const val LAUNCHER_EXPAND_INTO_NOTCH = "launcher_expand_into_notch"
        const val HIDE_TOUCH_GFX = "hide_touch_graphics"
        const val USE_SYSTEM_KEYBOARD = "use_system_keyboard"
        const val USE_ALT_TOUCH_CODE = "use_alt_touch_code"
        const val TOUCH_JOY_MULTITOUCH = "touch_joy_multitouch"
        const val GROUP_SIMILAR_ENGINES = "group_similar_engines"
        const val SDL_AUDIO_BACKEND = "sdl_audio_backend"
        const val OPENAL_AUDIO_BACKEND = "openal_audio_backend"
        const val ENABLE_VIBRATE = "enable_vibrate"
        const val ENABLE_GAMEPAD = "gamepad_enabled"
        const val GAMEPAD_HIDE_TOUCH = "gamepad_hide_touch"
        const val USE_MINI_UI = "use_mini_ui"
        const val SWAP_MOUSE_XY = "swap_mouse_xy"
        const val INVERT_MOUSE_X = "invert_mouse_x"
        const val INVERT_MOUSE_Y = "invert_mouse_y"

        val resolutions = arrayListOf(
            Pair("100%", 1.0f), Pair("75%", 0.75f), Pair("60%", 0.6f), Pair("50%", 0.5f), Pair("30%", 0.30f), Pair("25%", 0.25f)
        )

        fun GetResolutionScale(ctx: Context): Float
        {
            var idx = SpinnerWidget.fetchValue(ctx, SYSTEM_RESOLUTION_OVERRIDE, 0)
            return if (idx < resolutions.size) resolutions[idx].second
            else 1.0f
        }
    }

    private var dontUpdate = false

    init
    {
        val dialog = Dialog(activity, R.style.DialogEngineSettings)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            if (!dontUpdate) update.run()
        }
        val items: Array<Pair<String, View?>> = resolutions.map { Pair(it.first, null) }.toTypedArray()
        // Create and Setup the spinner options
        SpinnerWidget(
            activity,
            binding.resolutionOverride.root,
            "Display resolution",
            "Reduce the screen resolution for all engines.\nIt is recommended to use engine specific options instead of this.",
            items,
            SYSTEM_RESOLUTION_OVERRIDE,
            0,
            R.drawable.setting_resolution
        )

        SwitchWidget(
            activity,
            binding.hideNavButtons.root,
            "Full screen",
            "Go full screen by hiding the system navigation buttons",
            HIDE_NAV_BAR,
            false,
            R.drawable.settings_hide_nav
        )

        SwitchWidget(
            activity,
            binding.expandToNotch.root,
            "Expand in to camera notch",
            "Expand the screen in to the camera notch",
            EXPAND_INTO_NOTCH,
            false,
            R.drawable.setting_expand_notch
        )

        SwitchWidget(
            activity,
            binding.launcherHideNavButtons.root,
            "Full screen",
            "Go full screen by hiding the system navigation buttons",
            LAUNCHER_HIDE_NAV_BAR,
            false,
            R.drawable.settings_hide_nav
        )

        SwitchWidget(
            activity,
            binding.launcherExpandToNotch.root,
            "Expand in to camera notch",
            "Expand the screen in to the camera notch",
            LAUNCHER_EXPAND_INTO_NOTCH,
            false,
            R.drawable.setting_expand_notch
        )

        SwitchWidget(
            activity,
            binding.hideTouchGraphics.root,
            "Hide on-screen controls",
            "Hide all the touch screen graphics on menu and in-game",
            HIDE_TOUCH_GFX,
            false,
            R.drawable.setting_hide_touch_graphics
        )

        SwitchWidget(
            activity,
            binding.useAndroidKeyboard.root,
            "Use system keyboard",
            "Disable built-in keyboard. This option may not work with some keyboards",
            USE_SYSTEM_KEYBOARD,
            false
        )

        SwitchWidget(
            activity, binding.altTouchCode.root, "Use alternative touch code", "May fix touch screen control issues", USE_ALT_TOUCH_CODE, false
        )

        SwitchWidget(
            activity, binding.touchJoyMultitouch.root, "Touch pad multi-touch", "Enable to allow multiple touches on look and move pads", TOUCH_JOY_MULTITOUCH, false
        )

        // Swap Mouse X and Y with expandable sub-options
        SwitchWidget(
            activity, binding.swapMouseXy.root, "Swap Mouse X and Y", "Swap the mouse X and Y axes", SWAP_MOUSE_XY, false
        )

        SwitchWidget(
            activity, binding.invertMouseX.root, "Invert Mouse X", "Invert the mouse X axis", INVERT_MOUSE_X, false
        )

        SwitchWidget(
            activity, binding.invertMouseY.root, "Invert Mouse Y", "Invert the mouse Y axis", INVERT_MOUSE_Y, false
        )

        // Show/hide sub-options based on swap mouse state
        val updateSwapMouseSubOptions = {
            binding.swapMouseXySubOptions.visibility =
                if (SwitchWidget.fetchValue(activity, SWAP_MOUSE_XY, false)) View.VISIBLE else View.GONE
        }
        updateSwapMouseSubOptions()

        val swapBinding = com.opentouchgaming.androidcore.databinding.WidgetViewSwitchBinding.bind(binding.swapMouseXy.root)
        swapBinding.switch1.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setBoolOption(activity, SWAP_MOUSE_XY, isChecked)
            updateSwapMouseSubOptions()
        }

        SwitchWidget(
            activity,
            binding.groupSimilarEngines.root,
            "Group similar engines",
            "Group engines on the left panel, makes icons larger",
            GROUP_SIMILAR_ENGINES,
            AppInfo.groupSimilarEngines
        )

        SwitchWidget(
            activity, binding.enableVibrate.root, "Enable vibrate for keyboard", "Enable vibration on each keyboard key press", ENABLE_VIBRATE, true
        )

        val sdlAudioItems: Array<Pair<String, View?>>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            sdlAudioItems = arrayOf(
                Pair("OpenSL (Default)", null), Pair("Audio Tack (Old)", null), Pair("AAudio (low latency)", null)
            )
        } else
        {
            sdlAudioItems = arrayOf(
                Pair("OpenSL (Default)", null), Pair("Audio Tack (Old)", null)
            )
        }

        SpinnerWidget(
            activity,
            binding.sdlAudioBackend.root,
            "SDL audio backend",
            "Audio backend SDL uses to play audio",
            sdlAudioItems,
            SDL_AUDIO_BACKEND,
            0,
            R.drawable.setting_audio
        )

        val openalAudioItems: Array<Pair<String, View?>> = arrayOf(
            Pair("OpenSL", null), Pair("OBOE", null)
        )

        SpinnerWidget(
            activity,
            binding.openalAudioBackend.root,
            "OpenAL audio backend",
            "Audio backend OpenAL uses to play audio",
            openalAudioItems,
            OPENAL_AUDIO_BACKEND,
            0, // Default OpenSL
            R.drawable.setting_audio
        )

        SwitchWidget(
            activity,
            binding.enableGamepad.root,
            "Enable gamepad",
            "Enable gamepad input events. Disable if randomly moving.",
            ENABLE_GAMEPAD,
            true,
            R.drawable.baseline_videogame_asset_24
        )

        SwitchWidget(
            activity,
            binding.gamepadHideTouch.root,
            "Gamepad auto hide touch controls",
            "When you press a gamepad button the touch controls will automatically hide",
            GAMEPAD_HIDE_TOUCH,
            true,
            R.drawable.setting_hide_touch_graphics
        )

        SwitchWidget(
            activity,
            binding.useMiniUi.root,
            "Mini UI list",
            "Use smaller user interface for game list (restart needed)",
            USE_MINI_UI,
            false,
        )

        binding.resetButton.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(activity)
            dialogBuilder.setTitle("WARNING: This will reset all app settings! (Game data and save games are not touched)")
            dialogBuilder.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
                dontUpdate = true
                AppSettings.deleteAllOptions(activity)
                AppInfo.currentEngine = null
                dialog.dismiss()
                System.exit(0) // Kill the process so everything is reloaded
            }
            dialogBuilder.create().show()
        }

        if (extraOptions != null)
        {
            val layout = dialog.findViewById<LinearLayout>(R.id.extras_linearlayout)
            binding.extrasLinearlayout.addView(extraOptions)
        }


        dialog.show()
    }


}
