package com.opentouchgaming.androidcore.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.Utils
import com.opentouchgaming.androidcore.databinding.DialogUserFilesManagerBinding
import com.opentouchgaming.androidcore.databinding.ListItemUserFilesEntryBinding
import com.opentouchgaming.saffal.FileSAF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ocpsoft.prettytime.PrettyTime
import java.io.File
import java.io.InputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class UserFilesDialog
{
    lateinit var binding: DialogUserFilesManagerBinding

    data class UserFileEntryDescription(val name: String = "", val version: String = "", val icon: Int = 0, val path: String = "")

    class UserFileEntry(val description: UserFileEntryDescription)
    {
        val files: ArrayList<String> = ArrayList()
        var totalSize = 0L
        var lastModified = 0L
        var selected = true
    }

    enum class RunningState
    {
        READY, SCANNING, DELETING, EXPORTING, IMPORTING
    }

    var runningState = RunningState.READY

    var totalSize = 0L

    val userFileEntries: ArrayList<UserFileEntry> = ArrayList()

    val userFilesPath: String = AppInfo.getUserFiles()

    lateinit var adaptor: UserFileEntryAdapter

    lateinit var act: Activity

    var message = ""

    fun showDialog(activity: Activity, entries: Array<UserFileEntryDescription>)
    {
        act = activity

        binding = DialogUserFilesManagerBinding.inflate(activity.layoutInflater)

        val dialog = FullscreenDialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)

        Utils.setInsets(activity, dialog)

        val userPath = AppInfo.getDisplayPathAndImage(AppInfo.getUserFiles())
        binding.userDirTextview.text = userPath.first
        binding.userDirImage.setImageResource(userPath.second!!)

        adaptor = UserFileEntryAdapter(userFileEntries, userFilesPath)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        binding.recyclerView.adapter = adaptor

        for (description in entries)
        {
            userFileEntries.add(UserFileEntry(description))
        }

        binding.importButton.setOnClickListener {
            val d = ImportExportDialog()
            d.showDialog(activity, false, userFileEntries, userFilesPath) { filename, folders ->
                val file = FileSAF(filename)
                if (file.exists())
                {
                    val unzipper = ZipExtractor(file.inputStream, "$userFilesPath", folders)
                    import(unzipper)
                }
            }
        }

        binding.exportButton.setOnClickListener {
            val d = ImportExportDialog()
            d.showDialog(activity, true, userFileEntries, userFilesPath) { filename, folders ->
                val zipper = FolderZipper(userFilesPath, folders, filename)
                export(zipper)
            }
        }

        dialog.show()
        scanFolders()
    }

    fun traverseDirectory(directory: FileSAF, entry: UserFileEntry)
    {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory)
            {
                traverseDirectory(file, entry)
            }
            else
            {
                println(file.absolutePath)
                entry.lastModified = file.lastModified()
                entry.files.add(file.absolutePath)
                entry.totalSize += file.length()
            }
        }
    }

    fun scanFolders()
    {
        runningState = RunningState.SCANNING

        totalSize = 0

        GlobalScope.launch {
            for (entry in userFileEntries)
            {
                entry.totalSize = 0
                entry.lastModified = 0L
                entry.files.clear()

                val path = userFilesPath + "/" + entry.description.path

                traverseDirectory(FileSAF(path), entry)

                withContext(Dispatchers.Main) {
                    updateUI()
                }

                totalSize += entry.totalSize
            }

            withContext(Dispatchers.Main) {
                runningState = RunningState.READY
                updateUI()
            }
        }
    }

    fun deleteEntry(entry: UserFileEntry)
    {
        runningState = RunningState.DELETING
        updateUI()

        GlobalScope.launch {

            val path = userFilesPath + "/" + entry.description.path
            val file = FileSAF(path)

            if (file.isRealFile) file.deleteRecursively()
            else // SAF seems to delete whole folder
                file.delete()

            withContext(Dispatchers.Main) {
                runningState = RunningState.READY
                // Rescan
                scanFolders()
            }
        }
    }

    fun export(zipper: FolderZipper)
    {
        runningState = RunningState.EXPORTING
        updateUI()

        GlobalScope.launch {
            zipper.zip()
            withContext(Dispatchers.Main) {
                runningState = RunningState.READY
                // Rescan
                scanFolders()
            }
        }
    }

    fun import(zipper: ZipExtractor)
    {
        runningState = RunningState.IMPORTING
        updateUI()

        GlobalScope.launch {
            zipper.extract()
            withContext(Dispatchers.Main) {
                runningState = RunningState.READY
                // Rescan
                scanFolders()
            }
        }
    }

    class FolderZipper(private val topLevelFolderPath: String, private val foldersToZip: ArrayList<String>, private val zipFilePath: String)
    {
        fun zip()
        {
            val zipFile = FileSAF(zipFilePath)

            val zipFileParent = FileSAF(zipFile.parent)
            zipFileParent.mkdirs()

            if (!zipFile.exists())
            {
                zipFile.createNewFile()
            }

            val zipOut = ZipOutputStream(zipFile.getOutputStream())

            foldersToZip.forEach { folderName ->
                var folder: File
                folder = if (folderName.startsWith("/")) FileSAF(folderName)
                else FileSAF("$topLevelFolderPath/$folderName")

                if (folder.exists())
                {
                    if (folder.isDirectory)
                    {
                        addFolderToZip(zipOut, folder, folderName)
                    }
                    else
                    {
                        println("$folderName is not a directory. Skipping.")
                    }
                }
                else
                {
                    println("$folderName does not exist. Skipping.")
                }
            }

            zipOut.close()
            println("Folder(s) zipped successfully to $zipFilePath")
        }

        private fun addFolderToZip(zipOut: ZipOutputStream, folder: FileSAF, parentFolder: String)
        {
            val fileList = folder.listFiles()
            for (file in fileList)
            {
                if (file.isDirectory)
                {
                    addFolderToZip(zipOut, file, "$parentFolder/${file.name}")
                }
                else
                {
                    try
                    {
                        val entryPath = "$parentFolder/${file.name}"
                        zipOut.putNextEntry(ZipEntry(entryPath))
                        val input = file.inputStream
                        input.copyTo(zipOut)
                        input.close()
                        zipOut.closeEntry()
                    }
                    catch (e: Exception)
                    {
                        println("Error sipping file: " + file.absolutePath)
                    }
                }
            }
        }
    }

    class ZipExtractor(private val zipInputStream: InputStream, private val extractToFolder: String, private val topLevelFolders: List<String>)
    {
        fun extract()
        {
            val zipStream = ZipInputStream(zipInputStream)

            // Loop through each entry in the ZIP stream
            var entry: ZipEntry? = zipStream.nextEntry
            while (entry != null)
            {
                // Check if the entry's name starts with any of the top-level folder names
                val matchingFolder = topLevelFolders.find { entry!!.name.startsWith("$it/") }
                if (matchingFolder != null)
                {
                    // Determine the relative path of the entry within the matching folder
                    val relativePath = entry.name.removePrefix("$matchingFolder/")

                    // Create any missing directories in the target folder hierarchy
                    val targetFile = FileSAF("$extractToFolder/$matchingFolder/$relativePath")
                    targetFile.parentFile.mkdirs()

                    // Extract the entry to the target folder with the relative path
                    if (!entry.isDirectory)
                    {
                        println("Extracting: " + targetFile.absolutePath)
                        targetFile.createNewFile()

                        targetFile.outputStream.use { output ->
                            zipStream.copyTo(output)
                        }
                    }
                }

                // Move to the next entry in the ZIP stream
                entry = zipStream.nextEntry
            }

            // Close the ZIP stream
            zipStream.close()
        }
    }

    fun showAlert(activity: Activity, title: String, message: String, function: () -> (Unit))
    {
        val dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setTitle(title)
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("OK") { _: DialogInterface, _: Int ->
            function.invoke()
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    fun updateUI()
    {
        adaptor.notifyDataSetChanged()
        binding.totalSizeTextView.text = Utils.humanReadableByteCount(totalSize, false)

        if (runningState != RunningState.READY)
        {
            binding.statusTextView.setTextColor(Color.parseColor("#FF9f0f0f"))
            if (runningState == RunningState.EXPORTING)
            {
                binding.statusTextView.text = "Exporting..."
            }
            else if (runningState == RunningState.IMPORTING)
            {
                binding.statusTextView.text = "Importing..."
            }
            binding.importButton.isEnabled = false
            binding.exportButton.isEnabled = false
        }
        else
        {
            binding.statusTextView.setTextColor(Color.parseColor("#1c7a07"))
            binding.statusTextView.text = "Ready"
            binding.importButton.isEnabled = true
            binding.exportButton.isEnabled = true
        }

        if (message.length > 0)
        {
            binding.statusTextView.text = message
            message = ""
        }
    }

    inner class UserFileEntryAdapter(val entries: ArrayList<UserFileEntry>, val userFilesPath: String) : RecyclerView.Adapter<UserFileEntryAdapter.ViewHolder>()
    {
        inner class ViewHolder(view: View, val binding: ListItemUserFilesEntryBinding) : RecyclerView.ViewHolder(view)

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder
        {
            val binding = ListItemUserFilesEntryBinding.inflate(LayoutInflater.from(viewGroup.context))
            return ViewHolder(binding.root, binding)
        }

        // Replace the contents of a view (invoked by the layout manager)
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int)
        {
            viewHolder.binding.engineIconImageView.setImageResource((entries[position].description.icon))
            viewHolder.binding.engineNameTextView.text = entries[position].description.name
            viewHolder.binding.engineVersionTextView.text = entries[position].description.version

            if (entries[position].lastModified != 0L)
            {
                val p = PrettyTime()
                viewHolder.binding.engineDetailsTextView.text = "Last modified - ${p.format(Date(entries[position].lastModified))}"
                val sizeInMegabytes = entries[position].totalSize.toDouble() / (1024 * 1024)
                val size = "%.2f MB".format(sizeInMegabytes.coerceAtLeast(0.01))
                viewHolder.binding.engineSizeTextView.text = size
            }
            else
            {
                viewHolder.binding.engineDetailsTextView.text = "No files"
                viewHolder.binding.engineSizeTextView.text = ""
            }

            val userPath = AppInfo.getDisplayPathAndImage("..user_files/" + entries[position].description.path)
            viewHolder.binding.enginePathTextView.text = userPath.first
            viewHolder.binding.enginePathImageView.setImageResource(userPath.second)

            if (runningState == RunningState.READY && (entries[position].lastModified != 0L))
            {
                viewHolder.binding.deleteButton.visibility = View.VISIBLE

                viewHolder.binding.deleteButton.setOnClickListener {
                    showAlert(
                        act, "DELETE FILES",
                        "Delete all user files for: " + entries[position].description.name + " (" + entries[position].description.version + ")",
                    ) {
                        deleteEntry(entries[position])
                    }
                }
            }
            else
            {
                viewHolder.binding.deleteButton.visibility = View.INVISIBLE
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = entries.size
    }
}