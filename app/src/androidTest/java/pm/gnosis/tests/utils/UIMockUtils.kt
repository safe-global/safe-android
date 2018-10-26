package pm.gnosis.tests.utils

import org.mockito.Mockito

object UIMockUtils {
    // Svalinn depends on mockito inline which doesn't work for UI tests
    fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
