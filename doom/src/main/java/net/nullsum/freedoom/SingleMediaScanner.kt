package net.nullsum.freedoom

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import java.io.File

/**
 * Triggers a media scan of a file (or every file in a directory). Uses the static
 * MediaScannerConnection.scanFile API rather than the deprecated MediaScannerConnectionClient.
 */
class SingleMediaScanner(context: Context, path: Boolean, file: String) {
    init {
        Log.d("SingleMediaScanner", "path = $path, f = $file")

        val paths: Array<String> = if (path) {
            File(file).listFiles()?.map { it.absolutePath }?.toTypedArray() ?: emptyArray()
        } else {
            arrayOf(file)
        }

        if (paths.isNotEmpty()) {
            MediaScannerConnection.scanFile(context, paths, null, null)
        }
    }
}
