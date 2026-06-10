package net.nullsum.freedoom.idgames

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdgamesParserTest {

    private fun success(result: IdgamesResult<List<IdgamesFile>>): List<IdgamesFile> =
        (result as IdgamesResult.Success).value

    @Test
    fun `parses multi-result array`() {
        val json = """
            {"content": {"file": [
                {"id": 11944, "title": "Scythe", "dir": "levels/doom2/megawads/",
                 "filename": "scythe.zip", "size": 2086863, "date": "2003-04-10",
                 "author": "Erik Alm", "description": "32 maps", "rating": 4.1225, "votes": 408},
                {"id": 13600, "title": "Scythe 2", "dir": "levels/doom2/Ports/megawads/",
                 "filename": "scythe2.zip", "size": 8103001, "rating": 4.2431, "votes": 399}
            ]}, "meta": {"version": 3}}
        """.trimIndent()
        val files = success(IdgamesParser.parseFileList(json))
        assertEquals(2, files.size)
        assertEquals(11944L, files[0].id)
        assertEquals("Scythe", files[0].title)
        assertEquals("levels/doom2/megawads/", files[0].dir)
        assertEquals(2086863L, files[0].size)
        assertEquals(4.1225, files[0].rating, 1e-9)
        assertEquals("scythe", files[0].installName)
    }

    @Test
    fun `parses single result returned as bare object`() {
        val json = """
            {"content": {"file":
                {"id": 8194, "title": "Requiem", "dir": "levels/doom2/megawads/",
                 "filename": "requiem.zip", "size": 3987788, "rating": 3.9929, "votes": 283}
            }, "meta": {"version": 3}}
        """.trimIndent()
        val files = success(IdgamesParser.parseFileList(json))
        assertEquals(1, files.size)
        assertEquals("Requiem", files[0].title)
    }

    @Test
    fun `parses error payload`() {
        val json = """
            {"content": null,
             "error": {"type": "Query Too Small", "message": "Must be at least 3 characters."},
             "meta": {"version": 3}}
        """.trimIndent()
        val result = IdgamesParser.parseFileList(json)
        assertTrue(result is IdgamesResult.ApiError)
        result as IdgamesResult.ApiError
        assertEquals("Query Too Small", result.type)
        assertEquals("Must be at least 3 characters.", result.message)
    }

    @Test
    fun `parses warning payload as empty list`() {
        val json = """
            {"warning": {"type": "No Results", "message": "No files returned."},
             "meta": {"version": 3}}
        """.trimIndent()
        assertEquals(emptyList<IdgamesFile>(), success(IdgamesParser.parseFileList(json)))
    }

    @Test
    fun `ignores unknown fields and defaults missing ones`() {
        val json = """
            {"content": {"file": [
                {"id": 1, "filename": "x.zip", "dir": "levels/", "age": 1588914000,
                 "email": null, "credits": "someone", "textfile": "..."}
            ]}}
        """.trimIndent()
        val files = success(IdgamesParser.parseFileList(json))
        assertEquals(1, files.size)
        assertNull(files[0].title)
        assertEquals(0.0, files[0].rating, 0.0)
        assertEquals(0, files[0].votes)
    }

    @Test
    fun `parseSingle reads entry directly under content`() {
        val json = """
            {"content": {"id": 5191, "title": "Icarus: Alien Vanguard",
             "dir": "themes/TeamTNT/icarus/", "filename": "icarus.zip", "size": 2849657},
             "meta": {"version": 3}}
        """.trimIndent()
        val result = IdgamesParser.parseSingle(json) as IdgamesResult.Success
        assertEquals("Icarus: Alien Vanguard", result.value?.title)
    }
}
