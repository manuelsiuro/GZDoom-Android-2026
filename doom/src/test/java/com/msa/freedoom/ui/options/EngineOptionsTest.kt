package com.msa.freedoom.ui.options

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards [buildEngineCvarArgs]: deterministic ordering, Locale-independent float
 * formatting, and correct CVAR names. The string is concatenated into the engine
 * command line alongside the mod args (see LaunchState.launchGame), so drift here
 * silently changes engine behaviour.
 */
class EngineOptionsTest {

    @Test
    fun `defaults render the expected command line`() {
        assertEquals(
            "+fov 90 +set vid_gamma 1.00 +set crosshair 0 +set gl_lights 1 " +
                "+set vid_vsync 1 +set autosavecount 4 +set snd_mastervolume 1.00 " +
                "+set snd_musicvolume 1.00 +set snd_sfxvolume 1.00 +set vid_fps 0 ",
            buildEngineCvarArgs(EngineOptions.DEFAULT),
        )
    }

    @Test
    fun `custom values are rendered in order`() {
        val opts = EngineOptions(
            fov = 110,
            gamma = 1.5f,
            crosshair = 3,
            dynamicLights = false,
            vsync = false,
            autosaveCount = 0,
            masterVolume = 0.8f,
            musicVolume = 0.25f,
            sfxVolume = 0.5f,
            fpsHud = true,
        )
        assertEquals(
            "+fov 110 +set vid_gamma 1.50 +set crosshair 3 +set gl_lights 0 " +
                "+set vid_vsync 0 +set autosavecount 0 +set snd_mastervolume 0.80 " +
                "+set snd_musicvolume 0.25 +set snd_sfxvolume 0.50 +set vid_fps 1 ",
            buildEngineCvarArgs(opts),
        )
    }

    @Test
    fun `floats use a dot decimal regardless of locale`() {
        val prev = java.util.Locale.getDefault()
        try {
            // French locale uses a comma decimal separator; the engine only parses dots.
            java.util.Locale.setDefault(java.util.Locale.FRANCE)
            val out = buildEngineCvarArgs(EngineOptions.DEFAULT.copy(gamma = 2.0f))
            assertTrue(out, out.contains("+set vid_gamma 2.00 "))
            assertTrue(out, !out.contains(","))
        } finally {
            java.util.Locale.setDefault(prev)
        }
    }

    @Test
    fun `output ends with a trailing space for safe concatenation`() {
        assertTrue(buildEngineCvarArgs(EngineOptions.DEFAULT).endsWith(" "))
    }
}
