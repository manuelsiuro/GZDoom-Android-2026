package net.nullsum.freedoom

import android.app.Activity
import android.app.Dialog
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import java.io.File
import java.util.ArrayList
import java.util.Collections

open class ModSelectDialog(act: Activity?, path: String?, prboomMode: Boolean) {
    private val dialog: Dialog
    private val basePath: String?
    private var extraPath = ""
    private val PrBoomMode: Boolean
    private val filesArray = ArrayList<String>()
    private val selectedArray = ArrayList<String>()
    private val activity: Activity?
    private val resultTextView: TextView
    private val infoTextView: TextView

    private val listAdapter: ModsListAdapter

    init {
        basePath = path
        activity = act
        PrBoomMode = prboomMode

        dialog = Dialog(activity!!)
        dialog.setContentView(R.layout.dialog_select_mods_wads)
        dialog.setTitle("Touch Control Sensitivity Settings")
        dialog.setCancelable(true)

        dialog.setOnKeyListener { _, keyCode, _ ->
            // TODO Auto-generated method stub
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (extraPath.isEmpty() || !extraPath.contains("/")) {
                    return@setOnKeyListener false
                } else {
                    extraPath = extraPath.substring(0, extraPath.lastIndexOf("/"))
                    populateList(extraPath)
                    return@setOnKeyListener true
                }
            }
            false
        }

        resultTextView = dialog.findViewById(R.id.result_textView)
        infoTextView = dialog.findViewById(R.id.info_textView)

        val listView = dialog.findViewById<ListView>(R.id.listview)
        listAdapter = ModsListAdapter()
        listView.adapter = listAdapter

        listView.setOnItemClickListener { _, _, position, _ ->
            if (filesArray[position].startsWith("/")) {
                populateList(extraPath + filesArray[position])
            } else { //select/deselect
                var removed = false
                val iter = selectedArray.listIterator()
                while (iter.hasNext()) {
                    val s = iter.next()
                    if (s.contentEquals(extraPath + "/" + filesArray[position])) {
                        iter.remove()
                        removed = true
                    }
                }

                if (!removed)
                    selectedArray.add(extraPath + "/" + filesArray[position])

                //Log.d("TEST", "list size = " + selectedArray.size());

                listAdapter.notifyDataSetChanged()
                resultTextView.text = getResult()
            }
        }

        //Add folders on long press
        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (filesArray[position].startsWith("/")) {
                var removed = false
                val name = filesArray[position].substring(1)
                val iter = selectedArray.listIterator()
                while (iter.hasNext()) {
                    val s = iter.next()
                    if (s.contentEquals("$extraPath/$name")) {
                        iter.remove()
                        removed = true
                    }
                }

                if (!removed)
                    selectedArray.add("$extraPath/$name")

                //Log.d("TEST", "list size = " + selectedArray.size());

                listAdapter.notifyDataSetChanged()
                resultTextView.text = getResult()
                return@setOnItemLongClickListener true
            }
            false
        }

        val wadsButton = dialog.findViewById<Button>(R.id.wads_button)
        wadsButton.setOnClickListener { populateList("wads") }

        val modsButton = dialog.findViewById<Button>(R.id.mods_button)
        modsButton.setOnClickListener { populateList("mods") }

        val okButton = dialog.findViewById<Button>(R.id.ok_button)
        okButton.setOnClickListener {
            dialog.dismiss()
            resultResult(getResult())
        }

        populateList("wads")

        dialog.show()
    }

    open fun resultResult(result: String) {
    }

    fun getResult(): String {
        var result = StringBuilder()

        if (PrBoomMode && selectedArray.size > 0)
            result = StringBuilder("-file")

        for (n in selectedArray.indices) {
            if (PrBoomMode) {
                result.append(" ").append(selectedArray[n])
            } else {
                if (selectedArray[n].endsWith(".deh") || selectedArray[n].endsWith(".bex"))
                    result.append("-deh ").append(selectedArray[n]).append(" ")
                else
                    result.append("-file ").append(selectedArray[n]).append(" ")
            }
        }

        return result.toString()
    }

    private fun populateList(path: String) {
        extraPath = path
        dialog.setTitle(extraPath)
        val wadDir = "$basePath/$path"
        val files = File(wadDir).listFiles()
        filesArray.clear()
        if (files != null)
            for (f in files) {
                if (!f.isDirectory) {
                    val file = f.name.lowercase()
                    if (file.endsWith(".wad") || file.endsWith(".pk3") || file.endsWith(".pk7") || file.endsWith(".deh")) {
                        filesArray.add(f.name)
                    }
                } else { //Now also do directories
                    filesArray.add("/" + f.name)
                }
            }

        Collections.sort(filesArray)

        if (filesArray.size == 0)
            infoTextView.text = "Please copy addon wad/mods to here: \"$basePath/$path\""
        else
            infoTextView.text = ""

        listAdapter.notifyDataSetChanged()
        resultTextView.text = getResult()
    }

    inner class ModsListAdapter : BaseAdapter() {

        fun add(string: String) {
        }

        override fun getCount(): Int {
            return filesArray.size
        }

        override fun getItem(arg0: Int): Any? {
            // TODO Auto-generated method stub
            return null
        }

        override fun getItemId(arg0: Int): Long {
            // TODO Auto-generated method stub
            return 0
        }

        override fun getView(position: Int, convertView: View?, list: ViewGroup?): View {
            val view: View = convertView
                ?: activity!!.layoutInflater.inflate(R.layout.listview_item_mods_wads, null)

            var selected = false
            for (s in selectedArray) {
                if (s.contentEquals(extraPath + "/" + filesArray[position])) {
                    selected = true
                }
            }

            if (selected) {
                view.setBackgroundResource(R.drawable.layout_sel_background)
            } else {
                view.setBackgroundResource(0)
            }

            val title = view.findViewById<TextView>(R.id.name_textview)

            title.text = filesArray[position]
            return view
        }
    }
}
