package net.nullsum.freedoom.ui.editor.generate

import net.nullsum.freedoom.ui.editor.model.MapProject
import net.nullsum.freedoom.ui.editor.model.Tuning
import net.nullsum.freedoom.ui.editor.model.WadFormat
import kotlin.math.roundToInt

/**
 * Builds the Preferences.ini the native converter reads, from a [MapProject].
 *
 * Strategy: the verified `[Themes]`/`[Theme.*]` blocks and the thing `Types.*` lists are
 * carried verbatim from the engine-tested defaults ([THEME_BLOCKS], [thingTypesBlock]);
 * only `[Options]` and the `Count.*` lines are generated from the project's [Tuning].
 *
 * Note: the thing category key the C++ parser looks up is derived from
 * `ToString(ThingCategory)` (Config.h:102-112), so the "average monster" key MUST be
 * `MonstersAverage` for BOTH Types and Count. The legacy inline INI used
 * `Types.MonstersMedium`, which silently never loaded — fixed here.
 */
fun buildPreferencesIni(project: MapProject): String {
    val doom1 = project.format == WadFormat.DOOM1
    val t = project.tuning
    return buildString {
        append("[Options]\n")
        append("BuildNodes=false\n")
        append("Doom1Format=").append(doom1).append('\n')
        append("Episode=").append(project.episode.coerceIn(1, 9)).append('\n')
        append("GenerateEntranceAndExit=true\n")
        append("GenerateThings=true\n\n")

        append("[Things]\n")
        append(thingTypesBlock)
        append('\n')
        // Count.* scaled from the density sliders.
        for ((category, base) in COUNT_BASELINE) {
            val density = when (category) {
                "MonstersEasy", "MonstersAverage", "MonstersHard", "MonstersVeryHard" -> t.monsterDensity
                "AmmoLarge", "AmmoSmall" -> t.ammoDensity
                else -> t.itemDensity // Armor, Health, PowerUps, WeaponsHigh, WeaponsLow
            }
            val (lo, hi) = scaleRange(base.first, base.second, density)
            append("Count.").append(category).append('=').append(lo).append(',').append(hi).append('\n')
        }
        append('\n')
        // The native INI parser splits STRING arrays on ';' but numeric arrays on ','
        // (INIFile.h GetArraySeparator). The baseline blocks below are authored with commas
        // for readability; convert only the `Textures.*` value lists to semicolons so the
        // converter actually sees each texture name (the legacy comma form was read as one
        // truncated, invalid name — every generated map had missing floor/wall textures).
        append(
            THEME_BLOCKS.lineSequence().joinToString("\n") { line ->
                if (line.startsWith("Textures.")) line.replace(',', ';') else line
            },
        )
    }
}

/**
 * Maps a 0..1 density onto a baseline (min,max), preserving min<=max and min>=0.
 * factor = 0.25 + 1.75*d  → 0f≈quarter, 0.5f≈baseline, 1f≈double.
 */
internal fun scaleRange(baseMin: Int, baseMax: Int, density: Float): Pair<Int, Int> {
    val factor = 0.25f + 1.75f * density.coerceIn(0f, 1f)
    val lo = (baseMin * factor).roundToInt().coerceAtLeast(0)
    val hi = (baseMax * factor).roundToInt().coerceAtLeast(lo)
    return lo to hi
}

// Baseline (min,max) per thing category, copied from the engine-tested defaults.
// Order is cosmetic only (it's the order Count.* lines are emitted).
private val COUNT_BASELINE: List<Pair<String, Pair<Int, Int>>> = listOf(
    "AmmoLarge" to (4 to 8),
    "AmmoSmall" to (8 to 12),
    "Armor" to (2 to 4),
    "Health" to (8 to 10),
    "MonstersAverage" to (15 to 25),
    "MonstersEasy" to (15 to 25),
    "MonstersHard" to (5 to 10),
    "MonstersVeryHard" to (2 to 5),
    "PowerUps" to (0 to 2),
    "WeaponsHigh" to (1 to 3),
    "WeaponsLow" to (2 to 4),
)

// Thing type IDs per category — verbatim from the engine-tested defaults, with the
// MonstersMedium→MonstersAverage key corrected to match the C++ ToString(ThingCategory).
private val thingTypesBlock = """Types.AmmoSmall=2008,2007,2047,2010
Types.AmmoLarge=2048,2046,2049,17
Types.Armor=2018,2019
Types.Health=2012,2011
Types.MonstersVeryHard=64,69,3003
Types.MonstersHard=3005,69
Types.MonstersAverage=3002,3006,58,65
Types.MonstersEasy=3004,9,3001
Types.PowerUps=8,2023,2022,2024,2013
Types.WeaponsLow=2002,2005,2001,82
Types.WeaponsHigh=2006,2004,2003
"""

// [Themes] + per-theme blocks — verbatim from the engine-tested defaults. Theme pixel
// colours here must stay in sync with MapTheme.pixelRgb.
private val THEME_BLOCKS = """[Themes]
Default=255,255,255
Cave=128,128,128
City=128,128,255
Hell=255,0,0

[Theme.Default]
Height.Default=0,64
Height.DoorSide=0,64
Height.Entrance=4,64
Height.Exit=4,64
Height.Exterior=0,128
Height.SpecialCeiling=0,60
Height.SpecialFloor=-4,64

LightLevel.Default=192
LightLevel.DoorSide=192
LightLevel.Entrance=192
LightLevel.Exit=255
LightLevel.Exterior=255
LightLevel.SpecialCeiling=255
LightLevel.SpecialFloor=192

SectorSpecial.Default=0
SectorSpecial.DoorSide=0
SectorSpecial.Entrance=0
SectorSpecial.Exit=8
SectorSpecial.Exterior=0
SectorSpecial.SpecialCeiling=17
SectorSpecial.SpecialFloor=7

Textures.Ceiling=CEIL3_1,CEIL3_3,FLAT18,FLAT20,FLAT4,FLAT5_5
Textures.CeilingSpecial=CEIL3_4,FLAT17,FLAT2,FLOOR1_7,GRNLITE1,TLITE6_1,TLITE6_4,TLITE6_5,TLITE6_6
Textures.Door=DOOR1,DOOR3
Textures.DoorSide=LITE5
Textures.Floor=FLAT1_1,FLAT1_2,FLAT5,FLAT5_5,FLOOR0_1,FLOOR0_2,FLOOR1_1,FLOOR3_3,FLOOR4_1,FLOOR5_3,FLOOR5_4
Textures.FloorEntrance=CEIL4_3
Textures.FloorExit=FLAT22
Textures.FloorExterior=FLOOR6_2,FLAT10
Textures.FloorSpecial=NUKAGE1
Textures.Wall=STARTAN2,CEMENT6,GRAY1,ICKWALL1,SLADWALL,BIGBRIK3,BRONZE4,BROVINE2,SPACEW2,SPACEW4,TEKGREN2
Textures.WallExterior=

[Theme.Cave]
Height.Default=0,64
Height.DoorSide=0,64
Height.Entrance=4,64
Height.Exit=4,64
Height.Exterior=0,128
Height.SpecialCeiling=0,64
Height.SpecialFloor=-4,64

LightLevel.Default=164
LightLevel.DoorSide=164
LightLevel.Entrance=164
LightLevel.Exit=255
LightLevel.Exterior=192
LightLevel.SpecialCeiling=164
LightLevel.SpecialFloor=192

SectorSpecial.Default=0
SectorSpecial.DoorSide=0
SectorSpecial.Entrance=0
SectorSpecial.Exit=8
SectorSpecial.Exterior=0
SectorSpecial.SpecialCeiling=0
SectorSpecial.SpecialFloor=0

Textures.Ceiling=FLAT5_7,FLAT5_8,FLOOR6_2,MFLR8_2
Textures.CeilingSpecial=FLAT5_2
Textures.Door=BIGDOOR5
Textures.DoorSide=METAL,SUPPORT3
Textures.Floor=FLAT10,FLAT5_7,FLAT5_8,FLOOR6_2,MFLR8_2,MFLR8_4
Textures.FloorEntrance=CEIL4_3
Textures.FloorExit=FLAT22
Textures.FloorExterior=
Textures.FloorSpecial=FWATER1
Textures.Wall=ASHWALL2,ROCK1,STONE4,STONE6
Textures.WallExterior=

[Theme.City]
Height.Default=0,64
Height.DoorSide=0,64
Height.Entrance=4,64
Height.Exit=4,64
Height.Exterior=0,256
Height.SpecialCeiling=0,64
Height.SpecialFloor=-4,64

LightLevel.Default=192
LightLevel.DoorSide=192
LightLevel.Entrance=192
LightLevel.Exit=255
LightLevel.Exterior=220
LightLevel.SpecialCeiling=255
LightLevel.SpecialFloor=192

SectorSpecial.Default=0
SectorSpecial.DoorSide=0
SectorSpecial.Entrance=0
SectorSpecial.Exit=8
SectorSpecial.Exterior=0
SectorSpecial.SpecialCeiling=0
SectorSpecial.SpecialFloor=0

Textures.Ceiling=CEIL3_1,FLAT5_4,FLAT9
Textures.CeilingSpecial=CEIL3_4
Textures.Door=DOOR1,DOOR3
Textures.DoorSide=LITE5
Textures.Floor=FLAT3,FLAT5,FLOOR0_2,FLOOR0_6,FLOOR0_7,FLOOR3_3
Textures.FloorEntrance=CEIL4_3
Textures.FloorExit=FLAT22
Textures.FloorExterior=FLAT1,RROCK03
Textures.FloorSpecial=FLAT14,FLOOR1_1,FLOOR1_6
Textures.Wall=STARTAN2,CEMENT6,GRAY1,ICKWALL1,SLADWALL,BIGBRIK3,BRONZE4,BROVINE2,SPACEW2,SPACEW4,TEKGREN2
Textures.WallExterior=BIGBRIK2,BLAKWAL2,BRICK1,BRICK5,BRICK11

[Theme.Hell]
Height.Default=0,64
Height.DoorSide=0,64
Height.Entrance=4,64
Height.Exit=4,64
Height.Exterior=0,128
Height.SpecialCeiling=0,64
Height.SpecialFloor=-4,64

LightLevel.Default=164
LightLevel.DoorSide=164
LightLevel.Entrance=164
LightLevel.Exit=255
LightLevel.Exterior=192
LightLevel.SpecialCeiling=164
LightLevel.SpecialFloor=192

SectorSpecial.Default=0
SectorSpecial.DoorSide=0
SectorSpecial.Entrance=0
SectorSpecial.Exit=8
SectorSpecial.Exterior=0
SectorSpecial.SpecialCeiling=0
SectorSpecial.SpecialFloor=5

Textures.Ceiling=CEIL1_1,FLAT5_1,FLAT5_2,FLAT5_3
Textures.CeilingSpecial=FLAT5_6,SFLR6_1,SFLR6_4
Textures.Door=BIGDOOR5
Textures.DoorSide=METAL,SUPPORT3
Textures.Floor=FLAT1_1,FLAT1_2,FLAT5_1,FLAT5_2,FLAT5_3
Textures.FloorEntrance=GATE4
Textures.FloorExit=GATE1,GATE2,GATE3
Textures.FloorExterior=FLAT5_7,FLAT5_8,FLOOR6_1,FLOOR6_2,MFLR8_2,MFLR8_3
Textures.FloorSpecial=LAVA1
Textures.Wall=GSTONE1,GSTVINE1,MARBGRAY,MARBLE2,SKINEDGE,SKSNAKE1
Textures.WallExterior=
"""
