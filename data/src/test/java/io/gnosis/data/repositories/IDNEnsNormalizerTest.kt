package io.gnosis.data.repositories

import org.junit.Assert.*
import org.junit.Test
import java.lang.IllegalArgumentException

class IDNEnsNormalizerTest {

    private val normalizer = IDNEnsNormalizer()

    @Test
    fun `normalize (valid String) should return normalized string`() {
        assertEquals("clean-string-123658", normalizer.normalize("clean-string-123658"))
        assertEquals("clean-string-123658", normalizer.normalize("cLean-String-123658"))
        assertEquals("xn--clen-ssring-123658-ntb", normalizer.normalize("cLeän-ßring-123658"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `normalize (invalid String with underscore) should throw`() {
        normalizer.normalize("_invalid")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `normalize (invalid String with space) should throw`() {
        normalizer.normalize(" invalid")
    }
}
