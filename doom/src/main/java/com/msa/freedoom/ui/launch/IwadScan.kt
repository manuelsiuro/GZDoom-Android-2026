package com.msa.freedoom.ui.launch

import java.io.File

/**
 * Engine/bundled files that must never be offered as selectable IWADs.
 * Shared by the launch tab and the map editor's test-IWAD picker.
 */
val EXCLUDED_IWAD_FILES = setOf(
    "prboom-plus.wad", "gzdoom.pk3", "gzdoom_dev.pk3",
    "lights_dt.pk3", "brightmaps_dt.pk3", "lights.pk3", "brightmaps.pk3",
    // Extra engine pk3s bundled with the UZDoom 5.0 stack — not selectable IWADs.
    "game_widescreen_gfx.pk3", "uzdoom.pk3", "uzdoom_game_support.pk3",
)

/**
 * Scans the game-data directory for selectable IWADs (`.wad`/`.pk3`/`.pk7`, minus engine
 * files), returned as the raw filesystem order — the legacy int `last_iwad` indexes into
 * this. Sort with `.sortedBy { it.file.lowercase() }` for display.
 */
fun scanIwads(baseDir: String): List<WadEntry> =
    File(baseDir).listFiles().orEmpty()
        .filter { !it.isDirectory }
        .filter { f ->
            val name = f.name.lowercase()
            (name.endsWith(".wad") || name.endsWith(".pk3") || name.endsWith(".pk7")) &&
                name !in EXCLUDED_IWAD_FILES
        }
        .map { WadEntry(it.name, it.length()) }
