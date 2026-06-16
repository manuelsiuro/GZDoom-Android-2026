package com.opentouchgaming.androidcore.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.R
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.databinding.DialogImportExportBinding
import com.opentouchgaming.androidcore.databinding.ListItemUserFilesExportBinding
import com.opentouchgaming.saffal.FileSAF
import java.text.SimpleDateFormat
import java.util.Date

class ImportExportDialog
{
    lateinit var binding: DialogImportExportBinding
    lateinit var adaptor: ImportExportEntryAdapter
    lateinit var activity: Activity

    var export = false

    val backupPaths = ArrayList<String>()
    val exportList = ArrayList<String>()

    fun showDialog(
        activity: Activity, export: Boolean, entries: ArrayList<UserFilesDialog.UserFileEntry>, userFilesPath: String,
        callback: ((filename: String, folders: ArrayList<String>) -> Unit)
    )
    {
        this.activity = activity
        this.export = export

        binding = DialogImportExportBinding.inflate(activity.layoutInflater)

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)

        val width: Int = ((activity.resources.displayMetrics.widthPixels * 0.98).toInt())
        //val height: Int = ((activity.getResources().getDisplayMetrics().heightPixels * 0.90).toInt())
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.advancedButton.setOnClickListener {
            binding.recyclerView.visibility = View.VISIBLE
            binding.advancedButton.visibility = View.GONE
        }

        adaptor = ImportExportEntryAdapter(entries, userFilesPath)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        binding.recyclerView.adapter = adaptor

        for (e in entries)
        {
            e.selected = true
        }

        if (export)
        {
            backupPaths.addAll(AppInfo.getBackupPaths())

            binding.okButton.text = "Export"
            binding.okButton.setIconResource(R.drawable.ic_baseline_file_upload_24)

            val sdf = SimpleDateFormat("dd_M_yyyy")
            val currentDate = sdf.format(Date())

            val filename = getNewFilename(AppInfo.directory + "_" + currentDate + ".zip", backupPaths[0])

            binding.filenameEditText.setText(filename)
        }
        else
        {
            binding.okButton.text = "Import"
            binding.okButton.setIconResource(R.drawable.ic_baseline_file_download_24)

            binding.filenameEditText.visibility = View.GONE

            var backFileList = Utils.listFiles(AppInfo.getBackupPaths())
            if (backFileList.size > 0)
            {
                val filenames = backFileList.sortedByDescending { it.lastModified() }.map { it.absolutePath }
                backupPaths.addAll(filenames)
            }
            else
            {
                binding.okButton.isEnabled = false
            }
        }

        val customAdapter = CustomSpinnerAdapter(activity, backupPaths)
        binding.savePathSpinner.adapter = customAdapter

        binding.okButton.setOnClickListener {
            exportList.clear()
            for (entry in entries)
            {
                if (entry.selected) exportList.add(entry.description.path)
            }

            if (export)
            {
                var filename = binding.filenameEditText.text.toString()
                if (filename != null)
                {
                    if (filename.length > 1)
                    {
                        if (!filename.endsWith(".zip")) filename = "$filename.zip"
                        callback.invoke(backupPaths[binding.savePathSpinner.selectedItemPosition] + "/" + filename, exportList)
                        dialog.dismiss()
                    }
                }
            }
            else
            {
                if (binding.savePathSpinner.selectedItemPosition > -1)
                {
                    var filename = backupPaths[binding.savePathSpinner.selectedItemPosition]
                    if (filename != null)
                    {
                        callback.invoke(backupPaths[binding.savePathSpinner.selectedItemPosition], exportList)
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    fun getNewFilename(filename: String, directory: String): String
    {
        var file = FileSAF("$directory/$filename")
        var newFilename = filename
        var i = 1

        while (file.exists())
        {
            val extension = FileSAF(filename).extension
            val basename = filename.substringBeforeLast(".")
            newFilename = "$basename(${i}).$extension"
            file = FileSAF("$directory/$newFilename")
            i++
        }

        return newFilename
    }

    inner class CustomSpinnerAdapter(context: Context, private val itemList: List<String>) : ArrayAdapter<String>(context, 0, itemList)
    {
        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
        {
            val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_end, parent, false)
            val textView = view.findViewById<TextView>(R.id.path)
            val image = view.findViewById<ImageView>(R.id.image)
            val userPath = AppInfo.getDisplayPathAndImage(itemList[position])
            if (export) textView.text = userPath.first + "/"
            else textView.text = userPath.first
            image.setImageResource(userPath.second!!)
            return view
        }

        @SuppressLint("SetTextI18n")
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View
        {
            val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_end, parent, false)
            val textView = view.findViewById<TextView>(R.id.path)
            val image = view.findViewById<ImageView>(R.id.image)
            val userPath = AppInfo.getDisplayPathAndImage(itemList[position])
            if (export) textView.text = userPath.first + "/"
            else textView.text = userPath.first
            image.setImageResource(userPath.second!!)
            return view
        }
    }


    inner class ImportExportEntryAdapter(val entries: ArrayList<UserFilesDialog.UserFileEntry>, val userFilesPath: String) :
        RecyclerView.Adapter<ImportExportEntryAdapter.ViewHolder>()
    {
        inner class ViewHolder(view: View, val binding: ListItemUserFilesExportBinding) : RecyclerView.ViewHolder(view)

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder
        {
            val binding = ListItemUserFilesExportBinding.inflate(LayoutInflater.from(viewGroup.context))
            return ViewHolder(binding.root, binding)
        }

        // Replace the contents of a view (invoked by the layout manager)
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
        {
            viewHolder.binding.engineIconImageView.setImageResource(entries[position].description.icon)
            if (entries[position].description.version.isNotEmpty()) viewHolder.binding.engineNameTextView.text =
                entries[position].description.name + " (" + entries[position].description.version + ")"
            else viewHolder.binding.engineNameTextView.text = entries[position].description.name

            //viewHolder.binding.engineVersionTextView.text = entries[position].description.version

            // Clear to stop it triggering when recycling views
            viewHolder.binding.enableCheckBox.setOnCheckedChangeListener(null)

            if (export)
            {
                if (entries[position].lastModified != 0L)
                {
                    viewHolder.binding.engineSizeTextView.text = Utils.humanReadableByteCount(entries[position].totalSize, false)
                    viewHolder.binding.enableCheckBox.isEnabled = true
                    viewHolder.binding.enableCheckBox.isChecked = entries[position].selected
                }
                else
                {
                    //   viewHolder.binding.engineDetailsTextView.text = "No files"
                    viewHolder.binding.engineSizeTextView.text = "0"
                    viewHolder.binding.enableCheckBox.isEnabled = false
                    viewHolder.binding.enableCheckBox.isChecked = false
                }
            }
            else // import
            {
                viewHolder.binding.engineSizeTextView.text = ""
                viewHolder.binding.enableCheckBox.isEnabled = true
                viewHolder.binding.enableCheckBox.isChecked = entries[position].selected
            }

            viewHolder.binding.enableCheckBox.setOnCheckedChangeListener { compoundButton, b ->
                entries[position].selected = b
                adaptor.notifyDataSetChanged()
            }

            viewHolder.binding.enginePathTextView.text = entries[position].description.path
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = entries.size
    }
}