package com.msa.freedoom.ui.launch

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies buildLaunchArgs/buildModArgs stay byte-identical to the legacy
 * LaunchFragmentGZdoom.startGame() + ModSelectDialog.getResult() concatenation.
 */
class LaunchArgsTest {

    /** The exact string the legacy fragment produced: gameArgs + " " + editText + suffix. */
    private fun legacyArgs(gameArgs: String, editText: String, base: String): String {
        val args = "$gameArgs $editText"
        val saveDir = " -savedir $base/gzdoom_saves"
        return args + saveDir + " +set fluid_patchset " + "gzdoom.sf2" + " +set midi_dmxgus 0 "
    }

    private val base = "/storage/emulated/0/Android/data/com.msa.freedoom/files/Freedoom/config"

    @Test
    fun `iwad only`() {
        assertEquals(
            legacyArgs("-iwad freedoom1.wad", "", base),
            buildLaunchArgs("-iwad freedoom1.wad", "", "", base),
        )
    }

    @Test
    fun `iwad with extra args`() {
        assertEquals(
            legacyArgs("-iwad freedoom2.wad", "+map MAP05 -skill 4", base),
            buildLaunchArgs("-iwad freedoom2.wad", "", "+map MAP05 -skill 4", base),
        )
    }

    @Test
    fun `mods produce same string as legacy ModSelectDialog result`() {
        val mods = listOf(
            ModEntry("wads/SIGIL_v1_21.wad"),
            ModEntry("mods/patch.deh"),
            ModEntry("mods/fix.bex"),
            ModEntry("mods/texturepack"),
        )
        // Legacy getResult(): "-file wads/SIGIL_v1_21.wad -deh mods/patch.deh -deh mods/fix.bex -file mods/texturepack "
        val legacyModString =
            "-file wads/SIGIL_v1_21.wad -deh mods/patch.deh -deh mods/fix.bex -file mods/texturepack "
        assertEquals(legacyModString, buildModArgs(mods))
        assertEquals(
            legacyArgs("-iwad freedoom1.wad", legacyModString + "-fast", base),
            buildLaunchArgs("-iwad freedoom1.wad", buildModArgs(mods), "-fast", base),
        )
    }

    @Test
    fun `uppercase DEH is treated as -file, matching legacy case-sensitive check`() {
        assertEquals("-file mods/PATCH.DEH ", buildModArgs(listOf(ModEntry("mods/PATCH.DEH"))))
    }
}
