package pm.gnosis.heimdall.data.repositories.impls

import org.junit.Test

import org.junit.Assert.*
import java.lang.IllegalArgumentException

class IDNEnsNormalizerTest {

    private val normalizer = IDNEnsNormalizer()

    @Test
    fun normalizeValid() {
        assertEquals("clean-string-123658", normalizer.normalize("clean-string-123658"))
        assertEquals("clean-string-123658", normalizer.normalize("cLean-String-123658"))
        assertEquals("xn--clen-ssring-123658-ntb", normalizer.normalize("cLeän-ßring-123658"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun normalizeInvalidUnderscore() {
        normalizer.normalize("_invalid")
    }

    @Test(expected = IllegalArgumentException::class)
    fun normalizeInvalidSpace() {
        normalizer.normalize(" invalid")
    }
}