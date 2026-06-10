package com.opentouchgaming.androidcore.ui.widgets

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.R

class SDLMidiPlayerWidget(internal var settingPrefix: String)
{
    internal var player: Int = 0

    fun linkUI(activity: Activity, dialog: Dialog)
    {
        loadSettings()

        val paths: Array<String> = arrayOf("Timidity", "Fluidsynth")

        val spinner = dialog.findViewById<Spinner>(R.id.sdl_midi_player_spinner)
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, paths)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(player)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long)
            {
                player = position
                saveSettings()
            }

            override fun onNothingSelected(parent: AdapterView<*>?)
            {
            }
        }
    }

    private fun loadSettings()
    {
        player = AppSettings.getIntOption(AppInfo.getContext(), settingPrefix + "sdl_midi_player", 0) // default 48000
    }

    private fun saveSettings()
    {
        AppSettings.setIntOption(AppInfo.getContext(), settingPrefix + "sdl_midi_player", player)
    }

    fun getPlayer(): Int
    {
        loadSettings()
        return player
    }
}
