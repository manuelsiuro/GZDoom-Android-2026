package com.msa.freedoom.idgames

import com.msa.freedoom.ui.launch.GameFamily
import com.msa.freedoom.ui.launch.MapSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class IdgamesDirClassifierTest {

    @Test
    fun `doom2 path maps to Doom MAPxx`() {
        val info = IdgamesDirClassifier.classify("levels/doom2/megawads/", "scythe.zip")
        assertEquals(GameFamily.DOOM, info.family)
        assertEquals(MapSlot.MAPXX, info.slot)
    }

    @Test
    fun `doom path maps to Doom episodic`() {
        val info = IdgamesDirClassifier.classify("levels/doom/m-o/", "mylevel.zip")
        assertEquals(GameFamily.DOOM, info.family)
        assertEquals(MapSlot.EPISODIC, info.slot)
    }

    @Test
    fun `heretic hexen strife chex paths`() {
        assertEquals(GameFamily.HERETIC, IdgamesDirClassifier.classify("levels/heretic/", "x.zip").family)
        assertEquals(GameFamily.HEXEN, IdgamesDirClassifier.classify("levels/hexen/", "x.zip").family)
        assertEquals(GameFamily.STRIFE, IdgamesDirClassifier.classify("levels/strife/", "x.zip").family)
        assertEquals(GameFamily.CHEX, IdgamesDirClassifier.classify("levels/chex/", "x.zip").family)
    }

    @Test
    fun `themes subtree still finds the game segment`() {
        val info = IdgamesDirClassifier.classify("themes/TeamTNT/doom2/", "icarus.zip")
        assertEquals(GameFamily.DOOM, info.family)
        assertEquals(MapSlot.MAPXX, info.slot)
    }

    @Test
    fun `generic dir falls back to filename hint`() {
        assertEquals(GameFamily.HEXEN, IdgamesDirClassifier.classify("combos/", "hexenmod.zip").family)
        val unknown = IdgamesDirClassifier.classify("misc/", "coolthing.zip")
        assertEquals(GameFamily.UNKNOWN, unknown.family)
        assertEquals(MapSlot.NONE, unknown.slot)
    }
}
