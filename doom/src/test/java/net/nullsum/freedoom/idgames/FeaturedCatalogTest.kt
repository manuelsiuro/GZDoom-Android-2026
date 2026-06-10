package net.nullsum.freedoom.idgames

import org.junit.Assert.assertEquals
import org.junit.Test

class FeaturedCatalogTest {

    @Test
    fun `parses catalog json`() {
        val json = """
            {"version": 1, "entries": [
                {"title": "Scythe", "author": "Erik Alm", "dir": "levels/doom2/megawads/",
                 "filename": "scythe.zip", "size": 2086863, "idgamesId": 11944,
                 "description": "32 maps", "note": "Use Freedoom Phase 2."}
            ]}
        """.trimIndent()
        val catalog = idgamesJson.decodeFromString<FeaturedCatalogFile>(json)
        assertEquals(1, catalog.version)
        assertEquals(1, catalog.entries.size)
        assertEquals("scythe", catalog.entries[0].installName)
        assertEquals("Use Freedoom Phase 2.", catalog.entries[0].note)
    }
}
