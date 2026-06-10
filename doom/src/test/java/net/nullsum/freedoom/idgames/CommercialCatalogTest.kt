package net.nullsum.freedoom.idgames

import org.junit.Assert.assertEquals
import org.junit.Test

class CommercialCatalogTest {

    @Test
    fun `parses commercial catalog json`() {
        val json = """
            {"version": 1, "entries": [
                {"title": "Doom II: Hell on Earth", "filename": "doom2.wad",
                 "description": "The 1994 sequel.", "note": "Import doom2.wad from your purchased copy."}
            ]}
        """.trimIndent()
        val catalog = idgamesJson.decodeFromString<CommercialCatalogFile>(json)
        assertEquals(1, catalog.entries.size)
        assertEquals("doom2.wad", catalog.entries[0].filename)
        assertEquals("Import doom2.wad from your purchased copy.", catalog.entries[0].note)
    }
}
