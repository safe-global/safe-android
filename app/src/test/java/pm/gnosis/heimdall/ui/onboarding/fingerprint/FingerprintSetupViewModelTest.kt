package pm.gnosis.heimdall.ui.onboarding.fingerprint

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule

@RunWith(MockitoJUnitRunner::class)
class FingerprintSetupViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var viewModel: FingerprintSetupContract

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    @Before
    fun setup() {
        viewModel = FingerprintSetupViewModel(encryptionManagerMock)
    }

    @Test
    fun observeFingerprintForSetup() {
        val testObserver = TestObserver.create<Boolean>()
        val fingerprintSubject = PublishSubject.create<Boolean>()
        given(encryptionManagerMock.observeFingerprintForSetup()).willReturn(fingerprintSubject)

        viewModel.observeFingerprintForSetup().subscribe(testObserver)

        fingerprintSubject.onNext(true)
        testObserver.assertValueAt(0, true)
        fingerprintSubject.onNext(false)
        testObserver.assertValueAt(1, false)
    }

    @Test
    fun observeFingerprintForSetupError() {
        val testObserver = TestObserver.create<Boolean>()
        val exception = Exception()
        given(encryptionManagerMock.observeFingerprintForSetup()).willReturn(Observable.error(exception))

        viewModel.observeFingerprintForSetup().subscribe(testObserver)

        testObserver.assertError(exception)
    }
}
