package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.generate.buildPreferencesIni
import com.msa.freedoom.ui.editor.generate.encodeThings
import com.msa.freedoom.ui.editor.generate.validThings
import com.msa.freedoom.ui.editor.model.MapDoc
import com.msa.freedoom.ui.editor.model.MapProject
import com.msa.freedoom.ui.editor.model.MapThing
import com.msa.freedoom.ui.editor.model.TextureRole
import com.msa.freedoom.ui.editor.model.ThingCatalog
import com.msa.freedoom.ui.editor.model.TileType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThingsTexturesTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ---- thing descriptor encoding ----

    @Test
    fun encodeThings_matchesNativeDescriptorFormat() {
        val things = listOf(
            MapThing(type = 3001, cellX = 2, cellY = 5, angle = 90, flags = 7),
            MapThing(type = 5, cellX = 0, cellY = 0),
        )
        // "type,cellX,cellY,angle,flags" records joined by ';'
        assertEquals("3001,2,5,90,7;5,0,0,0,7", encodeThings(things))
    }

    @Test
    fun encodeThings_emptyWhenNoThings() {
        assertEquals("", encodeThings(emptyList()))
    }

    // ---- thing validity (open-floor cells only, in bounds) ----

    @Test
    fun validThings_dropsThingsOnWallsAndOutOfBounds() {
        // 3x1 grid: [Room, Wall, Room]
        val tiles = intArrayOf(TileType.Room.ordinal, TileType.Wall.ordinal, TileType.Room.ordinal)
        val map = MapDoc(
            width = 3, height = 1, tiles = tiles,
            things = listOf(
                MapThing(3001, 0, 0), // on Room -> ok
                MapThing(3001, 1, 0), // on Wall -> dropped
                MapThing(3001, 5, 0), // out of bounds -> dropped
            ),
        )
        val valid = validThings(map)
        assertEquals(1, valid.size)
        assertEquals(0, valid.first().cellX)
    }

    @Test
    fun validThings_keepsStartCells() {
        val tiles = intArrayOf(TileType.Start.ordinal)
        val map = MapDoc(1, 1, tiles, things = listOf(MapThing(1, 0, 0)))
        assertEquals(1, validThings(map).size)
    }

    // ---- texture overrides in Preferences.ini ----

    @Test
    fun ini_appliesWallTextureOverride() {
        val project = MapProject(textureOverrides = mapOf(TextureRole.Wall.name to listOf("BRICK1", "BRICK5")))
        val ini = buildPreferencesIni(project)
        val wallLines = ini.lineSequence().filter { it.startsWith("Textures.Wall=") }.toList()
        assertTrue("override must appear in every theme block", wallLines.isNotEmpty())
        wallLines.forEach {
            assertEquals("Textures.Wall=BRICK1;BRICK5", it)
        }
    }

    @Test
    fun ini_overrideUsesSemicolons() {
        val project = MapProject(textureOverrides = mapOf(TextureRole.Floor.name to listOf("FLAT1", "FLAT2")))
        val ini = buildPreferencesIni(project)
        val floorLine = ini.lineSequence().first { it.startsWith("Textures.Floor=") }
        assertTrue(floorLine.contains(';'))
        assertFalse(floorLine.substringAfter('=').contains(','))
    }

    @Test
    fun ini_emptyOverrideKeepsThemeDefault() {
        val withEmpty = MapProject(textureOverrides = mapOf(TextureRole.Wall.name to emptyList()))
        val ini = buildPreferencesIni(withEmpty)
        val defaultIni = buildPreferencesIni(MapProject())
        // An empty override list must not blank out the wall textures.
        val wall = ini.lineSequence().first { it.startsWith("Textures.Wall=") }
        val defaultWall = defaultIni.lineSequence().first { it.startsWith("Textures.Wall=") }
        assertEquals(defaultWall, wall)
    }

    // ---- ThingCatalog integrity ----

    @Test
    fun catalog_hasNoDuplicateDoomEdNums() {
        val ids = ThingCatalog.all.map { it.id }
        assertEquals("DoomEdNums must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun catalog_lookupByIdWorks() {
        assertEquals("Imp", ThingCatalog.byId(3001)?.displayName)
        assertEquals("Blue keycard", ThingCatalog.byId(5)?.displayName)
    }

    // ---- serialization round-trip (schema v2 fields) ----

    @Test
    fun project_thingsAndOverridesRoundTrip() {
        val project = MapProject(
            maps = listOf(MapDoc(8, 8, IntArray(64), things = listOf(MapThing(3001, 1, 2, 45, 7)))),
            textureOverrides = mapOf(TextureRole.Wall.name to listOf("BRICK1")),
            manualThings = true,
        )
        val restored = json.decodeFromString<MapProject>(json.encodeToString(project))
        assertEquals(project, restored)
        assertEquals(1, restored.maps.first().things.size)
        assertTrue(restored.manualThings)
    }

    @Test
    fun project_v1JsonWithoutNewFieldsStillLoads() {
        // A pre-schema-2 project (no things/textureOverrides/manualThings) must load with defaults.
        val legacy = """{"schemaVersion":1,"name":"old","maps":[{"width":4,"height":4,"tiles":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]}]}"""
        val restored = json.decodeFromString<MapProject>(legacy)
        assertEquals("old", restored.name)
        assertTrue(restored.maps.first().things.isEmpty())
        assertTrue(restored.textureOverrides.isEmpty())
        assertFalse(restored.manualThings)
    }
}
