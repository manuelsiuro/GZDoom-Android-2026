package net.nullsum.freedoom.idgames

import android.content.Context
import kotlinx.serialization.Serializable

/**
 * A classic-game IWAD from assets/classic_wads.json, downloaded as a plain .wad
 * into the game dir root so it shows up in "Select game". Only freely-distributable
 * releases belong here (shareware episodes, demos, freeware) — never the commercial
 * IWADs that are still sold.
 */
@Serializable
data class ClassicEntry(
    val title: String,
    val filename: String,
    val size: Long,
    val description: String,
    val note: String = "",
) {
    val downloadUrl: String get() = ClassicCatalog.RAW_BASE + filename
}

@Serializable
data class ClassicCatalogFile(val version: Int, val entries: List<ClassicEntry>)

object ClassicCatalog {

    const val ASSET_NAME = "classic_wads.json"
    const val RAW_BASE = "https://raw.githubusercontent.com/Akbar30Bill/DOOM_wads/master/"

    fun load(context: Context): List<ClassicEntry> = try {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        idgamesJson.decodeFromString<ClassicCatalogFile>(text).entries
    } catch (e: Exception) {
        emptyList()
    }
}
