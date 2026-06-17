package com.msa.freedoom.idgames

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveListingParserTest {

    // Abridged real Apache <pre> index from the gamers.org idgames mirror.
    private val sample = """
        <html><head><title>Index of /pub/idgames/levels/doom2/megawads</title></head>
        <body><h1>Index of /pub/idgames/levels/doom2/megawads</h1>
        <pre><img src="/icons/blank.gif" alt="Icon "> <a href="?C=N;O=D">Name</a>
        <hr><img src="/icons/back.gif" alt="[PARENTDIR]"> <a href="/pub/idgames/levels/doom2/">Parent Directory</a>       -
        <img src="/icons/folder.gif" alt="[DIR]"> <a href="Ports/">Ports/</a>            2020-01-04 09:12    -
        <img src="/icons/unknown.gif" alt="[   ]"> <a href="scythe.zip">scythe.zip</a>   2003-04-10 12:00  2.0M
        <img src="/icons/text.gif" alt="[TXT]"> <a href="scythe.txt">scythe.txt</a>      2003-04-10 12:00  6.1K
        <hr></pre></body></html>
    """.trimIndent()

    @Test
    fun `parses folders and files with sizes`() {
        val nodes = ArchiveListingParser.parse(sample)
        assertEquals(3, nodes.size)

        val dir = nodes.first { it.name == "Ports" }
        assertTrue(dir.isDir)
        assertNull(dir.sizeBytes)
        assertEquals("2020-01-04 09:12", dir.lastModified)

        val zip = nodes.first { it.name == "scythe.zip" }
        assertFalse(zip.isDir)
        assertEquals((2.0 * (1L shl 20)).toLong(), zip.sizeBytes)

        val txt = nodes.first { it.name == "scythe.txt" }
        assertEquals((6.1 * (1L shl 10)).toLong(), txt.sizeBytes)
    }

    @Test
    fun `skips parent directory and sort links`() {
        val names = ArchiveListingParser.parse(sample).map { it.name }
        assertFalse(names.any { it.contains("Parent") })
        assertFalse(names.any { it.startsWith("?") })
    }

    @Test
    fun `a 404 page with no rows parses to empty`() {
        val html = "<html><body><h1>404 Not Found</h1><p>Nothing here.</p></body></html>"
        assertEquals(emptyList<ArchiveListingParser.Node>(), ArchiveListingParser.parse(html))
    }
}
