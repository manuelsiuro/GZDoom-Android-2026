package com.msa.freedoom.idgames

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassicCatalogTest {

    @Test
    fun `parses catalog json and builds raw github url`() {
        val json = """
            {"version": 1, "entries": [
                {"title": "Doom (shareware)", "filename": "doom1.wad", "size": 4196020,
                 "description": "Episode 1 shareware", "note": "Shareware."}
            ]}
        """.trimIndent()
        val catalog = idgamesJson.decodeFromString<ClassicCatalogFile>(json)
        assertEquals(1, catalog.entries.size)
        val entry = catalog.entries[0]
        assertEquals("doom1.wad", entry.filename)
        assertEquals(
            "https://raw.githubusercontent.com/Akbar30Bill/DOOM_wads/master/doom1.wad",
            entry.downloadUrl,
        )
    }
}
