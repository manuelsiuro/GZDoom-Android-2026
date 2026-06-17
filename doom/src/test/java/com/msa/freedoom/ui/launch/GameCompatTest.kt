package com.msa.freedoom.ui.launch

import org.junit.Assert.assertEquals
import org.junit.Test

/** Guards the pure compatibility classification used by the mod picker. */
class GameCompatTest {

    @Test
    fun `iwad profiles by filename`() {
        assertEquals(IwadProfile(GameFamily.DOOM, MapSlot.EPISODIC), iwadProfile("freedoom1.wad"))
        assertEquals(IwadProfile(GameFamily.DOOM, MapSlot.MAPXX), iwadProfile("freedoom2.wad"))
        assertEquals(IwadProfile(GameFamily.DOOM, MapSlot.MAPXX), iwadProfile("PLUTONIA.WAD"))
        assertEquals(IwadProfile(GameFamily.HERETIC, MapSlot.EPISODIC), iwadProfile("heretic.wad"))
        assertEquals(IwadProfile(GameFamily.HEXEN, MapSlot.MAPXX), iwadProfile("hexen.wad"))
        assertEquals(IwadProfile(GameFamily.STRIFE, MapSlot.MAPXX), iwadProfile("strife1.wad"))
        assertEquals(IwadProfile(GameFamily.CHEX, MapSlot.EPISODIC), iwadProfile("chex3.wad"))
    }

    @Test
    fun `unknown iwad falls back to doom`() {
        assertEquals(IwadProfile(GameFamily.DOOM, MapSlot.NONE), iwadProfile("mystery.wad"))
    }

    private val doom1 = IwadProfile(GameFamily.DOOM, MapSlot.EPISODIC)
    private val doom2 = IwadProfile(GameFamily.DOOM, MapSlot.MAPXX)

    @Test
    fun `same family and slot is compatible`() {
        assertEquals(Verdict.COMPATIBLE, verdict(doom2, AddonInfo(GameFamily.DOOM, MapSlot.MAPXX)))
        assertEquals(Verdict.COMPATIBLE, verdict(doom1, AddonInfo(GameFamily.DOOM, MapSlot.EPISODIC)))
    }

    @Test
    fun `slot mismatch within family is minor`() {
        assertEquals(Verdict.MINOR, verdict(doom1, AddonInfo(GameFamily.DOOM, MapSlot.MAPXX)))
        assertEquals(Verdict.MINOR, verdict(doom2, AddonInfo(GameFamily.DOOM, MapSlot.EPISODIC)))
    }

    @Test
    fun `no-map or both-slot add-on is compatible regardless of native slot`() {
        assertEquals(Verdict.COMPATIBLE, verdict(doom1, AddonInfo(GameFamily.DOOM, MapSlot.NONE)))
        assertEquals(Verdict.COMPATIBLE, verdict(doom1, AddonInfo(GameFamily.DOOM, MapSlot.BOTH)))
    }

    @Test
    fun `different family is incompatible`() {
        assertEquals(Verdict.INCOMPATIBLE, verdict(doom2, AddonInfo(GameFamily.HERETIC, MapSlot.EPISODIC)))
        assertEquals(Verdict.INCOMPATIBLE, verdict(doom2, AddonInfo(GameFamily.HEXEN, MapSlot.MAPXX)))
    }

    @Test
    fun `unknown family is unknown verdict`() {
        assertEquals(Verdict.UNKNOWN, verdict(doom2, AddonInfo(GameFamily.UNKNOWN, MapSlot.NONE)))
        assertEquals(Verdict.UNKNOWN, verdict(doom1, AddonInfo(GameFamily.UNKNOWN, MapSlot.MAPXX)))
    }
}
