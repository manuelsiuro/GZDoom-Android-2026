package net.nullsum.freedoom.idgames

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ZipInstallerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun makeZip(vararg entries: Pair<String, ByteArray>): File {
        val zip = tmp.newFile("test.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            for ((name, bytes) in entries) {
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }
        return zip
    }

    @Test
    fun `extracts content and readme, skips junk`() {
        val zip = makeZip(
            "cool.wad" to byteArrayOf(1, 2, 3),
            "readme.txt" to "hi".toByteArray(),
            "setup.exe" to byteArrayOf(9),
            "nested.zip" to byteArrayOf(8),
        )
        val dest = tmp.newFolder("install")
        val content = ZipInstaller.install(zip, dest)
        assertEquals(listOf("cool.wad"), content)
        assertTrue(File(dest, "cool.wad").exists())
        assertTrue(File(dest, "readme.txt").exists())
        assertFalse(File(dest, "setup.exe").exists())
        assertFalse(File(dest, "nested.zip").exists())
    }

    @Test
    fun `preserves internal relative paths`() {
        val zip = makeZip(
            "maps/level.wad" to byteArrayOf(1),
            "dehacked/patch.deh" to byteArrayOf(2),
        )
        val dest = tmp.newFolder("install")
        val content = ZipInstaller.install(zip, dest)
        assertEquals(setOf("maps/level.wad", "dehacked/patch.deh"), content.toSet())
        assertTrue(File(dest, "maps/level.wad").exists())
        assertTrue(File(dest, "dehacked/patch.deh").exists())
    }

    @Test
    fun `rejects zip-slip entries`() {
        val zip = makeZip("../evil.wad" to byteArrayOf(1))
        val dest = tmp.newFolder("install")
        try {
            ZipInstaller.install(zip, dest)
            org.junit.Assert.fail("expected SecurityException")
        } catch (expected: SecurityException) {
        }
        assertFalse(File(dest.parentFile, "evil.wad").exists())
    }

    @Test
    fun `returns empty list when archive has nothing playable`() {
        val zip = makeZip("readme.txt" to "only text".toByteArray())
        val dest = tmp.newFolder("install")
        assertEquals(emptyList<String>(), ZipInstaller.install(zip, dest))
    }
}
