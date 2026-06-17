package com.msa.freedoom.ui.editor.launch

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Opens the Android share sheet to send a generated WAD ([file]) to another app (a friend, a
 * cloud drive, another Doom port, …). The file lives under the app-specific external files dir,
 * exposed through the manifest's FileProvider (`${applicationId}.fileprovider`, see file_paths.xml).
 */
fun shareWad(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        // clipData propagates the read grant to the share-sheet preview process too.
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share WAD"))
}
