package com.opentouchgaming.androidcore.ui.widgets


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.R
import com.opentouchgaming.androidcore.databinding.WidgetViewRadioLayoutBinding


class RadioLayoutWidget(
    val context: Context, view: View, private val title: String, private val description: String, private val settingPrefix: String, val default: Int
)
{
    private var binding = WidgetViewRadioLayoutBinding.bind(view)

    // Callback
    var callback: ((Int) -> Unit)? = null

    // Class to store our radio options
    data class Item(val title: String, val button: RadioButton, val view: View?)

    val items: ArrayList<Item> = ArrayList()

    fun addRadio(context: Context, title: String, viewLayout: View?)
    {
        val button = RadioButton(context)

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Don't add vertical line for first element
        if (items.size != 0) inflater.inflate(R.layout.view_vertical_line, binding.radioLayout)

        button.text = title

        // This is outside the change listener so it get 'saved' now
        val n = items.size
        button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) radioChecked(n)
        }

        binding.radioLayout.addView(button)

        items.add(Item(title, button, viewLayout))
    }

    private fun radioChecked(n: Int)
    {
        for (item in items)
        {
            // If NOT the one just checked, uncheck it. The 'radio' bit of the buttons does not
            if (item == items[n])
            {
                item.view?.visibility = View.VISIBLE
            }
            else // Disable others
            {
                item.button.isChecked = false
                item.view?.visibility = View.GONE
            }
        }

        callback?.invoke(n)
    }

    companion object
    {
        //This annotation tells Java classes to treat this method as if it was a static to [KotlinClass]
        @JvmStatic
        fun fetchValue(context: Context, settingPrefix: String, default: Int): Int
        {
            return AppSettings.getIntOption(context, settingPrefix, default)
        }
    }
}