package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.generate.buildPreferencesIni
import com.msa.freedoom.ui.editor.generate.scaleRange
import com.msa.freedoom.ui.editor.model.MapProject
import com.msa.freedoom.ui.editor.model.Tuning
import com.msa.freedoom.ui.editor.model.WadFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesIniTest {

    @Test
    fun scaleRange_midIsRoughlyBaseline() {
        // factor at 0.5 = 0.25 + 1.75*0.5 = 1.125
        val (lo, hi) = scaleRange(8, 12, 0.5f)
        assertEquals(9, lo)  // round(8*1.125)=9
        assertEquals(14, hi) // round(12*1.125)=13.5 -> 14
    }

    @Test
    fun scaleRange_zeroDensityQuarters() {
        val (lo, hi) = scaleRange(8, 12, 0f) // factor 0.25
        assertEquals(2, lo)
        assertEquals(3, hi)
    }

    @Test
    fun scaleRange_neverInvertedOrNegative() {
        val (lo, hi) = scaleRange(0, 2, 0f)
        assertTrue(lo >= 0)
        assertTrue(hi >= lo)
    }

    @Test
    fun ini_usesMonstersAverageKeyForBothTypesAndCount() {
        // The C++ parser derives the key from ToString(ThingCategory) == "MonstersAverage";
        // the legacy inline INI shipped Types.MonstersMedium, which silently never loaded.
        val ini = buildPreferencesIni(MapProject())
        assertTrue("Types.MonstersAverage must be present", ini.contains("Types.MonstersAverage="))
        assertTrue("Count.MonstersAverage must be present", ini.contains("Count.MonstersAverage="))
        assertTrue("legacy MonstersMedium key must be gone", !ini.contains("MonstersMedium"))
    }

    @Test
    fun ini_reflectsFormatAndEpisode() {
        val doom1 = buildPreferencesIni(MapProject(format = WadFormat.DOOM1, episode = 3))
        assertTrue(doom1.contains("Doom1Format=true"))
        assertTrue(doom1.contains("Episode=3"))
        val doom2 = buildPreferencesIni(MapProject(format = WadFormat.DOOM2))
        assertTrue(doom2.contains("Doom1Format=false"))
    }

    @Test
    fun ini_higherMonsterDensityRaisesCounts() {
        val low = buildPreferencesIni(MapProject(tuning = Tuning(monsterDensity = 0f)))
        val high = buildPreferencesIni(MapProject(tuning = Tuning(monsterDensity = 1f)))
        val lowEasy = countLine(low, "Count.MonstersEasy")
        val highEasy = countLine(high, "Count.MonstersEasy")
        assertTrue("high density max should exceed low", highEasy.second > lowEasy.second)
    }

    @Test
    fun ini_textureListsUseSemicolons() {
        // The C++ INI parser splits string arrays on ';' (INIFile.h GetArraySeparator),
        // so multi-texture lines must be semicolon-separated, not comma.
        val ini = buildPreferencesIni(MapProject())
        val wallLine = ini.lineSequence().first { it.startsWith("Textures.Wall=") }
        assertTrue("texture list must be semicolon-separated", wallLine.contains(';'))
        assertTrue("texture list must not contain commas", !wallLine.substringAfter('=').contains(','))
    }

    @Test
    fun ini_containsThemeBlocks() {
        val ini = buildPreferencesIni(MapProject())
        assertTrue(ini.contains("[Themes]"))
        assertTrue(ini.contains("[Theme.Default]"))
        assertTrue(ini.contains("[Theme.Hell]"))
    }

    private fun countLine(ini: String, key: String): Pair<Int, Int> {
        val line = ini.lineSequence().first { it.startsWith("$key=") }
        val parts = line.substringAfter('=').split(',')
        return parts[0].trim().toInt() to parts[1].trim().toInt()
    }
}
