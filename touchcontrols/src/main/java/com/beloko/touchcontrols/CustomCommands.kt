package com.beloko.touchcontrols

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.mobeta.android.dslv.DragSortListView
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.ArrayList

class CustomCommands private constructor() {
    private var commands: ArrayList<QuickCommand> = ArrayList()
    private var adapter: QuickCommandsAdapter
    private var editView: LinearLayout
    private var nameEditText: EditText
    private var commandEditText: EditText
    private var listView: DragSortListView

    private val onDrop = DragSortListView.DropListener { from, to ->
        if (TouchSettings.DEBUG) Log.d(LOG, "drop $from to $to")
        if (from != to) {
            val f = commands.removeAt(from)
            commands.add(to, f)
            saveQuickCommands()
        }
    }

    init {
        loadQuickCommands(currentList)

        val dialog = Dialog(activity!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.quick_commands)
        dialog.setCancelable(true)

        val add = dialog.findViewById<ImageView>(R.id.add_quick_command_image)
        add.setOnClickListener { editView.visibility = View.VISIBLE }

        val main = dialog.findViewById<Button>(R.id.main_button)
        main.setOnClickListener {
            loadQuickCommands(QuickCmdList.MAIN)
            adapter.notifyDataSetChanged()
        }

        val mod = dialog.findViewById<Button>(R.id.mod_button)
        mod.setOnClickListener {
            loadQuickCommands(QuickCmdList.MOD)
            adapter.notifyDataSetChanged()
        }

        if (modCmdsPath == null) {
            mod.visibility = View.GONE
        }

        editView = dialog.findViewById(R.id.edit_qc_view)
        nameEditText = dialog.findViewById(R.id.name_edittext)
        commandEditText = dialog.findViewById(R.id.command_edittext)

        val cancel = dialog.findViewById<Button>(R.id.cancel_button)
        cancel.setOnClickListener { editView.visibility = View.GONE }

        val save = dialog.findViewById<Button>(R.id.save_button)
        save.setOnClickListener {
            val qc = QuickCommand(nameEditText.text.toString(), commandEditText.text.toString())
            commands.add(qc)
            saveQuickCommands()
            nameEditText.setText("")
            commandEditText.setText("")
            editView.visibility = View.GONE
        }

        listView = dialog.findViewById(R.id.list)
        listView.setDragEnabled(true)
        listView.setDropListener(onDrop)

        adapter = QuickCommandsAdapter(activity!!)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, pos, _ ->
            quakeIf!!.quickCommand_if(commands[pos].command!!)
            dialog.dismiss()
        }

        listView.setOnItemLongClickListener { _, _, pos, _ ->
            val alertDialogBuilder = AlertDialog.Builder(activity)
            alertDialogBuilder.setTitle("Delete Command?")
            alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    commands.removeAt(pos)
                    saveQuickCommands()
                }
                .setNegativeButton("No") { dialog1, _ -> dialog1.cancel() }
            alertDialogBuilder.create().show()
            true
        }

        adapter.notifyDataSetChanged()

        dialog.show()
    }

    private fun loadQuickCommands(m: QuickCmdList) {
        currentList = m
        val filename: String?
        if (currentList == QuickCmdList.MAIN) {
            filename = mainCmdsPath
        } else {
            if (modCmdsPath == null) {
                filename = mainCmdsPath
                currentList = QuickCmdList.MAIN
            } else {
                filename = modCmdsPath
            }
        }

        try {
            val input = ObjectInputStream(FileInputStream(filename))
            @Suppress("UNCHECKED_CAST")
            commands = input.readObject() as ArrayList<QuickCommand>
            if (TouchSettings.DEBUG) Log.d(LOG, "Read commands")
            input.close()
            return
        } catch (ignored: IOException) {
        } catch (ignored: ClassNotFoundException) {
        }
        // failed load, load default
        commands = ArrayList()
    }

    private fun saveQuickCommands() {
        val filename = if (currentList == QuickCmdList.MAIN) mainCmdsPath else modCmdsPath

        try {
            val out = ObjectOutputStream(FileOutputStream(filename))
            out.writeObject(commands)
            out.close()
        } catch (ex: IOException) {
            Toast.makeText(activity, "Error saving commands $ex", Toast.LENGTH_LONG).show()
        }
        adapter.notifyDataSetChanged()
    }

    enum class QuickCmdList { MAIN, MOD }

    inner class QuickCommandsAdapter(private val context: Activity) : BaseAdapter() {
        fun add(string: String) {}

        override fun getCount(): Int = commands.size

        override fun getItem(arg0: Int): Any? = null

        override fun getItemId(arg0: Int): Long = 0

        override fun getView(position: Int, convertView: View?, list: ViewGroup): View {
            val view = activity!!.layoutInflater.inflate(R.layout.quick_command_listview_item, null)
            val image = view.findViewById<ImageView>(R.id.imageView)
            val title = view.findViewById<TextView>(R.id.title_textview)
            val command = view.findViewById<TextView>(R.id.command_textview)
            title.text = commands[position].title
            command.text = commands[position].command
            return view
        }
    }

    companion object {
        private const val LOG = "QuakeCustomCommands"

        @JvmField
        var activity: Activity? = null

        @JvmField
        var quakeIf: ControlInterface? = null

        private var mainCmdsPath: String? = null
        private var modCmdsPath: String? = null
        private var currentList = QuickCmdList.MAIN

        @JvmStatic
        fun setup(a: Activity, qif: ControlInterface, main: String?, mod: String?) {
            activity = a
            quakeIf = qif
            mainCmdsPath = main
            modCmdsPath = mod
            if (TouchSettings.DEBUG) Log.d(LOG, "main = $main, mod = $mod")
        }

        @JvmStatic
        fun showCommands() {
            if (TouchSettings.DEBUG) Log.d(LOG, "showCommands")
            CustomCommands()
        }
    }
}
