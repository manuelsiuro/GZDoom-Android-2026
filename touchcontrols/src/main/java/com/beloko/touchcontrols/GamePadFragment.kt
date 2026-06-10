@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.beloko.touchcontrols

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bda.controller.Controller
import com.bda.controller.ControllerListener
import com.bda.controller.StateEvent
import java.io.IOException
import java.util.ArrayList

class GamePadFragment : Fragment() {
    private val LOG = "GamePadFragment"
    private val mListener = MogaControllerListener()
    private lateinit var listView: ListView
    private lateinit var adapter: ControlListAdapter
    private lateinit var info: TextView
    private lateinit var config: ControlConfig
    private val genericAxisValues = GenericAxisValues()
    private var mogaController: Controller? = null
    private var isHidden = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = ControlConfig(TouchSettings.gamePadControlsFile, gamepadActions)

        try {
            config.loadControls()
        } catch (e: IOException) {
            // Failed to load, so save the default
            try {
                config.saveControls()
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
        } catch (e: ClassNotFoundException) {
            // ignored
        }

        mogaController = Controller.getInstance(requireActivity())
        MogaHack.init(mogaController!!, requireActivity())
        mogaController!!.setListener(mListener, Handler())
    }

    override fun onHiddenChanged(hidden: Boolean) {
        isHidden = hidden
        super.onHiddenChanged(hidden)
    }

    override fun onPause() {
        super.onPause()
        mogaController!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mogaController!!.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mogaController!!.exit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val mainView = inflater.inflate(R.layout.fragment_gamepad, null)

        val enableCb = mainView.findViewById<CheckBox>(R.id.gamepad_enable_checkbox)
        enableCb.isChecked = TouchSettings.gamePadEnabled

        enableCb.setOnCheckedChangeListener { _, isChecked ->
            TouchSettings.setBoolOption(requireActivity(), "gamepad_enabled", isChecked)
            TouchSettings.gamePadEnabled = isChecked
            setListViewEnabled(TouchSettings.gamePadEnabled)
        }

        val hideCtrlCb = mainView.findViewById<CheckBox>(R.id.gamepad_hide_touch_checkbox)
        hideCtrlCb.isChecked = TouchSettings.hideTouchControls

        hideCtrlCb.setOnCheckedChangeListener { _, isChecked ->
            TouchSettings.setBoolOption(requireActivity(), "hide_touch_controls", isChecked)
            TouchSettings.hideTouchControls = isChecked
        }

        val help = mainView.findViewById<Button>(R.id.gamepad_help_button)
        help.setOnClickListener { }

        listView = mainView.findViewById(R.id.gamepad_listview)
        adapter = ControlListAdapter(requireActivity())
        listView.adapter = adapter

        setListViewEnabled(TouchSettings.gamePadEnabled)

        listView.setSelector(R.drawable.layout_sel_background)
        listView.setOnItemClickListener { _, _, pos, _ -> config.startMonitor(requireActivity(), pos) }

        listView.setOnItemLongClickListener { _, _, pos, _ -> config.showExtraOptions(requireActivity(), pos) }

        adapter.notifyDataSetChanged()

        info = mainView.findViewById(R.id.gamepad_info_textview)
        info.text = "Select Action"
        info.setTextColor(ContextCompat.getColor(requireActivity(), android.R.color.holo_blue_light))

        config.setTextView(requireActivity(), info)

        return mainView
    }

    private fun setListViewEnabled(v: Boolean) {
        listView.isEnabled = v
        if (v) {
            listView.alpha = 1f
        } else {
            listView.alpha = 0.3f
        }
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        genericAxisValues.setAndroidValues(event)

        if (config.onGenericMotionEvent(genericAxisValues)) {
            adapter.notifyDataSetChanged()
        }

        // If gamepad tab visible always steal
        return !isHidden
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (config.onKeyDown(keyCode, event)) {
            adapter.notifyDataSetChanged()
            return true
        }
        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (config.onKeyUp(keyCode, event)) {
            adapter.notifyDataSetChanged()
            return true
        }
        return false
    }

    inner class ControlListAdapter(private val context: Activity) : BaseAdapter() {
        fun add(string: String) {}

        override fun getCount(): Int = config.getSize()

        override fun getItem(arg0: Int): Any? = null

        override fun getItemId(arg0: Int): Long = 0

        override fun getView(position: Int, convertView: View?, list: ViewGroup): View =
            config.getView(requireActivity(), position)
    }

    inner class MogaControllerListener : ControllerListener {
        override fun onKeyEvent(event: com.bda.controller.KeyEvent) {
            if (event.action == com.bda.controller.KeyEvent.ACTION_DOWN) {
                onKeyDown(event.keyCode, null)
            } else if (event.action == com.bda.controller.KeyEvent.ACTION_UP) {
                onKeyUp(event.keyCode, null)
            }
        }

        override fun onMotionEvent(event: com.bda.controller.MotionEvent) {
            genericAxisValues.setMogaValues(event)

            if (config.onGenericMotionEvent(genericAxisValues)) {
                adapter.notifyDataSetChanged()
            }
        }

        override fun onStateEvent(event: StateEvent) {
            Log.d(LOG, "onStateEvent " + event.state)
        }
    }

    companion object {
        // This is a bit shit, set this before instantiating the fragment
        @JvmField
        var gamepadActions: ArrayList<ActionInput>? = null
    }
}
