package net.nullsum.freedoom.idgames

import android.content.Context
import kotlinx.serialization.Serializable

/**
 * A still-sold commercial game IWAD from assets/commercial_wads.json. These are NEVER
 * downloaded — they are copyrighted games id Software/Bethesda and Raven still sell.
 * The user imports their own .wad from a copy they bought (the same model GZDoom uses
 * on PC). [filename] is the canonical IWAD name the engine expects.
 */
@Serializable
data class CommercialEntry(
    val title: String,
    val filename: String,
    val description: String,
    val note: String = "",
)

@Serializable
data class CommercialCatalogFile(val version: Int, val entries: List<CommercialEntry>)

object CommercialCatalog {

    const val ASSET_NAME = "commercial_wads.json"

    fun load(context: Context): List<CommercialEntry> = try {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        idgamesJson.decodeFromString<CommercialCatalogFile>(text).entries
    } catch (e: Exception) {
        emptyList()
    }
}
