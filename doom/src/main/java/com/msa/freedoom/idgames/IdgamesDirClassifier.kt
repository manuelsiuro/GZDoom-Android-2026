package com.msa.freedoom.idgames

import com.msa.freedoom.ui.launch.AddonInfo
import com.msa.freedoom.ui.launch.GameFamily
import com.msa.freedoom.ui.launch.MapSlot
import com.msa.freedoom.ui.launch.familyFromName

/**
 * Cheap, network-free pre-download compatibility guess from an idgames `dir` path
 * (e.g. "levels/doom2/megawads/") plus the filename. The archive's top-level game
 * folders are authoritative for the family; the map-slot follows the family's usual
 * style (Doom 1 / Heretic episodic, Doom 2 / Hexen / Strife MAPxx). Anything we can't
 * place stays [GameFamily.UNKNOWN] and is later confirmed by the post-install lump scan.
 */
object IdgamesDirClassifier {

    fun classify(dir: String, filename: String = ""): AddonInfo {
        val segs = dir.lowercase().split('/').filter { it.isNotBlank() }
        return when {
            "doom2" in segs -> AddonInfo(GameFamily.DOOM, MapSlot.MAPXX)
            "hacx" in segs -> AddonInfo(GameFamily.DOOM, MapSlot.MAPXX)
            "doom" in segs -> AddonInfo(GameFamily.DOOM, MapSlot.EPISODIC)
            "heretic" in segs -> AddonInfo(GameFamily.HERETIC, MapSlot.EPISODIC)
            "hexen" in segs -> AddonInfo(GameFamily.HEXEN, MapSlot.MAPXX)
            "strife" in segs -> AddonInfo(GameFamily.STRIFE, MapSlot.MAPXX)
            "chex" in segs -> AddonInfo(GameFamily.CHEX, MapSlot.EPISODIC)
            // No usable path hint: fall back to a filename family hint only.
            else -> AddonInfo(familyFromName(filename.lowercase()), MapSlot.NONE)
        }
    }
}
