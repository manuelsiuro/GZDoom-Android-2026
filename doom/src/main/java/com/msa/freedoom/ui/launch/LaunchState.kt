package com.msa.freedoom.ui.launch

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException
import java.util.ArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.msa.freedoom.AppSettings
import com.msa.freedoom.Game
import com.msa.freedoom.R
import com.msa.freedoom.Utils

/**
 * State holder for the launch tab. Durable state lives in [AppSettings] and the
 * args-history file; this class only mirrors it into Compose state.
 */
@Stable
class LaunchState(private val activity: Activity) {

    var games by mutableStateOf(listOf<WadEntry>())
        private set
    var selectedGame by mutableStateOf<WadEntry?>(null)
        private set
    val selectedMods = mutableStateListOf<ModEntry>()
    var extraArgs by mutableStateOf("")
    val argsHistory = mutableStateListOf<String>()
    var isPreparing by mutableStateOf(false)
        private set
    var isLaunching by mutableStateOf(false)
        private set
    var initialized by mutableStateOf(false)
        private set

    /** User-facing error from the unpack / launch paths; rendered as a dialog and dismissable. */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val baseDir: String get() = AppSettings.getQuakeFullDir()

    fun dismissError() { errorMessage = null }

    /** First-run unpack + initial scan. Replaces the legacy 10-second restart hack. */
    suspend fun initialize() {
        if (initialized) return
        // A failed unpack used to launch the engine into a native crash with missing data;
        // now it surfaces an error and leaves [initialized] false so a retry re-runs.
        val firstRun = !File(baseDir, "firstrun").exists()
        if (firstRun) isPreparing = true
        try {
            withContext(Dispatchers.IO) {
                AppSettings.createDirectories(activity)
                Utils.copyFreedoomFilesToSD(activity)
                if (firstRun) Utils.copyAsset(activity, "firstrun", baseDir)
            }
        } catch (e: IOException) {
            errorMessage = activity.getString(R.string.prepare_failed, e.message ?: e.toString())
            isPreparing = false
            return
        }
        isPreparing = false

        val history = ArrayList<String>()
        Utils.loadArgs(activity, history)
        argsHistory.clear()
        argsHistory.addAll(history)

        refreshGames()
        initialized = true
    }

    suspend fun refreshGames() {
        // Scan order (filesystem order) is what the legacy int "last_iwad" indexed into.
        val scanOrder = withContext(Dispatchers.IO) { scanIwads(baseDir) }
        games = scanOrder.sortedBy { it.file.lowercase() }

        val lastName = AppSettings.getStringOption(activity, "last_iwad_name", null)
        val lastIndex = AppSettings.getIntOption(activity, "last_iwad", -1)
        selectedGame = games.find { it.file == lastName }
            ?: scanOrder.getOrNull(lastIndex)
        // Drop selected mods whose files were removed since last time.
        selectedMods.removeAll { !File("$baseDir/${it.relPath}").exists() }
    }

    fun selectGame(entry: WadEntry) {
        selectedGame = entry
        AppSettings.setIntOption(activity, "last_iwad", games.indexOf(entry))
        AppSettings.setStringOption(activity, "last_iwad_name", entry.file)
    }

    /**
     * Clears the game selection, mod chips and the persisted last-IWAD prefs — the
     * escape hatch when a broken WAD would otherwise be auto-reselected every launch.
     * Keeps the extra-args field.
     */
    fun clearSelection() {
        selectedGame = null
        selectedMods.clear()
        AppSettings.setIntOption(activity, "last_iwad", -1)
        AppSettings.setStringOption(activity, "last_iwad_name", "")
    }

    /** Ports LaunchFragmentGZdoom.startGame() — the Intent contract must stay identical. */
    suspend fun launchGame() {
        val game = selectedGame ?: return
        if (isLaunching) return
        val base = baseDir

        isLaunching = true
        try {
            withContext(Dispatchers.IO) {
                // UZDoom 5.0 (__MOBILE__) loads its base data from ./res relative to
                // the game dir: res/uzdoom.pk3 (BASEWAD) + res/uzdoom_game_support.pk3
                // (OPTIONALWAD, also carries IWADINFO). See engine src/version.h.
                Utils.copyAsset(activity, "uzdoom.pk3", "$base/res")
                Utils.copyAsset(activity, "uzdoom_game_support.pk3", "$base/res")
                Utils.copyAsset(activity, "game_widescreen_gfx.pk3", base)
                // Autoload extras are searched via $PROGDIR (= the game dir).
                Utils.copyAsset(activity, "lights.pk3", base)
                Utils.copyAsset(activity, "brightmaps.pk3", base)
                // fluid_patchset gzdoom.sf2 resolves against the game dir; also
                // expose it in soundfonts/ for the engine's sound-font menu.
                Utils.copyAsset(activity, "gzdoom.sf2", base)
                Utils.copyAsset(activity, "gzdoom.sf2", "$base/soundfonts")
            }
        } catch (e: IOException) {
            errorMessage = activity.getString(R.string.launch_failed, e.message ?: e.toString())
            isLaunching = false
            return
        }
        isLaunching = false

        val trimmedArgs = extraArgs.trim()
        if (trimmedArgs.isNotEmpty()) {
            argsHistory.removeAll { it == trimmedArgs }
            while (argsHistory.size > 50) argsHistory.removeAt(argsHistory.size - 1)
            argsHistory.add(0, trimmedArgs)
            Utils.saveArgs(activity, ArrayList(argsHistory))
        }

        AppSettings.setStringOption(activity, "last_tab", "Freedoom")

        val intent = Intent(activity, Game::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            putExtra("res_div", AppSettings.getIntOption(activity, "gzdoom_res_div", 1))
            putExtra("game_path", base)
            putExtra("game", "com.msa.freedoom")
            putExtra("args", buildLaunchArgs(game.iwadArgs, buildModArgs(selectedMods), extraArgs, base))
        }
        activity.startActivity(intent)
    }
}
