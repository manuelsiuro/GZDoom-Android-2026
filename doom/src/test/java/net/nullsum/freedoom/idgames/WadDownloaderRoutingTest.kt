package net.nullsum.freedoom.idgames

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WadDownloaderRoutingTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val downloader = WadDownloader()

    @Test
    fun `unrecognized content falls back to the dir guess`() {
        // A staging dir with no map lumps detects as UNKNOWN, so the pre-download guess wins.
        val staging = tmp.newFolder("staging")
        File(staging, "readme.txt").writeText("hi")
        assertEquals("doom2", downloader.routeFolder(staging, listOf("readme.txt"), "doom2"))
    }

    @Test
    fun `placeInstall moves staging and replaces an existing install`() {
        val wads = tmp.newFolder("wads")
        val staging = File(wads, ".staging/scythe").apply { mkdirs() }
        File(staging, "scythe.wad").writeText("v2")

        val target = File(wads, "doom2/scythe")
        // Pre-existing (stale) install that must be replaced.
        target.mkdirs()
        File(target, "old.wad").writeText("v1")

        downloader.placeInstall(staging, target)

        assertFalse(staging.exists())
        assertFalse(File(target, "old.wad").exists())
        assertEquals("v2", File(target, "scythe.wad").readText())
        assertTrue(target.isDirectory)
    }
}
