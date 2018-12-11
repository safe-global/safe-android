package pm.gnosis.heimdall.ui.settings.general

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.Predicate
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
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.TestCompletable

@RunWith(MockitoJUnitRunner::class)
class GeneralSettingsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: GeneralSettingsViewModel

    @Before
    fun setUp() {
        viewModel = GeneralSettingsViewModel(encryptionManagerMock, tokenRepositoryMock)
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

    @Test
    fun loadPaymentToken() {
        val testObserver = TestObserver<ERC20Token>()
        given(tokenRepositoryMock.loadPaymentToken()).willReturn(Single.just(ERC20Token.ETHER_TOKEN))

        viewModel.loadPaymentToken().subscribe(testObserver)

        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ERC20Token.ETHER_TOKEN)
    }

    @Test
    fun loadPaymentTokenError() {
        val exception = Exception()
        val testObserver = TestObserver<ERC20Token>()
        given(tokenRepositoryMock.loadPaymentToken()).willReturn(Single.error(exception))

        viewModel.loadPaymentToken().subscribe(testObserver)

        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(Predicate { it == exception })
    }
}
