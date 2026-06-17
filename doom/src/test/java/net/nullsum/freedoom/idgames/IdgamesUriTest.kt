package net.nullsum.freedoom.idgames

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdgamesUriTest {

    @Test
    fun `numeric body parses to id`() {
        assertEquals(IdgamesUri.Ref.ById(12345), IdgamesUri.parse("idgames://12345"))
    }

    @Test
    fun `path body parses to path`() {
        assertEquals(
            IdgamesUri.Ref.ByPath("levels/doom2/megawads/scythe.zip"),
            IdgamesUri.parse("idgames://levels/doom2/megawads/scythe.zip"),
        )
    }

    @Test
    fun `leading slash is trimmed`() {
        assertEquals(
            IdgamesUri.Ref.ByPath("levels/doom/foo.zip"),
            IdgamesUri.parse("idgames:///levels/doom/foo.zip"),
        )
    }

    @Test
    fun `query and fragment are dropped`() {
        assertEquals(IdgamesUri.Ref.ById(7), IdgamesUri.parse("idgames://7?utm=x#frag"))
    }

    @Test
    fun `non-idgames and empty uris return null`() {
        assertNull(IdgamesUri.parse("https://example.com/foo"))
        assertNull(IdgamesUri.parse("idgames://"))
    }
}
