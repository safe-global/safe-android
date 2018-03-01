package pm.gnosis.heimdall.ui.settings.security

import io.reactivex.Completable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.TestCompletable

@RunWith(MockitoJUnitRunner::class)
class SecuritySettingsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    private lateinit var viewModel: SecuritySettingsViewModel

    @Before
    fun setUp() {
        viewModel = SecuritySettingsViewModel(encryptionManagerMock)
    }

    @Test
    fun fingerprintAvailable() {
        given(encryptionManagerMock.canSetupFingerprint()).willReturn(true)

        val result = viewModel.isFingerprintAvailable()

        then(encryptionManagerMock).should().canSetupFingerprint()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        assertEquals(result, true)
    }

    @Test
    fun fingerprintNotAvailable() {
        given(encryptionManagerMock.canSetupFingerprint()).willReturn(false)

        val result = viewModel.isFingerprintAvailable()

        then(encryptionManagerMock).should().canSetupFingerprint()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        assertEquals(result, false)
    }

    @Test
    fun clearFingerprintData() {
        val testCompletable = TestCompletable()
        val testObserver = TestObserver<Result<Unit>>()
        given(encryptionManagerMock.clearFingerprintData()).willReturn(testCompletable)

        viewModel.clearFingerprintData().subscribe(testObserver)

        then(encryptionManagerMock).should().clearFingerprintData()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(DataResult(Unit))
    }

    @Test
    fun clearFingerprintDataError() {
        val exception = Exception()
        val testObserver = TestObserver<Result<Unit>>()
        given(encryptionManagerMock.clearFingerprintData()).willReturn(Completable.error(exception))

        viewModel.clearFingerprintData().subscribe(testObserver)

        then(encryptionManagerMock).should().clearFingerprintData()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }
}
