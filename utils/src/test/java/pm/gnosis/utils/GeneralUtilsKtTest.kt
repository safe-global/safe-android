package pm.gnosis.utils

import org.junit.Assert.*
import org.junit.Test

class GeneralUtilsKtTest {
    @Test
    fun testNullOnThrow() {
        assertNull(nullOnThrow {
            throw Exception()
        })

        assertEquals("Test", nullOnThrow { "Test" })
    }

    @Test
    fun testSameSign() {
        assertTrue("Should be same sign!", sameSign(13, 7))
        assertTrue("Should be same sign!", sameSign(-13, -7))
        assertFalse("Should not be same sign!", sameSign(13, -7))
        assertFalse("Should not be same sign!", sameSign(-13, 7))
    }

    @Test
    fun testTrimWhitespace() {
        assertEquals("", " ".trimWhitespace())
        assertEquals("", "        ".trimWhitespace())
        assertEquals("test", " test       ".trimWhitespace())
        assertEquals("test string", " test    string   ".trimWhitespace())
        assertEquals("test string", "test string".trimWhitespace())
        assertEquals("test", "test".trimWhitespace())
    }

    @Test
    fun testWords() {
        assertEquals(listOf("test", "that", "we", "split", "every", "word"), "   test  that      we split every  word  ".words())
        assertEquals(listOf("single-word"), "single-word".words())
        assertEquals(listOf<String>(), "        ".words())
        assertEquals(listOf<String>(), "".words())
    }
}