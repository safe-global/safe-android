package pm.gnosis.tests.utils

import android.content.Context
import org.mockito.BDDMockito.anyInt
import org.mockito.BDDMockito.given
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

fun Context.mockGetStringWithArgs(): Context {
    given(getString(anyInt(), MockUtils.any())).will { it.arguments.joinToString() }
    return this
}
