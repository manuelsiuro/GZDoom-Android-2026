package com.msa.freedoom.ui.launch

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.msa.freedoom.AppSettings

/**
 * A saved launch loadout: an IWAD plus an ordered list of add-ons (load order matters —
 * gameplay mods, then maps, then HUD/texture packs) and any extra args. Profiles are
 * keyed by IWAD so the launcher only offers the ones relevant to the selected game.
 */
@Serializable
data class LaunchProfile(
    val name: String,
    val iwadName: String,
    /** Add-on paths relative to the game dir, in load order. */
    val modRelPaths: List<String> = emptyList(),
    val extraArgs: String = "",
)

/**
 * Persists [LaunchProfile]s as a single JSON blob in the OPTIONS prefs. Kept tiny and
 * dependency-free (reuses the kotlinx.serialization already pulled in for the idgames
 * client) so it's unit-testable without Android.
 */
object ProfileStore {
    private const val KEY = "launch_profiles"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(ctx: Context): List<LaunchProfile> {
        val raw = AppSettings.getStringOption(ctx, KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<LaunchProfile>>(raw) }.getOrDefault(emptyList())
    }

    /** All profiles whose IWAD matches [iwadName] (the only ones relevant to that game). */
    fun forIwad(ctx: Context, iwadName: String): List<LaunchProfile> =
        load(ctx).filter { it.iwadName == iwadName }.sortedBy { it.name.lowercase() }

    /** Saves [profile], replacing any existing one with the same (name, iwad). */
    fun save(ctx: Context, profile: LaunchProfile) {
        val updated = load(ctx).filterNot {
            it.name.equals(profile.name, ignoreCase = true) && it.iwadName == profile.iwadName
        } + profile
        persist(ctx, updated)
    }

    fun delete(ctx: Context, profile: LaunchProfile) {
        persist(ctx, load(ctx).filterNot {
            it.name.equals(profile.name, ignoreCase = true) && it.iwadName == profile.iwadName
        })
    }

    private fun persist(ctx: Context, profiles: List<LaunchProfile>) {
        AppSettings.setStringOption(ctx, KEY, json.encodeToString(profiles))
    }
}
