package net.nullsum.freedoom.idgames

import org.junit.Assert.assertEquals
import org.junit.Test

class IdgamesMirrorsTest {

    @Test
    fun `joins mirror dir and filename with single slashes`() {
        assertEquals(
            "https://youfailit.net/pub/idgames/levels/doom2/Ports/megawads/dscythe.zip",
            IdgamesMirrors.downloadUrl(
                "https://youfailit.net/pub/idgames/",
                "levels/doom2/Ports/megawads/",
                "dscythe.zip",
            ),
        )
    }

    @Test
    fun `normalizes missing and duplicate slashes`() {
        val expected = "https://example.com/idgames/levels/doom2/av.zip"
        assertEquals(expected, IdgamesMirrors.downloadUrl("https://example.com/idgames", "levels/doom2/", "av.zip"))
        assertEquals(expected, IdgamesMirrors.downloadUrl("https://example.com/idgames/", "/levels/doom2", "/av.zip"))
        assertEquals(expected, IdgamesMirrors.downloadUrl("https://example.com/idgames", "levels/doom2", "av.zip"))
    }
}
