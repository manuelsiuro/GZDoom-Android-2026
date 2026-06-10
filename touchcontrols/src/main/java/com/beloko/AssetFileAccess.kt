package com.beloko

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.util.Log
import java.io.IOException
import java.util.ArrayList

/**
 * Created by Emile on 26/03/2016.
 */
class AssetFileAccess {
    companion object {
        private const val LOG = "AssetFileAccess"

        @JvmField
        var ctx: Context? = null

        @JvmField
        val openFiles = ArrayList<AssetFileDescriptor>()

        @JvmStatic
        external fun setAssetManager(mng: AssetManager)

        @JvmStatic
        fun init(c: Context) {
            ctx = c
        }

        @JvmStatic
        fun fopen(filename: String, mode: String): Int {
            Log.d(LOG, "filename = $filename")

            return try {
                val fd = ctx!!.assets.openFd(filename)
                openFiles.add(fd)
                openFiles.indexOf(fd)
            } catch (e: IOException) {
                Log.e(LOG, "fopen: No file found with name: $filename")
                e.printStackTrace()
                -1
            }
        }

        @JvmStatic
        fun flen(handle: Int): Int {
            val fd = getFd(handle)
            return if (fd != null) {
                fd.length.toInt()
            } else {
                Log.e(LOG, "flen: No file found with handle: $handle")
                0
            }
        }

        private fun getFd(handle: Int): AssetFileDescriptor? {
            return if (handle - 1 < openFiles.size) {
                openFiles[handle - 1]
            } else {
                Log.e(LOG, "getFd: No file found with handle: $handle")
                null
            }
        }
    }
}
