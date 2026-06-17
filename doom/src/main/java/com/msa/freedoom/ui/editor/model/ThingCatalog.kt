package com.msa.freedoom.ui.editor.model

/**
 * The palette of placeable things, keyed by Doom editor number (DoomEdNum).
 *
 * Numbers follow the classic Doom/Doom II set, which Freedoom reuses verbatim, so
 * these resolve against `freedoom2.wad` (and other Doom-II-compatible IWADs).
 * [sprite] is the 4-letter sprite prefix used to fetch a thumbnail from the IWAD via
 * `WadTextures` (the browser tries `<sprite>A1` then `<sprite>A0`); null = no preview.
 *
 * Source: DoomWiki "Thing types" (DoomEdNum) table.
 */
data class ThingType(
    val id: Int,
    val displayName: String,
    val category: ThingCategory,
    val sprite: String?,
) {
    /** Candidate sprite lump names to try for a thumbnail, most-specific first. */
    val spriteCandidates: List<String>
        get() = sprite?.let { listOf("${it}A1", "${it}A0", "${it}A2A8") } ?: emptyList()
}

enum class ThingCategory(val displayName: String) {
    Starts("Starts"),
    Keys("Keys"),
    Monsters("Monsters"),
    Weapons("Weapons"),
    Ammo("Ammo"),
    Items("Items"),
    Misc("Misc"),
}

object ThingCatalog {

    val all: List<ThingType> = listOf(
        // Player / co-op / deathmatch starts
        ThingType(1, "Player 1 start", ThingCategory.Starts, "PLAY"),
        ThingType(2, "Player 2 start", ThingCategory.Starts, "PLAY"),
        ThingType(3, "Player 3 start", ThingCategory.Starts, "PLAY"),
        ThingType(4, "Player 4 start", ThingCategory.Starts, "PLAY"),
        ThingType(11, "Deathmatch start", ThingCategory.Starts, "PLAY"),

        // Keys
        ThingType(5, "Blue keycard", ThingCategory.Keys, "BKEY"),
        ThingType(13, "Red keycard", ThingCategory.Keys, "RKEY"),
        ThingType(6, "Yellow keycard", ThingCategory.Keys, "YKEY"),
        ThingType(40, "Blue skull key", ThingCategory.Keys, "BSKU"),
        ThingType(38, "Red skull key", ThingCategory.Keys, "RSKU"),
        ThingType(39, "Yellow skull key", ThingCategory.Keys, "YSKU"),

        // Monsters
        ThingType(3004, "Zombieman", ThingCategory.Monsters, "POSS"),
        ThingType(9, "Shotgun guy", ThingCategory.Monsters, "SPOS"),
        ThingType(65, "Heavy weapon dude", ThingCategory.Monsters, "CPOS"),
        ThingType(3001, "Imp", ThingCategory.Monsters, "TROO"),
        ThingType(3002, "Demon", ThingCategory.Monsters, "SARG"),
        ThingType(58, "Spectre", ThingCategory.Monsters, "SARG"),
        ThingType(3006, "Lost soul", ThingCategory.Monsters, "SKUL"),
        ThingType(3005, "Cacodemon", ThingCategory.Monsters, "HEAD"),
        ThingType(69, "Hell knight", ThingCategory.Monsters, "BOS2"),
        ThingType(3003, "Baron of Hell", ThingCategory.Monsters, "BOSS"),
        ThingType(68, "Arachnotron", ThingCategory.Monsters, "BSPI"),
        ThingType(71, "Pain elemental", ThingCategory.Monsters, "PAIN"),
        ThingType(66, "Revenant", ThingCategory.Monsters, "SKEL"),
        ThingType(67, "Mancubus", ThingCategory.Monsters, "FATT"),
        ThingType(64, "Arch-vile", ThingCategory.Monsters, "VILE"),
        ThingType(16, "Cyberdemon", ThingCategory.Monsters, "CYBR"),
        ThingType(7, "Spider mastermind", ThingCategory.Monsters, "SPID"),
        ThingType(84, "Wolfenstein SS", ThingCategory.Monsters, "SSWV"),

        // Weapons
        ThingType(2001, "Shotgun", ThingCategory.Weapons, "SHOT"),
        ThingType(82, "Super shotgun", ThingCategory.Weapons, "SGN2"),
        ThingType(2002, "Chaingun", ThingCategory.Weapons, "MGUN"),
        ThingType(2003, "Rocket launcher", ThingCategory.Weapons, "LAUN"),
        ThingType(2004, "Plasma rifle", ThingCategory.Weapons, "PLAS"),
        ThingType(2006, "BFG9000", ThingCategory.Weapons, "BFUG"),
        ThingType(2005, "Chainsaw", ThingCategory.Weapons, "CSAW"),

        // Ammo
        ThingType(2007, "Clip", ThingCategory.Ammo, "CLIP"),
        ThingType(2048, "Box of bullets", ThingCategory.Ammo, "AMMO"),
        ThingType(2008, "Shotgun shells", ThingCategory.Ammo, "SHEL"),
        ThingType(2049, "Box of shells", ThingCategory.Ammo, "SBOX"),
        ThingType(2010, "Rocket", ThingCategory.Ammo, "ROCK"),
        ThingType(2046, "Box of rockets", ThingCategory.Ammo, "BROK"),
        ThingType(2047, "Energy cell", ThingCategory.Ammo, "CELL"),
        ThingType(17, "Energy cell pack", ThingCategory.Ammo, "CELP"),

        // Health & items
        ThingType(2011, "Stimpack", ThingCategory.Items, "STIM"),
        ThingType(2012, "Medikit", ThingCategory.Items, "MEDI"),
        ThingType(2018, "Armor", ThingCategory.Items, "ARM1"),
        ThingType(2019, "Mega armor", ThingCategory.Items, "ARM2"),
        ThingType(2025, "Radiation suit", ThingCategory.Items, "SUIT"),

        // Misc
        ThingType(14, "Teleport landing", ThingCategory.Misc, null),
    )

    private val byIdMap: Map<Int, ThingType> = all.associateBy { it.id }

    val byCategory: Map<ThingCategory, List<ThingType>> =
        all.groupBy { it.category }

    fun byId(id: Int): ThingType? = byIdMap[id]
}
