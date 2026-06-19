package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.model.TileType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefabStampTest {

    @Test
    fun catalog_allWellFormed() {
        assertTrue(MapPrefabs.all.isNotEmpty())
        for (p in MapPrefabs.all) {
            assertEquals("${p.name} cell count", p.width * p.height, p.cells.size)
            assertTrue(
                "${p.name} ordinals in range",
                p.cells.all { it in 0..TileType.entries.lastIndex },
            )
            for (t in p.things) {
                assertTrue(
                    "${p.name} thing in bounds",
                    t.cellX in 0 until p.width && t.cellY in 0 until p.height,
                )
                assertTrue(
                    "${p.name} thing on open floor",
                    TileType.fromOrdinal(p.cells[t.cellY * p.width + t.cellX]).acceptsThing,
                )
            }
        }
    }

    @Test
    fun catalog_groupingCoversEverything() {
        assertEquals(MapPrefabs.all.size, MapPrefabs.byCategory.values.sumOf { it.size })
    }

    @Test
    fun transform_rotationSwapsDimsAndKeepsThingsValid() {
        val p = MapPrefabs.all.first { it.things.isNotEmpty() }
        val r = p.transformed(rotation = 1, mirror = false)
        assertEquals(p.height, r.width)
        assertEquals(p.width, r.height)
        assertEquals(p.width * p.height, r.cells.size)
        assertEquals(p.things.size, r.things.size)
        for (t in r.things) {
            assertTrue(t.cellX in 0 until r.width && t.cellY in 0 until r.height)
            assertTrue(TileType.fromOrdinal(r.cells[t.cellY * r.width + t.cellX]).acceptsThing)
        }
    }

    @Test
    fun transform_fourRotationsReturnToOriginal() {
        val p = MapPrefabs.all.first { it.things.isNotEmpty() }
        val back = p.transformed(1, false)
            .transformed(1, false)
            .transformed(1, false)
            .transformed(1, false)
        assertEquals(p.width, back.width)
        assertEquals(p.height, back.height)
        assertArrayEquals(p.cells, back.cells)
        assertEquals(p.things, back.things)
    }

    @Test
    fun transform_mirrorTwiceIsIdentity() {
        val p = MapPrefabs.all.first { it.things.isNotEmpty() }
        val back = p.transformed(0, true).transformed(0, true)
        assertArrayEquals(p.cells, back.cells)
        assertEquals(p.things, back.things)
    }

    @Test
    fun transform_noOpReturnsSameInstance() {
        val p = MapPrefabs.all.first()
        assertTrue(p === p.transformed(0, false))
    }
}
