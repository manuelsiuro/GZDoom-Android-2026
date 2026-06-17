package com.msa.freedoom.ui.editor

import com.msa.freedoom.ui.editor.data.sanitizeWadName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SanitizeWadNameTest {

    @Test
    fun lowercasesAndReplacesIllegalChars() {
        assertEquals("my_map_01", sanitizeWadName("My Map 01"))
    }

    @Test
    fun collapsesRepeatsAndTrims() {
        assertEquals("a_b", sanitizeWadName("__a???b__"))
    }

    @Test
    fun emptyFallsBackToMymap() {
        assertEquals("mymap", sanitizeWadName(""))
        assertEquals("mymap", sanitizeWadName("***"))
    }

    @Test
    fun keepsDigitsLettersDashUnderscore() {
        assertEquals("level-2_b", sanitizeWadName("level-2_b"))
    }

    @Test
    fun capsLength() {
        val out = sanitizeWadName("x".repeat(100))
        assertTrue(out.length <= 32)
    }
}
