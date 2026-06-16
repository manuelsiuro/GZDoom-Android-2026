package com.opentouchgaming.androidcore.ui.widgets

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.R

class AudioOverrideWidget internal constructor(internal var settingPrefix: String)
{
    private val freqList = arrayOf(48000, 44100, 22050, 11025)
    private val samplesList = arrayOf(128, 256, 512, 1024, 1536, 2048, 2560, 3072, 3584, 4096, 5120, 6144, 7168, 8192)

    internal var override: Boolean = false
    internal var backend: Int = -1
    internal var freq: Int = 0
    internal var samples: Int = 0

    fun linkUI(activity: Activity, dialog: Dialog)
    {
        loadSettings()

        val overrideCheckbox = dialog.findViewById<Switch>(R.id.override_checkBox)
        val overrideLayout = dialog.findViewById<LinearLayout>(R.id.override_layout)

        if (override) overrideLayout.visibility = View.VISIBLE else overrideLayout.visibility = View.GONE

        // Android O allows AAudio
        val paths: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            arrayOf("Default", "OpenSL", "Audio Tack (Old)", "AAudio (low latency)")
        }
        else
        {
            arrayOf("Default", "OpenSL", "Audio Tack (Old)")
        }

        val spinner = dialog.findViewById<Spinner>(R.id.audio_spinner)
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, paths)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(backend)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long)
            {
                backend = position
                saveSettings()
            }

            override fun onNothingSelected(parent: AdapterView<*>?)
            {
            }
        }

        val freqSpinner = dialog.findViewById<Spinner>(R.id.freq_spinner)
        val adapterFreq = ArrayAdapter(activity, android.R.layout.simple_spinner_item, freqList)
        freqSpinner.adapter = adapterFreq
        freqSpinner.setSelection(freq)
        freqSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
            {
                freq = position
                saveSettings()
            }

            override fun onNothingSelected(parent: AdapterView<*>?)
            {

            }
        }

        val samplesSpinner = dialog.findViewById<Spinner>(R.id.samples_spinner)
        val adapterSamples = ArrayAdapter(activity, android.R.layout.simple_spinner_item, samplesList)
        samplesSpinner.adapter = adapterSamples
        samplesSpinner.setSelection(samples)

        samplesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                samples = position
                saveSettings()
            }

            override fun onNothingSelected(parent: AdapterView<*>?)
            {
            }
        }

        overrideCheckbox.isChecked = override

        overrideCheckbox.setOnCheckedChangeListener { _, isChecked ->
            override = isChecked
            if (override) overrideLayout.visibility = View.VISIBLE else overrideLayout.visibility = View.GONE
            saveSettings()
        }
    }

    private fun loadSettings()
    {
        override = AppSettings.getBoolOption(AppInfo.getContext(), settingPrefix + "audio_override", false)
        freq = AppSettings.getIntOption(AppInfo.getContext(), settingPrefix + "audio_freq", 0) // default 48000
        samples = AppSettings.getIntOption(AppInfo.getContext(), settingPrefix + "audio_samples_v2", 5) // default 2048
        backend = AppSettings.getIntOption(AppInfo.getContext(), settingPrefix + "audio_backend", 0)
    }

    private fun saveSettings()
    {
        AppSettings.setBoolOption(AppInfo.getContext(), settingPrefix + "audio_override", override)
        AppSettings.setIntOption(AppInfo.getContext(), settingPrefix + "audio_freq", freq)
        AppSettings.setIntOption(AppInfo.getContext(), settingPrefix + "audio_samples_v2", samples)
        AppSettings.setIntOption(AppInfo.getContext(), settingPrefix + "audio_backend", backend)
    }

    fun getFreq(): Int
    {
        loadSettings()
        return if (override) freqList[freq]
        else 0
    }

    fun getSamples(): Int
    {
        loadSettings()
        return if (override) samplesList[samples]
        else 0
    }

    fun getBackend(): Int
    {
        return backend - 1 // Shift down so default returns -1
    }
}
