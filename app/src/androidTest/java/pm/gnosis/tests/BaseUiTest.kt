package pm.gnosis.tests

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.mock
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.UIMockUtils

abstract class BaseUiTest {

    protected val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    protected val eventTrackerMock = mock(EventTracker::class.java)!!

    protected val encryptionManagerMock = mock(EncryptionManager::class.java)!!

    protected fun setupBaseInjects(): ApplicationComponent =
        TestApplication.mockComponent().apply {
            given(inject(UIMockUtils.any<BaseActivity>())).will {
                (it.arguments.first() as BaseActivity).encryptionManager = encryptionManagerMock
                (it.arguments.first() as BaseActivity).eventTracker = eventTrackerMock
                Unit
            }
            given(encryptionManager()).willReturn(encryptionManagerMock)
            given(eventTracker()).willReturn(eventTrackerMock)
        }
}
