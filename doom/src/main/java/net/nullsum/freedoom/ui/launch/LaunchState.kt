package net.nullsum.freedoom.ui.launch

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.ArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.nullsum.freedoom.AppSettings
import net.nullsum.freedoom.Game
import net.nullsum.freedoom.Utils

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
    var initialized by mutableStateOf(false)
        private set

    val baseDir: String get() = AppSettings.getQuakeFullDir()

    // Engine/bundled files that must not be offered as IWADs (same list as the legacy fragment).
    private val excludedFiles = setOf(
        "prboom-plus.wad", "gzdoom.pk3", "gzdoom_dev.pk3",
        "lights_dt.pk3", "brightmaps_dt.pk3", "lights.pk3", "brightmaps.pk3",
    )

    /** First-run unpack + initial scan. Replaces the legacy 10-second restart hack. */
    suspend fun initialize() {
        if (initialized) return
        withContext(Dispatchers.IO) {
            AppSettings.createDirectories(activity)
            val marker = File(baseDir, "firstrun")
            if (!marker.exists()) isPreparing = true
            Utils.copyFreedoomFilesToSD(activity)
            if (!marker.exists()) Utils.copyAsset(activity, "firstrun", baseDir)
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
        val scanOrder = withContext(Dispatchers.IO) {
            File(baseDir).listFiles().orEmpty()
                .filter { !it.isDirectory }
                .filter { f ->
                    val name = f.name.lowercase()
                    (name.endsWith(".wad") || name.endsWith(".pk3") || name.endsWith(".pk7")) &&
                        name !in excludedFiles
                }
                .map { WadEntry(it.name, it.length()) }
        }
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

    /** Ports LaunchFragmentGZdoom.startGame() — the Intent contract must stay identical. */
    suspend fun launchGame() {
        val game = selectedGame ?: return
        val base = baseDir

        withContext(Dispatchers.IO) {
            Utils.copyAsset(activity, "gzdoom.pk3", base)
            Utils.copyAsset(activity, "gzdoom.sf2", base)
        }

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
            putExtra("game", "net.nullsum.freedoom")
            putExtra("args", buildLaunchArgs(game.iwadArgs, buildModArgs(selectedMods), extraArgs, base))
        }
        activity.startActivity(intent)
    }
}
