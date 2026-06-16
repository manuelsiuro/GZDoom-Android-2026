package com.opentouchgaming.androidcore.ui.widgets

import android.R
import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.databinding.WidgetViewGamepadConfigBinding
import com.opentouchgaming.saffal.FileSAF

/**
 * A settings widget that lists available `.padconfig` files from the gamepad directory
 * and allows the user to select one as an engine-specific override.
 *
 * The first entry is always "Default (global)" which means no override — the global
 * [AppSettings] key `gamepad_config_filename` will be used instead.
 *
 * Use [fetchValue] to retrieve the selected filename (e.g. "myconfig.padconfig"), or
 * null when the default is selected.
 */
class GamepadConfigWidget(
    val context: Context,
    view: View,
    private val settingKey: String
)
{
    private val binding = WidgetViewGamepadConfigBinding.bind(view)

    private val DEFAULT_LABEL = "Default (global)"

    /** Ordered list matching spinner positions: index 0 = null (default), rest = filenames. */
    private val fileNames = ArrayList<String?>()

    init
    {
        binding.title.text = "Gamepad configuration"
        binding.description.text = "Select a gamepad config for this engine,\nAdd and remove configs from the Gamepad setup screen."

        val labels = ArrayList<String>()
        fileNames.add(null) // position 0 = default
        labels.add(DEFAULT_LABEL)

        val gamepadDir = FileSAF(AppInfo.getGamepadDirectory())
        val listed = gamepadDir.list() ?: emptyArray()
        listed.sorted().filter { it != "DEFAULT" }.forEach { filename ->
            fileNames.add(filename)
            labels.add(filename)
        }

        val adapter = ArrayAdapter(context, R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                val value = fileNames[position] ?: ""
                AppSettings.setStringOption(context, settingKey + SUFFIX, value)
            }
        }

        // Restore saved selection
        val saved = AppSettings.getStringOption(context, settingKey + SUFFIX, "")
        val idx = if (saved.isEmpty()) 0 else fileNames.indexOf(saved).coerceAtLeast(0)
        binding.spinner.setSelection(idx)
    }

    companion object
    {
        private const val SUFFIX = "_gamepad_config"

        /**
         * Returns the selected `.padconfig` filename, or null if "Default (global)" is selected.
         */
        @JvmStatic
        fun fetchValue(context: Context, settingKey: String): String?
        {
            val value = AppSettings.getStringOption(context, settingKey + SUFFIX, "")
            return if (value.isEmpty()) null else value
        }
    }
}
