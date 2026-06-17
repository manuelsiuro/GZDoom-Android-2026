package com.msa.freedoom.idgames

import android.content.Context
import kotlinx.serialization.Serializable

/**
 * A hand-curated entry from assets/featured_wads.json. Only vanilla/Boom-compatible
 * classics belong here — the bundled engine is GZDoom 1.9-era and cannot run
 * modern ZScript releases.
 */
@Serializable
data class FeaturedEntry(
    val title: String,
    val author: String,
    val dir: String,
    val filename: String,
    val size: Long,
    val description: String,
    val idgamesId: Long = 0,
    val note: String = "",
) {
    val installName: String get() = filename.substringBeforeLast('.')
}

@Serializable
data class FeaturedCatalogFile(val version: Int, val entries: List<FeaturedEntry>)

object FeaturedCatalog {

    const val ASSET_NAME = "featured_wads.json"

    fun load(context: Context): List<FeaturedEntry> = try {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        idgamesJson.decodeFromString<FeaturedCatalogFile>(text).entries
    } catch (e: Exception) {
        emptyList()
    }
}
