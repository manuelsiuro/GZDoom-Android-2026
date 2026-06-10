package com.beloko.touchcontrols

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object TouchSettings {
    @JvmField
    var DEBUG = true

    @JvmField
    var gamePadControlsFile: String? = null

    @JvmField
    var gamePadEnabled = false

    @JvmField
    var hideTouchControls = false

    @JvmStatic
    fun reloadSettings(ctx: Context) {
        hideTouchControls = getBoolOption(ctx, "hide_touch_controls", true)
        gamePadEnabled = getBoolOption(ctx, "gamepad_enabled", true)

        gamePadControlsFile = ctx.filesDir.toString() + "/gamepadSettings.dat"
    }

    @JvmStatic
    fun getFloatOption(ctx: Context, name: String, def: Float): Float =
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).getFloat(name, def)

    @JvmStatic
    fun setFloatOption(ctx: Context, name: String, value: Float) {
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).edit().putFloat(name, value).apply()
    }

    @JvmStatic
    fun getBoolOption(ctx: Context, name: String, def: Boolean): Boolean =
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).getBoolean(name, def)

    @JvmStatic
    fun setBoolOption(ctx: Context, name: String, value: Boolean) {
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).edit().putBoolean(name, value).apply()
    }

    @JvmStatic
    fun getIntOption(ctx: Context, name: String, def: Int): Int =
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).getInt(name, def)

    @JvmStatic
    fun setIntOption(ctx: Context, name: String, value: Int) {
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).edit().putInt(name, value).apply()
    }

    @JvmStatic
    fun getLongOption(ctx: Context, name: String, def: Long): Long =
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).getLong(name, def)

    @JvmStatic
    fun setLongOption(ctx: Context, name: String, value: Long) {
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).edit().putLong(name, value).apply()
    }

    @JvmStatic
    fun getStringOption(ctx: Context, name: String, def: String?): String? =
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).getString(name, def)

    @JvmStatic
    fun setStringOption(ctx: Context, name: String, value: String) {
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).edit().putString(name, value).apply()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(input: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
        out.close()
    }

    @JvmStatic
    fun copyPNGAssets(ctx: Context, dir: String, prefix: String?) {
        val pre = prefix ?: ""

        val d = File(dir)
        if (!d.exists()) d.mkdirs()

        val assetManager = ctx.assets
        val files: Array<String> = try {
            assetManager.list("") ?: emptyArray()
        } catch (e: IOException) {
            Log.e("tag", "Failed to get asset file list.", e)
            emptyArray()
        }
        for (filename in files) {
            if (filename.endsWith("png") && filename.startsWith(pre)) {
                try {
                    val input = assetManager.open(filename)
                    val out = FileOutputStream(dir + "/" + filename.substring(pre.length))
                    copyFile(input, out)
                    input.close()
                    out.flush()
                    out.close()
                } catch (e: IOException) {
                    Log.e("tag", "Failed to copy asset file: $filename", e)
                }
            }
        }
    }
}
