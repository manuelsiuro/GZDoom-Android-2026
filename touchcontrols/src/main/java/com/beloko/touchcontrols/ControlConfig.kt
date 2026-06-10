package com.beloko.touchcontrols

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.ArrayList
import kotlin.math.abs

class ControlConfig(file: String?, gamepadActions: ArrayList<ActionInput>?) : Serializable {

    private val LOG = "QuakeControlConfig"
    private var ctx: Context? = null
    private var infoTextView: TextView? = null
    private var filename: String? = null
    var ignoreDirectionFromJoystick = false
    val actions = ArrayList<ActionInput>()
    private var actionMonitor: ActionInput? = null
    private var monitoring = false
    private val axisTest = intArrayOf(
        MotionEvent.AXIS_HAT_X,
        MotionEvent.AXIS_HAT_Y,
        MotionEvent.AXIS_LTRIGGER,
        MotionEvent.AXIS_RTRIGGER,
        MotionEvent.AXIS_RUDDER,
        MotionEvent.AXIS_RX,
        MotionEvent.AXIS_RY,
        MotionEvent.AXIS_RZ,
        MotionEvent.AXIS_THROTTLE,
        MotionEvent.AXIS_X,
        MotionEvent.AXIS_Y,
        MotionEvent.AXIS_Z,
        MotionEvent.AXIS_BRAKE,
        MotionEvent.AXIS_GAS,
    )

    init {
        // Conditional null check hotfix to prevent crashing
        if (gamepadActions != null && file != null) {
            actions.addAll(gamepadActions)
            filename = file
        } else {
            Log.d(LOG, "TouchControls gamepadActions or file was null!")
        }
    }

    fun setTextView(c: Context, tv: TextView) {
        ctx = c
        infoTextView = tv
    }

    @Throws(IOException::class)
    fun saveControls() {
        saveControls(File(filename!!))
    }

    @Throws(IOException::class)
    fun saveControls(file: File) {
        if (TouchSettings.DEBUG) Log.d(LOG, "saveControls, file = $file")
        val out = ObjectOutputStream(FileOutputStream(file))
        out.writeObject(actions)
        out.close()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun loadControls() {
        loadControls(File(filename!!))
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun loadControls(file: File) {
        if (TouchSettings.DEBUG) Log.d(LOG, "loadControls, file = $file")

        val fis = FileInputStream(file)
        val input = ObjectInputStream(fis)

        @Suppress("UNCHECKED_CAST")
        val cd = input.readObject() as ArrayList<ActionInput>
        if (TouchSettings.DEBUG) Log.d(LOG, "loadControls, file loaded OK")
        input.close()

        for (d in cd) {
            for (a in actions) {
                if (d.tag!!.contentEquals(a.tag)) {
                    a.invert = d.invert
                    a.source = d.source
                    a.sourceType = d.sourceType
                    a.sourcePositive = d.sourcePositive
                    a.scale = d.scale
                    if (a.scale == 0f) a.scale = 1f
                }
            }
        }

        // Now check no buttons are also assigned to analog, if it is, clear the buttons.
        // This is because n00bs keep assigning movement analog AND buttons!
        for (a in actions) {
            if (a.source != -1 && a.sourceType == Type.ANALOG && a.actionType == Type.BUTTON) {
                for (aCheck in actions) {
                    if (aCheck.sourceType == Type.ANALOG && aCheck.actionType == Type.ANALOG) {
                        if (a.source == aCheck.source) {
                            a.source = -1
                            break
                        }
                    }
                }
            }
        }

        fis.close()
    }

    fun updated() {
        try {
            saveControls(File(filename!!))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun showExtraOptions(act: Activity, pos: Int): Boolean {
        val input = actions[pos]

        if (input.actionType == Type.ANALOG) {
            val dialog = Dialog(act)
            dialog.setTitle("Axis Sensitivity Setting")
            dialog.setCancelable(true)

            val l = LinearLayout(act)
            l.orientation = LinearLayout.VERTICAL

            val sb = SeekBar(act)
            l.addView(sb)

            sb.max = 100
            sb.progress = (input.scale * 50).toInt()

            val invert = CheckBox(act)
            invert.text = "Invert"
            invert.isChecked = input.invert

            l.addView(invert)

            dialog.setOnDismissListener {
                input.scale = sb.progress.toFloat() / 50f
                input.invert = invert.isChecked
                updated()
            }

            dialog.setContentView(l)
            dialog.show()
            return true
        }
        return false
    }

    fun startMonitor(act: Activity, pos: Int) {
        actionMonitor = actions[pos]
        monitoring = true

        if (actionMonitor!!.actionType == Type.ANALOG) {
            infoTextView!!.text = "Move Stick for: " + actionMonitor!!.description
        } else {
            infoTextView!!.text = "Press Button for: " + actionMonitor!!.description
        }

        infoTextView!!.setTextColor(ContextCompat.getColor(ctx!!, android.R.color.holo_green_light))
    }

    fun onGenericMotionEvent(event: GenericAxisValues): Boolean {
        if (TouchSettings.DEBUG) Log.d(LOG, "onGenericMotionEvent")
        if (monitoring) {
            val monitor = actionMonitor
            if (monitor != null) {
                for (a in axisTest) {
                    if (abs(event.getAxisValue(a)) > 0.6) {
                        monitor.source = a
                        monitor.sourceType = Type.ANALOG
                        // Used for button actions
                        monitor.sourcePositive = event.getAxisValue(a) > 0

                        monitoring = false

                        if (TouchSettings.DEBUG) {
                            Log.d(LOG, monitor.description + " = Analog (" + monitor.source + ")")
                        }

                        infoTextView!!.text = "Select Action"
                        infoTextView!!.setTextColor(
                            ContextCompat.getColor(ctx!!, android.R.color.holo_blue_light)
                        )

                        updated()
                        return true
                    }
                }
            }
        }
        return false
    }

    fun isMonitoring(): Boolean = monitoring

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (TouchSettings.DEBUG) Log.d(LOG, "onKeyDown $keyCode")

        if (monitoring) {
            if (keyCode == KeyEvent.KEYCODE_BACK) { // Cancel and clear button assignment
                actionMonitor!!.source = -1
                actionMonitor!!.sourceType = Type.BUTTON
                monitoring = false
                infoTextView!!.text = "CANCELED"
                infoTextView!!.setTextColor(ContextCompat.getColor(ctx!!, android.R.color.holo_red_light))

                updated()
                return true
            } else {
                val monitor = actionMonitor
                if (monitor != null) {
                    if (monitor.actionType != Type.ANALOG) {
                        monitor.source = keyCode
                        monitor.sourceType = Type.BUTTON
                        monitoring = false

                        infoTextView!!.text = "Select Action"
                        infoTextView!!.setTextColor(
                            ContextCompat.getColor(ctx!!, android.R.color.holo_blue_light)
                        )

                        updated()
                        return true
                    }
                }
            }
        }

        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = false

    fun getSize(): Int = actions.size

    fun getView(ctx: Activity, nbr: Int): View {
        val view = ctx.layoutInflater.inflate(R.layout.controls_listview_item, null)
        val image = view.findViewById<ImageView>(R.id.imageView)
        val name = view.findViewById<TextView>(R.id.name_textview)
        val binding = view.findViewById<TextView>(R.id.binding_textview)
        val settingImage = view.findViewById<ImageView>(R.id.settings_imageview)

        val ai = actions[nbr]

        if (ai.actionType == Type.BUTTON || ai.actionType == Type.MENU) {
            if (ai.sourceType == Type.ANALOG) {
                binding.text = MotionEvent.axisToString(ai.source)
            } else {
                binding.text = KeyEvent.keyCodeToString(ai.source)
            }

            settingImage.visibility = View.GONE

            if (ai.actionType == Type.MENU) {
                name.setTextColor(0xFF00aeef.toInt()) // BLUEY
                image.setImageResource(R.drawable.gamepad_menu)
            } else {
                image.setImageResource(R.drawable.gamepad)
            }
        } else if (ai.actionType == Type.ANALOG) {
            binding.text = MotionEvent.axisToString(ai.source)
            settingImage.setOnClickListener { showExtraOptions(ctx, nbr) }
            name.setTextColor(0xFFf7941d.toInt()) // ORANGE
        }

        name.text = ai.description

        return view
    }

    enum class Type { ANALOG, BUTTON, MENU }

    companion object {
        const val LOOK_MODE_MOUSE = 0
        const val LOOK_MODE_ABSOLUTE = 1
        const val LOOK_MODE_JOYSTICK = 2

        const val ACTION_ANALOG_FWD = 0x100
        const val ACTION_ANALOG_STRAFE = 0x101
        const val ACTION_ANALOG_PITCH = 0x102
        const val ACTION_ANALOG_YAW = 0x103
        const val PORT_ACT_LEFT = 1
        const val PORT_ACT_RIGHT = 2
        const val PORT_ACT_FWD = 3
        const val PORT_ACT_BACK = 4
        const val PORT_ACT_LOOK_UP = 5
        const val PORT_ACT_LOOK_DOWN = 6
        const val PORT_ACT_MOVE_LEFT = 7
        const val PORT_ACT_MOVE_RIGHT = 8
        const val PORT_ACT_STRAFE = 9
        const val PORT_ACT_SPEED = 10
        const val PORT_ACT_USE = 11
        const val PORT_ACT_JUMP = 12
        const val PORT_ACT_ATTACK = 13
        const val PORT_ACT_UP = 14
        const val PORT_ACT_DOWN = 15
        const val PORT_ACT_NEXT_WEP = 16
        const val PORT_ACT_PREV_WEP = 17

        // Quake 2
        const val PORT_ACT_INVEN = 18
        const val PORT_ACT_INVUSE = 19
        const val PORT_ACT_INVDROP = 20
        const val PORT_ACT_INVPREV = 21
        const val PORT_ACT_INVNEXT = 22
        const val PORT_ACT_HELPCOMP = 23

        // Doom
        const val PORT_ACT_MAP = 30
        const val PORT_ACT_MAP_UP = 31
        const val PORT_ACT_MAP_DOWN = 32
        const val PORT_ACT_MAP_LEFT = 33
        const val PORT_ACT_MAP_RIGHT = 34
        const val PORT_ACT_MAP_ZOOM_IN = 35
        const val PORT_ACT_MAP_ZOOM_OUT = 36

        // RTCW
        const val PORT_ACT_ZOOM_IN = 50
        const val PORT_ACT_ALT_FIRE = 51
        const val PORT_ACT_RELOAD = 52
        const val PORT_ACT_QUICKSAVE = 53
        const val PORT_ACT_QUICKLOAD = 54
        const val PORT_ACT_KICK = 56
        const val PORT_ACT_LEAN_LEFT = 57
        const val PORT_ACT_LEAN_RIGHT = 58

        // MALICE
        const val PORT_MALICE_USE = 59
        const val PORT_MALICE_RELOAD = 60
        const val PORT_MALICE_CYCLE = 61

        // JK2
        const val PORT_ACT_ALT_ATTACK = 64
        const val PORT_ACT_NEXT_FORCE = 65
        const val PORT_ACT_PREV_FORCE = 66
        const val PORT_ACT_FORCE_USE = 67
        const val PORT_ACT_DATAPAD = 68
        const val PORT_ACT_FORCE_SELECT = 69
        const val PORT_ACT_WEAPON_SELECT = 70
        const val PORT_ACT_SABER_STYLE = 71
        const val PORT_ACT_FORCE_PULL = 75
        const val PORT_ACT_FORCE_MIND = 76
        const val PORT_ACT_FORCE_LIGHT = 77
        const val PORT_ACT_FORCE_HEAL = 78
        const val PORT_ACT_FORCE_GRIP = 79
        const val PORT_ACT_FORCE_SPEED = 80
        const val PORT_ACT_FORCE_PUSH = 81
        const val PORT_ACT_SABER_SEL = 87 // Just chooses weapon 1 so show/hide saber.

        // Chocolate
        const val PORT_ACT_GAMMA = 90
        const val PORT_ACT_SHOW_WEAPONS = 91
        const val PORT_ACT_SHOW_KEYS = 92
        const val PORT_ACT_FLY_UP = 93
        const val PORT_ACT_FLY_DOWN = 94

        // Custom
        const val PORT_ACT_CUSTOM_0 = 150
        const val PORT_ACT_CUSTOM_1 = 151
        const val PORT_ACT_CUSTOM_2 = 152
        const val PORT_ACT_CUSTOM_3 = 153
        const val PORT_ACT_CUSTOM_4 = 154
        const val PORT_ACT_CUSTOM_5 = 155
        const val PORT_ACT_CUSTOM_6 = 156
        const val PORT_ACT_CUSTOM_7 = 157

        // Menu
        const val MENU_UP = 0x200
        const val MENU_DOWN = 0x201
        const val MENU_LEFT = 0x202
        const val MENU_RIGHT = 0x203
        const val MENU_SELECT = 0x204
        const val MENU_BACK = 0x205

        private const val serialVersionUID = 1L
    }
}
