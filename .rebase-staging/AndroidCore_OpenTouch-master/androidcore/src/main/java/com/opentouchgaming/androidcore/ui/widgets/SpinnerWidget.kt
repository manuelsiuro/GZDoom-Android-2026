package com.opentouchgaming.androidcore.ui.widgets

import android.R
import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.databinding.WidgetViewSpinnerBinding

class SpinnerWidget(
    val context: Context, view: View, title: String, description: String, items: Array<Pair<String, View?>>, settingPrefix: String, default: Int, image: Int = 0
)
{
    private var binding = WidgetViewSpinnerBinding.bind(view)

    // Callback
    var callback: ((Int) -> Unit)? = null

    init
    {
        binding.title.text = title
        binding.description.text = description

        if (image != 0) binding.imageView.setImageResource(image)


        val labels = ArrayList<String>()
        for (i in items)
        {
            labels.add(i.first)
        }

        val adapter = ArrayAdapter(context, R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter

        // Save setting on change
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener
        {
            override fun onNothingSelected(parent: AdapterView<*>?)
            {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                AppSettings.setIntOption(context, settingPrefix + "_spinner", position)

                callback?.invoke(position)

                // Hide/show views if set
                for (i in items)
                {
                    if (i.second != items[position].second) i.second?.visibility = View.GONE
                }

                // Show view
                items[position].second?.visibility = View.VISIBLE
            }
        }

        // Set to existing value if set
        var selection = fetchValue(context, settingPrefix, default)
        if (selection >= labels.size) selection = 0

        binding.spinner.setSelection(selection)
    }

    companion object
    {
        //This annotation tells Java classes to treat this method as if it was a static to [KotlinClass]
        @JvmStatic
        fun fetchValue(context: Context, settingPrefix: String, default: Int): Int
        {
            return AppSettings.getIntOption(context, settingPrefix + "_spinner", default)
        }
    }
}