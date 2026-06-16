package com.opentouchgaming.androidcore.ui.widgets

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.Window
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.databinding.WidgetViewDeleteDataBinding
import com.opentouchgaming.saffal.FileSAF

class DeleteDataWidget(
    val context: Context, view: View, messageSettings: String, pathsSettings: Array<String>, fileTypesSettings: Array<String>, messageSaves: String,
    pathsSaves: Array<String>, fileTypesSaves: Array<String>
)
{
    private var binding = WidgetViewDeleteDataBinding.bind(view)

    init
    {
        binding.deleteConfigButton.setOnClickListener {

            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage(messageSettings)
            dialogBuilder.setPositiveButton("OK") { dialog, which ->
                deleteFiles(pathsSettings, fileTypesSettings)
            }
            val dialog = dialogBuilder.create()
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.show()
        }

        binding.deleteSavesButton.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage(messageSaves)
            dialogBuilder.setPositiveButton("OK") { dialog, which ->
                deleteFiles(pathsSaves, fileTypesSaves)
            }
            val dialog = dialogBuilder.create()
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.show()
        }
    }

    fun deleteFiles(paths: Array<String>, fileTypes: Array<String>)
    {
        val files = ArrayList<String>()
        for (p in paths)
        {
            for (type in fileTypes)
            {
                Utils.findFiles(FileSAF(AppInfo.getUserFiles() + p), type, files)
            }
        }

        for (f in files)
        {
            FileSAF(f).delete()
        }
    }
}