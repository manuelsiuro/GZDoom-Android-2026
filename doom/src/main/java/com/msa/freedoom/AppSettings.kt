package com.msa.freedoom

import android.content.Context
import com.beloko.touchcontrols.TouchSettings
import java.io.File
import java.io.IOException

object AppSettings {

    @JvmField
    var freedoomBaseDir: String? = null

    @JvmField
    var musicBaseDir: String? = null

    @JvmField
    var graphicsDir = ""

    @JvmField
    var vibrate = false

    @JvmField
    var immersionMode = false

    @JvmField
    var ctx: Context? = null

    /**
     * Base directory for game data. On modern Android this is the app-specific external files
     * directory (no storage permission required, works under scoped storage).
     */
    @JvmStatic
    fun resetBaseDir(ctx: Context) {
        val external = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        freedoomBaseDir = "$external/Freedoom"
        setStringOption(ctx, "base_path", freedoomBaseDir!!)
    }

    @JvmStatic
    fun reloadSettings(ctx: Context) {
        AppSettings.ctx = ctx

        TouchSettings.reloadSettings(ctx)

        freedoomBaseDir = getStringOption(ctx, "base_path", null)
        if (freedoomBaseDir == null) {
            resetBaseDir(ctx)
        }

        var music = getStringOption(ctx, "music_path", null)
        if (music == null) {
            music = "$freedoomBaseDir/doom/Music"
            setStringOption(ctx, "music_path", music)
        }

        musicBaseDir = music

        graphicsDir = ctx.filesDir.toString() + "/"

        vibrate = getBoolOption(ctx, "vibrate", true)

        immersionMode = getBoolOption(ctx, "immersion_mode", true)
    }

    @JvmStatic
    fun getQuakeFullDir(): String = "$freedoomBaseDir/config"

    @JvmStatic
    fun createDirectories(ctx: Context) {
        var scan = false

        if (!File(getQuakeFullDir()).exists()) {
            scan = true
        }

        File(getQuakeFullDir()).mkdirs()

        // create folder gzdoom_dev where we store a modified ini
        File(getQuakeFullDir() + "/gzdoom_dev").mkdirs()

        // create a folder for user wads
        File(getQuakeFullDir() + "/wads").mkdirs()

        // create a folder for user mods (experimental)
        File(getQuakeFullDir() + "/mods").mkdirs()

        // This is totally stupid, need to do this so folder shows up in explorer!
        if (scan) {
            val f = File(getQuakeFullDir(), "temp_")
            try {
                f.createNewFile()
                SingleMediaScanner(ctx, false, f.absolutePath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            File(getQuakeFullDir(), "temp_").delete()
        }
    }

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

    /** baseDir-relative paths the user starred in the add-on picker (e.g. "wads/doom2/scythe.wad"). */
    @JvmStatic
    fun getFavoriteMods(ctx: Context): Set<String> =
        // Copy: the Set returned by SharedPreferences must not be mutated or stored.
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE)
            .getStringSet("mod_favorites", emptySet()).orEmpty().toSet()

    @JvmStatic
    fun setFavoriteMods(ctx: Context, value: Set<String>) {
        ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE)
            .edit().putStringSet("mod_favorites", value).apply()
    }
}
