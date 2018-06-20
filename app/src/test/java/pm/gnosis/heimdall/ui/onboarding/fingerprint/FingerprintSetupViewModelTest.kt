package pm.gnosis.heimdall.ui.onboarding.fingerprint

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
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
        testObserver.assertValues(true)
        fingerprintSubject.onNext(false)
        testObserver.assertValues(true, false)

        then(encryptionManagerMock).should().observeFingerprintForSetup()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeFingerprintForSetupError() {
        val testObserver = TestObserver.create<Boolean>()
        given(encryptionManagerMock.observeFingerprintForSetup()).willReturn(Observable.error(Exception()))

        viewModel.observeFingerprintForSetup().subscribe(testObserver)

        testObserver.assertFailure(Exception::class.java)
        then(encryptionManagerMock).should().observeFingerprintForSetup()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
    }
}
