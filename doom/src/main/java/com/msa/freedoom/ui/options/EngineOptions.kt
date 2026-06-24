package com.msa.freedoom.ui.options

import android.content.Context
import java.util.Locale
import com.msa.freedoom.AppSettings

/**
 * User-facing engine settings that the launcher surfaces so players don't have to
 * dig through UZDoom's tiny in-game touch menus. Each field maps to a real engine
 * CVAR (or the `fov` console command); [buildEngineCvarArgs] renders them to the
 * `+set`/`+fov` portion of the command line.
 *
 * These values are launcher-authoritative: when the feature is enabled we re-pass
 * them every launch (the in-game menu is the override of last resort). The args are
 * composed in `LaunchState.launchGame()` — NOT in `buildLaunchArgs` — so the
 * byte-identical legacy command-line contract and its golden test stay untouched
 * (same pattern as the gore add-on).
 */
data class EngineOptions(
    /** Player field-of-view in degrees; emitted as the `fov` console command. */
    val fov: Int = 90,
    /** Display gamma/brightness (`vid_gamma`); 1.0 = engine default. */
    val gamma: Float = 1.0f,
    /** Crosshair style index (`crosshair`); 0 = none/off. */
    val crosshair: Int = 0,
    /** Dynamic lights (`gl_lights`). */
    val dynamicLights: Boolean = true,
    /** Vertical sync (`vid_vsync`). */
    val vsync: Boolean = true,
    /** Rotating autosave slot count (`autosavecount`). */
    val autosaveCount: Int = 4,
    /** Master volume (`snd_mastervolume`), 0.0–1.0. */
    val masterVolume: Float = 1.0f,
    /** Music volume (`snd_musicvolume`), 0.0–1.0. */
    val musicVolume: Float = 1.0f,
    /** Sound-effects volume (`snd_sfxvolume`), 0.0–1.0. */
    val sfxVolume: Float = 1.0f,
    /** On-screen FPS / frametime counter (`vid_fps`). */
    val fpsHud: Boolean = false,
) {
    companion object {
        val DEFAULT = EngineOptions()

        // Sensible UI bounds. The engine clamps internally, but keeping the sliders
        // in range avoids passing absurd values.
        val FOV_RANGE = 60..120
        val GAMMA_RANGE = 0.5f..3.0f
        val CROSSHAIR_RANGE = 0..9
        val AUTOSAVE_RANGE = 0..8

        // SharedPreferences keys (the "eo_" prefix namespaces them in the OPTIONS store).
        const val KEY_ENABLED = "enable_engine_options"
        private const val KEY_FOV = "eo_fov"
        private const val KEY_GAMMA = "eo_gamma"
        private const val KEY_CROSSHAIR = "eo_crosshair"
        private const val KEY_DYNLIGHTS = "eo_dynlights"
        private const val KEY_VSYNC = "eo_vsync"
        private const val KEY_AUTOSAVE = "eo_autosave"
        private const val KEY_VOL_MASTER = "eo_vol_master"
        private const val KEY_VOL_MUSIC = "eo_vol_music"
        private const val KEY_VOL_SFX = "eo_vol_sfx"
        private const val KEY_FPS = "eo_fps"

        /** Reads the persisted engine options, falling back to engine defaults. */
        @JvmStatic
        fun fromPrefs(ctx: Context): EngineOptions = EngineOptions(
            fov = AppSettings.getIntOption(ctx, KEY_FOV, DEFAULT.fov),
            gamma = AppSettings.getFloatOption(ctx, KEY_GAMMA, DEFAULT.gamma),
            crosshair = AppSettings.getIntOption(ctx, KEY_CROSSHAIR, DEFAULT.crosshair),
            dynamicLights = AppSettings.getBoolOption(ctx, KEY_DYNLIGHTS, DEFAULT.dynamicLights),
            vsync = AppSettings.getBoolOption(ctx, KEY_VSYNC, DEFAULT.vsync),
            autosaveCount = AppSettings.getIntOption(ctx, KEY_AUTOSAVE, DEFAULT.autosaveCount),
            masterVolume = AppSettings.getFloatOption(ctx, KEY_VOL_MASTER, DEFAULT.masterVolume),
            musicVolume = AppSettings.getFloatOption(ctx, KEY_VOL_MUSIC, DEFAULT.musicVolume),
            sfxVolume = AppSettings.getFloatOption(ctx, KEY_VOL_SFX, DEFAULT.sfxVolume),
            fpsHud = AppSettings.getBoolOption(ctx, KEY_FPS, DEFAULT.fpsHud),
        )

        @JvmStatic fun saveFov(ctx: Context, v: Int) = AppSettings.setIntOption(ctx, KEY_FOV, v)
        @JvmStatic fun saveGamma(ctx: Context, v: Float) = AppSettings.setFloatOption(ctx, KEY_GAMMA, v)
        @JvmStatic fun saveCrosshair(ctx: Context, v: Int) = AppSettings.setIntOption(ctx, KEY_CROSSHAIR, v)
        @JvmStatic fun saveDynamicLights(ctx: Context, v: Boolean) = AppSettings.setBoolOption(ctx, KEY_DYNLIGHTS, v)
        @JvmStatic fun saveVsync(ctx: Context, v: Boolean) = AppSettings.setBoolOption(ctx, KEY_VSYNC, v)
        @JvmStatic fun saveAutosaveCount(ctx: Context, v: Int) = AppSettings.setIntOption(ctx, KEY_AUTOSAVE, v)
        @JvmStatic fun saveMasterVolume(ctx: Context, v: Float) = AppSettings.setFloatOption(ctx, KEY_VOL_MASTER, v)
        @JvmStatic fun saveMusicVolume(ctx: Context, v: Float) = AppSettings.setFloatOption(ctx, KEY_VOL_MUSIC, v)
        @JvmStatic fun saveSfxVolume(ctx: Context, v: Float) = AppSettings.setFloatOption(ctx, KEY_VOL_SFX, v)
        @JvmStatic fun saveFpsHud(ctx: Context, v: Boolean) = AppSettings.setBoolOption(ctx, KEY_FPS, v)
    }
}

/** Renders a float with a Locale-independent decimal point (engine parses "1.00", never "1,00"). */
private fun f2(v: Float): String = String.format(Locale.US, "%.2f", v)

private fun b(v: Boolean): String = if (v) "1" else "0"

/**
 * Builds the optional engine-configuration portion of the command line as a
 * space-separated, trailing-space string ready to concatenate alongside the mod
 * args. Returns "" only via the caller's enable gate — this pure function always
 * emits the full set so it is deterministic and unit-testable.
 *
 * Order is fixed for test stability; later `+set` on the line win, so user-typed
 * extra args (appended after this) still override the launcher.
 */
fun buildEngineCvarArgs(opts: EngineOptions): String = buildString {
    append("+fov ").append(opts.fov).append(' ')
    append("+set vid_gamma ").append(f2(opts.gamma)).append(' ')
    append("+set crosshair ").append(opts.crosshair).append(' ')
    append("+set gl_lights ").append(b(opts.dynamicLights)).append(' ')
    append("+set vid_vsync ").append(b(opts.vsync)).append(' ')
    append("+set autosavecount ").append(opts.autosaveCount).append(' ')
    append("+set snd_mastervolume ").append(f2(opts.masterVolume)).append(' ')
    append("+set snd_musicvolume ").append(f2(opts.musicVolume)).append(' ')
    append("+set snd_sfxvolume ").append(f2(opts.sfxVolume)).append(' ')
    append("+set vid_fps ").append(b(opts.fpsHud)).append(' ')
}
