package com.opentouchgaming.androidcore

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.util.Log
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class FileMover
{
    fun doMove(activity: Activity, destination: String, files: ArrayList<Pair<String, String>>): Boolean
    {
        var filesFiltered = fileFiles(files)

        // No files to move
        if (filesFiltered.size == 0) return false


        var dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_move_files)

        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        var text = dialog.findViewById<TextView>(R.id.title)

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)

        progressBar.max = filesFiltered.size
        dialog.show()


        GlobalScope.launch {

            for (file in filesFiltered)
            {
                // Move (rename file)
                var fileReal = File(file.first)
                var fileName = fileReal.name
                if (file.second != null) fileName = file.second

                if (!fileReal.renameTo(File(destination + "/" + fileName)))
                {
                    Log.e("FileMover", "Failed to move: " + fileReal.absolutePath)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        try
                        {
                            Files.copy(Paths.get(file.first), Paths.get(destination + "/" + fileName))
                        }
                        catch (ex: Exception)
                        {

                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.progress++
                }
            }

            withContext(Dispatchers.Main) {
                dialog.dismiss()
            }
        }

        return true
    }


    fun fileFiles(files: ArrayList<Pair<String, String>>): ArrayList<Pair<String, String>>
    {
        var filesFiltered = ArrayList<Pair<String, String>>()

        for (file in files)
        {
            if (File(file.first).exists())
            {
                filesFiltered.add(file)
            }
        }

        return filesFiltered
    }
}