@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package net.nullsum.freedoom

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Collections

class DirectoryChooserDialog(
    private val m_context: Context,
    private val m_chosenDirectoryListener: ChosenDirectoryListener?
) {
    private var m_isNewFolderEnabled = true
    private var m_sdcardDirectory = ""
    private var m_titleView: TextView? = null

    private var m_dir = ""
    private var m_subdirs: MutableList<String>? = null
    private var m_listAdapter: ArrayAdapter<String>? = null

    init {
        val externalDir = m_context.getExternalFilesDir(null) ?: m_context.filesDir
        m_sdcardDirectory = externalDir.absolutePath

        try {
            m_sdcardDirectory = File(m_sdcardDirectory).canonicalPath
        } catch (ioe: IOException) {
        }
    }

    fun getNewFolderEnabled(): Boolean {
        return m_isNewFolderEnabled
    }

    ///////////////////////////////////////////////////////////////////////
    // setNewFolderEnabled() - enable/disable new folder button
    ///////////////////////////////////////////////////////////////////////

    fun setNewFolderEnabled(isNewFolderEnabled: Boolean) {
        m_isNewFolderEnabled = isNewFolderEnabled
    }

    fun chooseDirectory() {
        // Initial directory is sdcard directory
        chooseDirectory(m_sdcardDirectory)
    }

    ///////////////////////////////////////////////////////////////////////
    // chooseDirectory() - load directory chooser dialog for initial
    // default sdcard directory
    ///////////////////////////////////////////////////////////////////////

    fun chooseDirectory(dir: String?) {
        var dir = dir ?: m_sdcardDirectory
        val dirFile = File(dir)

        Log.d("dir", "dir = $dir")

        if (!dirFile.exists() || !dirFile.isDirectory) {
            dir = m_sdcardDirectory
        }

        try {
            dir = File(dir).canonicalPath
        } catch (ioe: IOException) {
            Log.d("dir", "an exception happened while trying to get a cannon. path")
            return
        }

        m_dir = dir

        m_subdirs = getDirectories(dir)

        val directoryOnClickListener = DialogInterface.OnClickListener { dialog, item ->
            // Navigate into the sub-directory
            m_dir += "/" + (dialog as AlertDialog).listView.adapter.getItem(item)

            try {
                m_dir = File(m_dir).canonicalPath
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            updateDirectory()
        }

        val dialogBuilder =
            createDirectoryChooserDialog(dir, m_subdirs!!, directoryOnClickListener)

        dialogBuilder.setPositiveButton("OK") { _, _ ->
            // Current directory chosen
            // Call registered listener supplied with the chosen directory
            m_chosenDirectoryListener?.onChosenDir(m_dir)
        }.setNegativeButton("Cancel", null)

        val dirsDialog = dialogBuilder.create()

        dirsDialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                // Back button pressed
                if (m_dir == "/") {
                    // The very top level directory, do nothing
                    false
                } else {
                    // Navigate back to an upper directory
                    m_dir = File(m_dir).parent ?: m_dir
                    updateDirectory()
                    true
                }
            } else {
                false
            }
        }

        // Show directory chooser dialog
        dirsDialog.show()
    }

    ////////////////////////////////////////////////////////////////////////////////
    // chooseDirectory(String dir) - load directory chooser dialog for initial
    // input 'dir' directory
    ////////////////////////////////////////////////////////////////////////////////

    private fun createSubDir(newDir: String): Boolean {
        val newDirFile = File(newDir)
        if (!newDirFile.exists()) {
            Log.d("dir", "createSubDir: $newDir success")
            return newDirFile.mkdir()
        }

        Log.d("dir", "createSubDir: $newDir failure")
        return false
    }

    private fun getDirectories(dir: String): MutableList<String> {
        val dirs = ArrayList<String>()

        dirs.add("..")

        try {
            val dirFile = File(dir)
            if (!dirFile.exists() || !dirFile.isDirectory) {
                return dirs
            }

            for (file in dirFile.listFiles()!!) {
                if (file.isDirectory) {
                    dirs.add(file.name)
                }
            }
        } catch (ignored: Exception) {
        }

        Collections.sort(dirs) { a, b -> a.compareTo(b) }

        return dirs
    }

    private fun createDirectoryChooserDialog(
        title: String,
        listItems: List<String>,
        onClickListener: DialogInterface.OnClickListener
    ): AlertDialog.Builder {
        val dialogBuilder = AlertDialog.Builder(m_context)

        // Create custom view for AlertDialog title containing
        // current directory TextView and possible 'New folder' button.
        // Current directory TextView allows long directory path to be wrapped to multiple lines.
        val titleLayout = LinearLayout(m_context)
        titleLayout.orientation = LinearLayout.VERTICAL

        val titleView = TextView(m_context)
        m_titleView = titleView
        titleView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        titleView.setTextAppearance(m_context, android.R.style.TextAppearance_Large)
        titleView.setTextColor(ContextCompat.getColor(m_context, android.R.color.white))
        titleView.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
        titleView.text = title

        val newDirButton = Button(m_context)
        newDirButton.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        newDirButton.text = "New folder"
        newDirButton.setOnClickListener {
            val input = EditText(m_context)

            // Show new folder name input dialog
            AlertDialog.Builder(m_context)
                .setTitle("New folder name")
                .setView(input).setPositiveButton("OK") { _, _ ->
                    val newDir = input.text
                    val newDirName = newDir.toString()
                    // Create new directory
                    if (createSubDir("$m_dir/$newDirName")) {
                        // Navigate into the new directory
                        m_dir += "/$newDirName"
                        updateDirectory()
                    } else {
                        Toast.makeText(
                            m_context, "Failed to create '$newDirName' folder",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.setNegativeButton("Cancel", null).show()
        }

        if (!m_isNewFolderEnabled) {
            newDirButton.visibility = View.GONE
        }

        titleLayout.addView(titleView)
        titleLayout.addView(newDirButton)

        dialogBuilder.setCustomTitle(titleLayout)

        m_listAdapter = createListAdapter(listItems)

        dialogBuilder.setSingleChoiceItems(m_listAdapter, -1, onClickListener)
        dialogBuilder.setCancelable(false)

        return dialogBuilder
    }

    private fun updateDirectory() {
        m_subdirs!!.clear()
        m_subdirs!!.addAll(getDirectories(m_dir))
        m_titleView!!.text = m_dir

        m_listAdapter!!.notifyDataSetChanged()
    }

    private fun createListAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            m_context,
            android.R.layout.select_dialog_item, android.R.id.text1, items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)

                if (v is TextView) {
                    // Enable list item (directory) text wrapping
                    v.layoutParams.height = LayoutParams.WRAP_CONTENT
                    v.ellipsize = null
                }
                return v
            }
        }
    }

    //////////////////////////////////////////////////////
    // Callback interface for selected directory
    //////////////////////////////////////////////////////
    fun interface ChosenDirectoryListener {
        fun onChosenDir(chosenDir: String)
    }
}
