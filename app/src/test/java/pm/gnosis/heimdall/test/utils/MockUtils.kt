package pm.gnosis.heimdall.test.utils

import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.internal.creation.MockSettingsImpl
import org.mockito.internal.util.MockUtil


object MockUtils {
    fun <T> anyObject(clzz: Class<T>): T {
        Mockito.any<T>()
        return uncheckedMock(clzz)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uncheckedMock(clzz: Class<T>): T {
        val impl = MockSettingsImpl<T>().defaultAnswer(Answers.RETURNS_DEFAULTS) as MockSettingsImpl<T>
        val creationSettings = impl.confirm(clzz)
        return MockUtil.createMock(creationSettings)
    }

    fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}