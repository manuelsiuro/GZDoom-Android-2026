package com.msa.freedoom.ui.launch

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the on-disk shape of [LaunchProfile]. ProfileStore persists a JSON list of
 * these in prefs; a field rename/removal would silently drop saved profiles, so the
 * round-trip and load order are pinned here.
 */
class LaunchProfileTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `round-trips a list of profiles preserving mod load order`() {
        val profiles = listOf(
            LaunchProfile(
                name = "Brutal + Maps",
                iwadName = "freedoom2.wad",
                modRelPaths = listOf("mods/brutal.pk3", "wads/maps.wad", "mods/hud.pk3"),
                extraArgs = "-fast",
            ),
            LaunchProfile(name = "Vanilla", iwadName = "freedoom1.wad"),
        )
        val decoded = json.decodeFromString<List<LaunchProfile>>(json.encodeToString(profiles))
        assertEquals(profiles, decoded)
        // Load order is significant and must survive serialization unchanged.
        assertEquals(
            listOf("mods/brutal.pk3", "wads/maps.wad", "mods/hud.pk3"),
            decoded[0].modRelPaths,
        )
    }

    @Test
    fun `tolerates unknown future fields`() {
        val raw = """[{"name":"P","iwadName":"freedoom1.wad","modRelPaths":[],"extraArgs":"","futureField":42}]"""
        val decoded = json.decodeFromString<List<LaunchProfile>>(raw)
        assertEquals("P", decoded.single().name)
    }
}
