@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package net.nullsum.freedoom

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.beloko.touchcontrols.ActionInput
import com.beloko.touchcontrols.ControlConfig
import com.beloko.touchcontrols.ControlConfig.Type
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.util.ArrayList
import kotlin.math.roundToInt

object Utils {
    private const val BUFFER_SIZE = 1024
    private const val LOG = "Utils"

    @JvmStatic
    fun copyFreedoomFilesToSD(responsibleActivity: Activity) {
        val fullBaseDir = AppSettings.getQuakeFullDir()
        val fullWadDir = "$fullBaseDir/wads"

        // Freedoom additions
        copyAsset(responsibleActivity, "freedoom1.wad", fullBaseDir)
        copyAsset(responsibleActivity, "freedoom2.wad", fullBaseDir)
        // Freedoom licence and credits
        copyAsset(responsibleActivity, "COPYING.txt", fullBaseDir)
        copyAsset(responsibleActivity, "CREDITS.txt", fullBaseDir)

        // Add Romero's Sigil addon wad
        copyAsset(responsibleActivity, "SIGIL_v1_21.wad", fullWadDir)
        // Credits
        copyAsset(responsibleActivity, "SIGIL_v1_21.txt", fullWadDir)

        // Add 10sector and 10sector2 wads, megawads with ultra small levels
        copyAsset(responsibleActivity, "10sector.wad", fullWadDir)
        copyAsset(responsibleActivity, "10sector.txt", fullWadDir)
        copyAsset(responsibleActivity, "10secto2.wad", fullWadDir)
        copyAsset(responsibleActivity, "10secto2.txt", fullWadDir)

        // copy a custom gzdoom iniFile to set midi device to fluidsynth
        val iniFileName = "zdoom.ini"
        val iniFolderName = "/gzdoom_dev"
        var tester = File("$fullBaseDir$iniFolderName/$iniFileName")
        if (!tester.exists()) {
            Log.d(LOG, "zdoom.ini file not present, copying custom one")
            copyAsset(responsibleActivity, iniFileName, fullBaseDir + iniFolderName)
        } else {
            Log.d(LOG, "zdoom.ini file is already present")
        }

        // copy over gzdoom mod/package files
        val fullModDir = "$fullBaseDir/mods"

        tester = File("$fullModDir/brightmaps.pk3")
        if (!tester.exists()) {
            copyAsset(responsibleActivity, "brightmaps.pk3", fullModDir)
        }

        tester = File("$fullModDir/lights.pk3")
        if (!tester.exists()) {
            copyAsset(responsibleActivity, "lights.pk3", fullModDir)
        }

        tester = File("$fullModDir/zdextra.pk3")
        if (!tester.exists()) {
            copyAsset(responsibleActivity, "zdextra.pk3", fullModDir)
        }
    }

    @Throws(IOException::class)
    private fun copyFile(input: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
        out.close()
    }

    @JvmStatic
    fun showDownloadDialog(act: Activity, title: String, KEY: String, directory: String, file: String) {
        val builder = AlertDialog.Builder(act)
        builder.setMessage(title)
            .setCancelable(true)
            .setPositiveButton("OK") { _, _ -> }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    @JvmStatic
    fun checkFiles(basePath: String, filesToCheck: Array<String>): String? {
        var files = File(basePath).listFiles()

        val filesNotFound = StringBuilder()

        if (files == null) files = arrayOf()

        for (f in files) {
            Log.d(LOG, "FILES: $f")
        }

        for (e in filesToCheck) {
            var found = false
            for (f in files) {
                if (f.toString().lowercase().endsWith(e.lowercase())) {
                    found = true
                }
            }
            if (!found) {
                Log.d(LOG, "Didnt find $e")
                filesNotFound.append(e).append("\n")
            }
        }

        return if (filesNotFound.toString().contentEquals("")) null else filesNotFound.toString()
    }

    @JvmStatic
    fun copyPNGAssets(ctx: Context, dir: String) {
        val prefix = ""

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
            if (filename.endsWith("png")) {
                try {
                    val input = assetManager.open(filename)
                    val out = FileOutputStream(dir + "/" + filename.substring(prefix.length))
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

    @JvmStatic
    fun createArgs(appArgs: String): Array<String> =
        appArgs.split(" ").filter { it.isNotEmpty() }.toTypedArray()

    @JvmStatic
    fun expand(v: View) {
        v.measure(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT)
        val targetHeight = v.measuredHeight

        v.layoutParams.height = 0
        v.visibility = View.VISIBLE
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height = if (interpolatedTime == 1f) {
                    LayoutParams.WRAP_CONTENT
                } else {
                    (targetHeight * interpolatedTime).toInt()
                }
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean = true
        }

        // 1dp/ms
        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toLong()
        v.startAnimation(a)
    }

    @JvmStatic
    fun collapse(v: View) {
        val initialHeight = v.measuredHeight

        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean = true
        }

        // 1dp/ms
        a.duration = (initialHeight / v.context.resources.displayMetrics.density).toLong()
        v.startAnimation(a)
    }

    @JvmStatic
    fun getLogCat(): String? {
        val logcatArgs = arrayOf("logcat", "-d", "-v", "time")

        val logcatProc = try {
            Runtime.getRuntime().exec(logcatArgs)
        } catch (e: IOException) {
            return null
        }

        var reader: BufferedReader? = null
        var response: String? = null
        try {
            val separator = System.getProperty("line.separator")
            val sb = StringBuilder()
            reader = BufferedReader(InputStreamReader(logcatProc.inputStream), BUFFER_SIZE)
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append(separator)
            }
            response = sb.toString()
        } catch (ignored: IOException) {
        } finally {
            try {
                reader?.close()
            } catch (ignored: IOException) {
            }
        }

        return response
    }

    @JvmStatic
    fun copyAsset(ctx: Context, file: String, destdir: String) {
        val assetManager = ctx.assets

        try {
            val input = assetManager.open(file)
            val out = FileOutputStream("$destdir/$file")
            copyFile(input, out)
            input.close()
            out.flush()
            out.close()
        } catch (e: IOException) {
            Log.e("tag", "Failed to copy asset file: $file")
            e.printStackTrace()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
            val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }

        return inSampleSize
    }

    @JvmStatic
    fun decodeSampledBitmapFromResource(res: Resources, resId: Int, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(res, resId, options)
    }

    @JvmStatic
    fun loadArgs(ctx: Context, args: ArrayList<String>) {
        val cacheDir = ctx.filesDir

        try {
            val input = ObjectInputStream(FileInputStream(File(cacheDir, "args_hist.dat")))
            @Suppress("UNCHECKED_CAST")
            val argsHistory = input.readObject() as ArrayList<String>
            args.clear()
            args.addAll(argsHistory)
            input.close()
            return
        } catch (ignored: IOException) {
        } catch (ignored: ClassNotFoundException) {
        }
        // failed load, load default
        args.clear()
    }

    @JvmStatic
    fun saveArgs(ctx: Context, args: ArrayList<String>) {
        val cacheDir = ctx.filesDir

        if (!cacheDir.exists()) cacheDir.mkdirs()

        try {
            val out = ObjectOutputStream(FileOutputStream(File(cacheDir, "args_hist.dat")))
            out.writeObject(args)
            out.close()
        } catch (ex: IOException) {
            Toast.makeText(ctx, "Error saving args History list: $ex", Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun setImmersionMode(act: Activity) {
        if (AppSettings.immersionMode) {
            val window = act.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @JvmStatic
    fun onWindowFocusChanged(act: Activity, hasFocus: Boolean) {
        if (AppSettings.immersionMode && hasFocus) {
            Handler(Looper.getMainLooper()).postDelayed({ setImmersionMode(act) }, 2000)
        }
    }

    @JvmStatic
    fun getGameGamepadConfig(res: Resources): ArrayList<ActionInput> {
        val actions = ArrayList<ActionInput>()

        actions.add(ActionInput("analog_look_pitch", res.getString(R.string.look_up_down_option), ControlConfig.ACTION_ANALOG_PITCH, Type.ANALOG))
        actions.add(ActionInput("analog_look_yaw", res.getString(R.string.look_left_right_option), ControlConfig.ACTION_ANALOG_YAW, Type.ANALOG))
        actions.add(ActionInput("analog_move_fwd", res.getString(R.string.forward_back_option), ControlConfig.ACTION_ANALOG_FWD, Type.ANALOG))
        actions.add(ActionInput("analog_move_strafe", res.getString(R.string.strafe_option), ControlConfig.ACTION_ANALOG_STRAFE, Type.ANALOG))
        actions.add(ActionInput("attack", res.getString(R.string.attack_option), ControlConfig.PORT_ACT_ATTACK, Type.BUTTON))
        actions.add(ActionInput("attack_alt", res.getString(R.string.attack_alt_option), ControlConfig.PORT_ACT_ALT_ATTACK, Type.BUTTON))
        actions.add(ActionInput("back", res.getString(R.string.move_back_option), ControlConfig.PORT_ACT_BACK, Type.BUTTON))
        actions.add(ActionInput("crouch", res.getString(R.string.crouch_option), ControlConfig.PORT_ACT_DOWN, Type.BUTTON))
        actions.add(ActionInput("custom_0", res.getString(R.string.custom_0_option), ControlConfig.PORT_ACT_CUSTOM_0, Type.BUTTON))
        actions.add(ActionInput("custom_1", res.getString(R.string.custom_1_option), ControlConfig.PORT_ACT_CUSTOM_1, Type.BUTTON))
        actions.add(ActionInput("custom_2", res.getString(R.string.custom_2_option), ControlConfig.PORT_ACT_CUSTOM_2, Type.BUTTON))
        actions.add(ActionInput("custom_3", res.getString(R.string.custom_3_option), ControlConfig.PORT_ACT_CUSTOM_3, Type.BUTTON))
        actions.add(ActionInput("custom_4", res.getString(R.string.custom_4_option), ControlConfig.PORT_ACT_CUSTOM_4, Type.BUTTON))
        actions.add(ActionInput("custom_5", res.getString(R.string.custom_5_option), ControlConfig.PORT_ACT_CUSTOM_5, Type.BUTTON))
        actions.add(ActionInput("fly_down", res.getString(R.string.fly_down_option), ControlConfig.PORT_ACT_FLY_DOWN, Type.BUTTON))
        actions.add(ActionInput("fly_up", res.getString(R.string.fly_up_option), ControlConfig.PORT_ACT_FLY_UP, Type.BUTTON))
        actions.add(ActionInput("fwd", res.getString(R.string.fwd_option), ControlConfig.PORT_ACT_FWD, Type.BUTTON))
        actions.add(ActionInput("inv_drop", res.getString(R.string.inv_drop_option), ControlConfig.PORT_ACT_INVDROP, Type.BUTTON))
        actions.add(ActionInput("inv_next", res.getString(R.string.inv_next_option), ControlConfig.PORT_ACT_INVNEXT, Type.BUTTON))
        actions.add(ActionInput("inv_prev", res.getString(R.string.inv_prev_option), ControlConfig.PORT_ACT_INVPREV, Type.BUTTON))
        actions.add(ActionInput("inv_use", res.getString(R.string.inv_use_option), ControlConfig.PORT_ACT_INVUSE, Type.BUTTON))
        actions.add(ActionInput("jump", res.getString(R.string.jump_option), ControlConfig.PORT_ACT_JUMP, Type.BUTTON))
        actions.add(ActionInput("left", res.getString(R.string.left_option), ControlConfig.PORT_ACT_MOVE_LEFT, Type.BUTTON))
        actions.add(ActionInput("look_left", res.getString(R.string.look_left_option), ControlConfig.PORT_ACT_LEFT, Type.BUTTON))
        actions.add(ActionInput("look_right", res.getString(R.string.look_right_option), ControlConfig.PORT_ACT_RIGHT, Type.BUTTON))
        actions.add(ActionInput("map_down", res.getString(R.string.map_down_option), ControlConfig.PORT_ACT_MAP_DOWN, Type.BUTTON))
        actions.add(ActionInput("map_left", res.getString(R.string.map_left_option), ControlConfig.PORT_ACT_MAP_LEFT, Type.BUTTON))
        actions.add(ActionInput("map_right", res.getString(R.string.map_right_option), ControlConfig.PORT_ACT_MAP_RIGHT, Type.BUTTON))
        actions.add(ActionInput("map_show", res.getString(R.string.map_show_option), ControlConfig.PORT_ACT_MAP, Type.BUTTON))
        actions.add(ActionInput("map_up", res.getString(R.string.map_up_option), ControlConfig.PORT_ACT_MAP_UP, Type.BUTTON))
        actions.add(ActionInput("map_zoomin", res.getString(R.string.map_zoomin_option), ControlConfig.PORT_ACT_MAP_ZOOM_IN, Type.BUTTON))
        actions.add(ActionInput("map_zoomout", res.getString(R.string.map_zoomout_option), ControlConfig.PORT_ACT_MAP_ZOOM_OUT, Type.BUTTON))
        actions.add(ActionInput("menu_back", res.getString(R.string.menu_back_option), ControlConfig.MENU_BACK, Type.MENU))
        actions.add(ActionInput("menu_down", res.getString(R.string.menu_down_option), ControlConfig.MENU_DOWN, Type.MENU))
        actions.add(ActionInput("menu_left", res.getString(R.string.menu_left_option), ControlConfig.MENU_LEFT, Type.MENU))
        actions.add(ActionInput("menu_right", res.getString(R.string.menu_right_option), ControlConfig.MENU_RIGHT, Type.MENU))
        actions.add(ActionInput("menu_select", res.getString(R.string.menu_select_option), ControlConfig.MENU_SELECT, Type.MENU))
        actions.add(ActionInput("menu_up", res.getString(R.string.menu_up_option), ControlConfig.MENU_UP, Type.MENU))
        actions.add(ActionInput("next_weapon", res.getString(R.string.next_weapon_option), ControlConfig.PORT_ACT_NEXT_WEP, Type.BUTTON))
        actions.add(ActionInput("prev_weapon", res.getString(R.string.prev_weapon_option), ControlConfig.PORT_ACT_PREV_WEP, Type.BUTTON))
        actions.add(ActionInput("quick_load", res.getString(R.string.quick_load_option), ControlConfig.PORT_ACT_QUICKLOAD, Type.BUTTON))
        actions.add(ActionInput("quick_save", res.getString(R.string.quick_save_option), ControlConfig.PORT_ACT_QUICKSAVE, Type.BUTTON))
        actions.add(ActionInput("right", res.getString(R.string.right_option), ControlConfig.PORT_ACT_MOVE_RIGHT, Type.BUTTON))
        actions.add(ActionInput("show_keys", res.getString(R.string.show_keys_option), ControlConfig.PORT_ACT_SHOW_KEYS, Type.BUTTON))
        actions.add(ActionInput("show_weap", res.getString(R.string.show_weap_option), ControlConfig.PORT_ACT_SHOW_WEAPONS, Type.BUTTON))
        actions.add(ActionInput("speed", res.getString(R.string.speed_option), ControlConfig.PORT_ACT_SPEED, Type.BUTTON))
        actions.add(ActionInput("strafe_on", res.getString(R.string.strafe_on_option), ControlConfig.PORT_ACT_STRAFE, Type.BUTTON))
        actions.add(ActionInput("use", res.getString(R.string.use_option), ControlConfig.PORT_ACT_USE, Type.BUTTON))

        return actions
    }
}
