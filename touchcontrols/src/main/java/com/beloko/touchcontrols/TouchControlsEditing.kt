@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.beloko.touchcontrols

import android.app.Activity
import android.app.Dialog
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.ToggleButton

class TouchControlsEditing {

    // Written field-by-field from native code via JNIGetControlInfo — keep @JvmField + no-arg ctor.
    class ControlInfo {
        @JvmField
        var tag: String? = null

        @JvmField
        var image: String? = null

        @JvmField
        var enabled: Boolean = false

        @JvmField
        var hidden: Boolean = false
    }

    internal class ListAdapter(private val context: Activity) : BaseAdapter() {

        fun add(string: String) {}

        override fun getCount(): Int = JNIGetNbrControls()

        override fun getItem(arg0: Int): Any? = null

        override fun getItemId(arg0: Int): Long = 0

        override fun getView(position: Int, convertView: View?, list: ViewGroup): View {
            // Don't reuse view otherwise onCheckedChange gets called.
            val view = activity!!.layoutInflater.inflate(R.layout.edit_controls_listview_item, null)

            val image = view.findViewById<ImageView>(R.id.imageView)
            val name = view.findViewById<TextView>(R.id.name_textview)
            val hidden = view.findViewById<ToggleButton>(R.id.hidden_switch)

            val ci = ControlInfo()
            JNIGetControlInfo(position, ci)

            name.text = ci.tag
            hidden.isChecked = !ci.hidden
            hidden.tag = position

            hidden.setOnCheckedChangeListener { buttonView, isChecked ->
                val pos = buttonView.tag as Int
                JNISetHidden(pos, !isChecked)
                adapter!!.notifyDataSetChanged()
            }

            val png = activity!!.filesDir.toString() + "/" + ci.image + ".png"
            Log.d(TAG, "png = $png")
            val bm = BitmapDrawable(png)

            image.setImageDrawable(bm)
            return view
        }
    }

    companion object {
        private const val TAG = "TouchControlsEditing"

        private var adapter: ListAdapter? = null

        private var activity: Activity? = null

        @JvmStatic
        external fun JNIGetControlInfo(pos: Int, info: ControlInfo)

        @JvmStatic
        external fun JNIGetNbrControls(): Int

        @JvmStatic
        external fun JNISetHidden(pos: Int, hidden: Boolean)

        @JvmStatic
        fun setup(a: Activity) {
            activity = a
        }

        @JvmStatic
        fun show() {
            show(activity)
        }

        @JvmStatic
        fun show(act: Activity?) {
            Log.d(TAG, "showSettings")

            if (act != null) activity = act

            activity!!.runOnUiThread {
                val dialog = Dialog(activity!!)
                val listView = ListView(activity)

                dialog.setContentView(listView)
                dialog.setTitle("Add/remove buttons")
                dialog.setCancelable(true)

                adapter = ListAdapter(activity!!)
                listView.adapter = adapter

                dialog.window!!.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                )

                dialog.show()
            }
        }
    }
}
