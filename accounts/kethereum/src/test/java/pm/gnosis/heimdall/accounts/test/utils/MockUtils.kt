package pm.gnosis.heimdall.test.utils

import org.mockito.Mockito


object MockUtils {
    fun <T> eq(value: T): T {
        Mockito.eq(value)
        return uninitialized()
    }

    fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}