package com.msa.freedoom.ui.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class GameFolderNameTest {

    @Test
    fun `doom splits by map slot`() {
        assertEquals("doom", gameFolderName(AddonInfo(GameFamily.DOOM, MapSlot.EPISODIC)))
        assertEquals("doom2", gameFolderName(AddonInfo(GameFamily.DOOM, MapSlot.MAPXX)))
        assertEquals("doom2", gameFolderName(AddonInfo(GameFamily.DOOM, MapSlot.BOTH)))
        // No maps (e.g. a gameplay mod) defaults to the episodic/base doom folder.
        assertEquals("doom", gameFolderName(AddonInfo(GameFamily.DOOM, MapSlot.NONE)))
    }

    @Test
    fun `other families map to their own folder`() {
        assertEquals("heretic", gameFolderName(AddonInfo(GameFamily.HERETIC, MapSlot.EPISODIC)))
        assertEquals("hexen", gameFolderName(AddonInfo(GameFamily.HEXEN, MapSlot.MAPXX)))
        assertEquals("strife", gameFolderName(AddonInfo(GameFamily.STRIFE, MapSlot.MAPXX)))
        assertEquals("chex", gameFolderName(AddonInfo(GameFamily.CHEX, MapSlot.EPISODIC)))
        assertEquals("unknown", gameFolderName(AddonInfo(GameFamily.UNKNOWN, MapSlot.NONE)))
    }
}
