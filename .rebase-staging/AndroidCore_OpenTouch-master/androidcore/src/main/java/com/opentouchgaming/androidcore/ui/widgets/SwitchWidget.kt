package com.opentouchgaming.androidcore.ui.widgets

import android.content.Context
import android.view.View
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.databinding.WidgetViewSwitchBinding

class SwitchWidget(
    val context: Context, view: View, title: String, description: String, private val settingPrefix: String, val default: Boolean, image: Int = 0
)
{
    private var binding = WidgetViewSwitchBinding.bind(view)

    init
    {
        binding.title.text = title
        binding.description.text = description

        // Update image if set
        if (image != 0) binding.imageView.setImageResource(image)

        // Clear any change listeners which may be present
        binding.switch1.setOnCheckedChangeListener(null)

        // Set current value
        binding.switch1.isChecked = fetchValue(context, settingPrefix, default)

        // Add new change listener
        binding.switch1.setOnCheckedChangeListener { _, isChecked: Boolean ->
            AppSettings.setBoolOption(context, settingPrefix, isChecked)
        }

        // Allow whole control to be clickable
        binding.topLayout.setOnClickListener {
            binding.switch1.isChecked = !binding.switch1.isChecked
        }
    }

    fun setEnabled(enabled: Boolean)
    {
        binding.switch1.isEnabled = enabled
        binding.title.isEnabled = enabled
        binding.imageView.isEnabled = enabled
    }

    companion object
    {
        //This annotation tells Java classes to treat this method as if it was a static to [KotlinClass]
        @JvmStatic
        fun fetchValue(context: Context, settingPrefix: String, default: Boolean): Boolean
        {
            return AppSettings.getBoolOption(context, settingPrefix, default)
        }
    }
}