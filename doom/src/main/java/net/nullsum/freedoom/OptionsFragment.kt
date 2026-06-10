package net.nullsum.freedoom

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.io.File
import java.io.IOException

class OptionsFragment : Fragment() {
    private val LOG = "OptionsFragment"

    private var basePathTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mainView = inflater.inflate(R.layout.fragment_options, null)
        val resSpinnder = mainView.findViewById<Spinner>(R.id.resolution_div_spinner)

        val list = listOf("1", "2", "3", "4", "5", "6", "7", "8")

        val dataAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_item, list)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        resSpinnder.adapter = dataAdapter
        resSpinnder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                AppSettings.setIntOption(requireActivity(), "gzdoom_res_div", position + 1)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        val selected = AppSettings.getIntOption(requireActivity(), "gzdoom_res_div", 1)
        resSpinnder.setSelection(selected - 1)

        basePathTextView = mainView.findViewById(R.id.base_path_textview)

        basePathTextView?.text = AppSettings.freedoomBaseDir

        val chooseDir = mainView.findViewById<Button>(R.id.choose_base_button)
        chooseDir.setOnClickListener {
            val directoryChooserDialog =
                DirectoryChooserDialog(requireActivity()) { dir -> updateBaseDir(dir) }

            directoryChooserDialog.chooseDirectory(AppSettings.freedoomBaseDir)
        }

        val resetDir = mainView.findViewById<Button>(R.id.reset_base_button)
        resetDir.setOnClickListener {
            AppSettings.resetBaseDir(requireActivity())
            updateBaseDir(AppSettings.freedoomBaseDir)
        }

        val sdcardDir = mainView.findViewById<Button>(R.id.sdcard_base_button)
        sdcardDir.setOnClickListener {
            val files = requireActivity().getExternalFilesDirs(null)

            if (files.size < 2 || files[1] == null) {
                showError("Can not find an external SD Card, is the card inserted?")
                return@setOnClickListener
            }

            val path = files[1].toString()

            val dialogBuilder = AlertDialog.Builder(activity)
            dialogBuilder.setTitle("WARNING")
            dialogBuilder.setMessage(
                "This will use the special location on the external SD Card which can be written to by this app, Android will DELETE this"
                        + " area when you uninstall the app and you will LOSE YOUR SAVEGAMES and game data!"
            )
            dialogBuilder.setPositiveButton("OK") { _, _ -> updateBaseDir(path) }
            dialogBuilder.setNegativeButton("Cancel") { _, _ -> }

            val errdialog = dialogBuilder.create()
            errdialog.show()
        }

        return mainView
    }

    private fun updateBaseDir(dir: String?) {
        val fdir = File(dir.orEmpty())

        if (!fdir.isDirectory) {
            showError("$dir is not a directory")
            return
        }

        if (!fdir.canWrite()) {
            showError("$dir is not a writable")
            return
        }

        //Test CAN actually write, the above canWrite can pass on KitKat SD cards WTF GOOGLE
        val testWrite = File(dir, "test_write")
        try {
            testWrite.createNewFile()
            if (!testWrite.exists()) {
                showError("$dir is not a writable")
                return
            }
        } catch (e: IOException) {
            showError("$dir is not a writable")
            return
        }
        testWrite.delete()

        if (dir!!.contains(" ")) {
            showError("$dir must not contain any spaces")
            return
        }

        AppSettings.freedoomBaseDir = dir
        AppSettings.setStringOption(requireActivity(), "base_path", AppSettings.freedoomBaseDir!!)
        AppSettings.createDirectories(requireActivity())

        basePathTextView?.text = AppSettings.freedoomBaseDir
    }

    private fun showError(error: String) {
        val dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setTitle(error)
        dialogBuilder.setPositiveButton("OK") { _, _ -> }

        val errdialog = dialogBuilder.create()
        errdialog.show()
    }
}
