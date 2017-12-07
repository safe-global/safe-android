package pm.gnosis.tests.utils

import android.content.Context
import org.mockito.BDDMockito.*
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

fun Context.mockGetString(): Context {
    given(getString(anyInt())).will { it.arguments.first().toString() }
    return this
}