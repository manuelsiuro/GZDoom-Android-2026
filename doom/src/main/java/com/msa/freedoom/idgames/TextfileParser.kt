package com.msa.freedoom.idgames

import com.msa.freedoom.ui.launch.GameFamily
import com.msa.freedoom.ui.launch.MapSlot

/**
 * Parses the standard Doom `.txt` template that ships inside almost every idgames
 * upload. Only the few machine-useful fields are extracted — the "Game" line and the
 * map slot drive a confident pre-download compatibility hint, the multiplayer flags
 * are shown in the detail sheet. Scanning is capped so a pathological multi-hundred-KB
 * readme can't stall the UI.
 */
object TextfileParser {

    data class TextfileInfo(
        val gameLine: String?,
        val family: GameFamily,
        val slot: MapSlot,
        val singlePlayer: Boolean?,
        val coop: Boolean?,
        val deathmatch: Boolean?,
    )

    private const val MAX_CHARS = 64 * 1024
    private const val MAX_LINES = 400
    private val FIELD_RE = Regex("""^\s*([A-Za-z0-9 #/_'-]+?)\s*:\s*(.*)$""")
    private val EPISODIC_RE = Regex("""E\dM\d""", RegexOption.IGNORE_CASE)
    private val MAPXX_RE = Regex("""MAP\d\d""", RegexOption.IGNORE_CASE)

    fun parse(textfile: String): TextfileInfo {
        var gameLine: String? = null
        var maps: String? = null
        var singlePlayer: Boolean? = null
        var coop: Boolean? = null
        var deathmatch: Boolean? = null

        textfile.take(MAX_CHARS).lineSequence().take(MAX_LINES).forEach { line ->
            val m = FIELD_RE.matchEntire(line) ?: return@forEach
            val key = m.groupValues[1].trim().lowercase()
            val value = m.groupValues[2].trim()
            when {
                key == "game" && gameLine == null -> gameLine = value
                (key.startsWith("map #") || key == "maps" || key.startsWith("level #") ||
                    key.startsWith("map(s)")) && maps == null -> maps = value
                key.startsWith("single player") -> singlePlayer = yesNo(value)
                key.startsWith("cooperative") || key.startsWith("co-op") -> coop = yesNo(value)
                key.startsWith("deathmatch") -> deathmatch = yesNo(value)
            }
        }

        return TextfileInfo(
            gameLine = gameLine,
            family = familyOf(gameLine),
            slot = slotOf(gameLine, maps),
            singlePlayer = singlePlayer,
            coop = coop,
            deathmatch = deathmatch,
        )
    }

    private fun familyOf(game: String?): GameFamily {
        val g = game?.lowercase() ?: return GameFamily.UNKNOWN
        return when {
            "heretic" in g -> GameFamily.HERETIC
            "hexen" in g -> GameFamily.HEXEN
            "strife" in g -> GameFamily.STRIFE
            "chex" in g -> GameFamily.CHEX
            // "doom 2", "doom ii", "final doom", "tnt", "plutonia" and plain "doom" all map to Doom.
            "doom" in g || "tnt" in g || "plutonia" in g || "freedoom" in g -> GameFamily.DOOM
            else -> GameFamily.UNKNOWN
        }
    }

    private fun slotOf(game: String?, maps: String?): MapSlot {
        val hay = "${game.orEmpty()} ${maps.orEmpty()}"
        val episodic = EPISODIC_RE.containsMatchIn(hay)
        val mapxx = MAPXX_RE.containsMatchIn(hay)
        return when {
            episodic && mapxx -> MapSlot.BOTH
            episodic -> MapSlot.EPISODIC
            mapxx -> MapSlot.MAPXX
            // Game line without a map token: infer the family's usual slot.
            game != null && ("doom 2" in game.lowercase() || "doom ii" in game.lowercase() ||
                "final doom" in game.lowercase() || "tnt" in game.lowercase() ||
                "plutonia" in game.lowercase()) -> MapSlot.MAPXX
            else -> MapSlot.NONE
        }
    }

    private fun yesNo(value: String): Boolean? = when {
        value.startsWith("yes", ignoreCase = true) -> true
        value.startsWith("no", ignoreCase = true) -> false
        else -> null
    }
}
