package pm.gnosis.tests

import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.mock
import org.mockito.Mockito
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.svalinn.security.EncryptionManager

abstract class BaseUiTest {

    protected val eventTrackerMock = mock(EventTracker::class.java)

    protected val encryptionManagerMock = mock(EncryptionManager::class.java)

    protected fun setupBaseInjects(): ApplicationComponent =
        TestApplication.mockComponent().apply {
            given(inject(any<BaseActivity>())).will {
                (it.arguments.first() as BaseActivity).encryptionManager = encryptionManagerMock
                (it.arguments.first() as BaseActivity).eventTracker = eventTrackerMock
                Unit
            }
            given(encryptionManager()).willReturn(encryptionManagerMock)
            given(eventTracker()).willReturn(eventTrackerMock)
    }

    // Svalinn depends on mockito inline which doesn't work for UI tests
    fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
