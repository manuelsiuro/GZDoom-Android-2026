package net.nullsum.freedoom.idgames

import net.nullsum.freedoom.ui.launch.GameFamily
import net.nullsum.freedoom.ui.launch.MapSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextfileParserTest {

    @Test
    fun `parses Doom 2 megawad fields`() {
        val txt = """
            ===========================================================================
            Title                   : Scythe
            Game                    : DOOM2
            Map #                   : MAP01 - MAP32
            Single Player           : Yes
            Cooperative 2-4 Player  : Yes
            Deathmatch 2-4 Player   : No
        """.trimIndent()
        val info = TextfileParser.parse(txt)
        assertEquals("DOOM2", info.gameLine)
        assertEquals(GameFamily.DOOM, info.family)
        assertEquals(MapSlot.MAPXX, info.slot)
        assertEquals(true, info.singlePlayer)
        assertEquals(true, info.coop)
        assertEquals(false, info.deathmatch)
    }

    @Test
    fun `parses Doom 1 episodic`() {
        val txt = """
            Game     : Doom
            Map #    : E1M1, E1M2
        """.trimIndent()
        val info = TextfileParser.parse(txt)
        assertEquals(GameFamily.DOOM, info.family)
        assertEquals(MapSlot.EPISODIC, info.slot)
    }

    @Test
    fun `final doom without map token infers mapxx`() {
        val info = TextfileParser.parse("Game : Final Doom (TNT)")
        assertEquals(GameFamily.DOOM, info.family)
        assertEquals(MapSlot.MAPXX, info.slot)
    }

    @Test
    fun `heretic family`() {
        val info = TextfileParser.parse("Game : Heretic\nMap # : E1M1")
        assertEquals(GameFamily.HERETIC, info.family)
        assertEquals(MapSlot.EPISODIC, info.slot)
    }

    @Test
    fun `empty readme yields unknowns`() {
        val info = TextfileParser.parse("just some prose with no fields at all")
        assertNull(info.gameLine)
        assertEquals(GameFamily.UNKNOWN, info.family)
        assertEquals(MapSlot.NONE, info.slot)
        assertNull(info.singlePlayer)
    }

    @Test
    fun `caps scanning of a huge textfile`() {
        // A field far past the line cap must be ignored (proves the cap is enforced).
        val txt = buildString {
            repeat(1000) { appendLine("filler line $it") }
            appendLine("Game : Doom2")
        }
        assertTrue(TextfileParser.parse(txt).family == GameFamily.UNKNOWN)
    }
}
