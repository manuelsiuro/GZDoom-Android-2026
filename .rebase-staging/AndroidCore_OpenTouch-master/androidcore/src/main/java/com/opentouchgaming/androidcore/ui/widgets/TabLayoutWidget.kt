package com.opentouchgaming.androidcore.ui.widgets


import android.content.Context
import android.view.View
import com.google.android.material.tabs.TabLayout
import com.opentouchgaming.androidcore.AppSettings
import com.opentouchgaming.androidcore.databinding.WidgetViewTabLayoutBinding


class TabLayoutWidget(val context: Context, view: View)
{
    private var binding = WidgetViewTabLayoutBinding.bind(view)

    // Callback
    var callback: ((Int) -> Unit)? = null

    init
    {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener
        {
            override fun onTabSelected(tab: TabLayout.Tab)
            {
                tabChanged(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab)
            {

            }

            override fun onTabReselected(tab: TabLayout.Tab)
            {

            }
        })
    }

    val items: ArrayList<View?> = ArrayList()

    fun addTab(title: String, viewLayout: View?)
    {

        val tab: TabLayout.Tab = binding.tabLayout.newTab().setText(title)

        items.add(viewLayout)

        binding.tabLayout.addTab(tab)
    }

    fun selectTab(pos: Int)
    {
        val tab: TabLayout.Tab? = binding.tabLayout.getTabAt(pos)
        tab?.select()
        tabChanged(pos)
    }

    private fun tabChanged(n: Int)
    {
        callback?.invoke(n)

        // Hide all views first
        for (view in items)
        {
            if (view != items[n]) view?.visibility = View.GONE
        }

        // Show selected view
        items[n]?.visibility = View.VISIBLE
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