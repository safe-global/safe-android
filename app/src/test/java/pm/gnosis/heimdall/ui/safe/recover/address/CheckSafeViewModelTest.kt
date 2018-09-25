package pm.gnosis.heimdall.ui.safe.recover.address

import android.arch.persistence.room.EmptyResultSetException
import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class CheckSafeViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var safeRepoMock: GnosisSafeRepository

    private lateinit var viewModel: CheckSafeViewModel

    @Before
    fun setUp() {
        viewModel = CheckSafeViewModel(contextMock, safeRepoMock)
    }

    @Test
    fun checkSafeAddressEmpty() {
        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("").subscribe(observer)
        observer.assertResult(DataResult(false))

        then(safeRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeAddressInvalid() {
        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("InvalidAddress").subscribe(observer)
        observer.assertResult(DataResult(false))

        then(safeRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeTooShort() {
        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("C2AC20b3Bb950C087f18a458DB68271325a48132").subscribe(observer)
        observer.assertResult(DataResult(false))

        then(safeRepoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun checkSafeAlreadyDeployed() {
        contextMock.mockGetString()
        given(safeRepoMock.loadSafe(MockUtils.any())).willReturn(Single.just(Safe(TEST_SAFE)))

        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("0x1f81FFF89Bd57811983a35650296681f99C65C7e").subscribe(observer)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.safe_already_exists.toString())))

        then(safeRepoMock).should().loadSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeAlreadyPending() {
        contextMock.mockGetString()
        given(safeRepoMock.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadPendingSafe(MockUtils.any())).willReturn(
            Single.just(PendingSafe(TEST_PENDING_SAFE, TEST_TX_HASH, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT))
        )

        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("0xC2AC20b3Bb950C087f18a458DB68271325a48132").subscribe(observer)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.safe_already_exists.toString())))

        then(safeRepoMock).should().loadSafe(TEST_PENDING_SAFE)
        then(safeRepoMock).should().loadPendingSafe(TEST_PENDING_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeAlreadyRecovering() {
        contextMock.mockGetString()
        given(safeRepoMock.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadPendingSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(
            Single.just(RecoveringSafe(TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, "", BigInteger.ZERO, BigInteger.ZERO,
                TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT, BigInteger.ZERO, TransactionExecutionRepository.Operation.CALL, emptyList()))
        )

        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("0xb36574155395D41b92664e7A215103262a14244A").subscribe(observer)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.safe_already_exists.toString())))

        then(safeRepoMock).should().loadSafe(TEST_RECOVERING_SAFE)
        then(safeRepoMock).should().loadPendingSafe(TEST_RECOVERING_SAFE)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_RECOVERING_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeUnknownErrorCheckingExistence() {
        contextMock.mockGetString()
        given(safeRepoMock.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadPendingSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        val error = IllegalStateException()
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("0xb36574155395D41b92664e7A215103262a14244A").subscribe(observer)
        observer.assertResult(ErrorResult(error))

        then(safeRepoMock).should().loadSafe(TEST_RECOVERING_SAFE)
        then(safeRepoMock).should().loadPendingSafe(TEST_RECOVERING_SAFE)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_RECOVERING_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeNetworkError() {
        contextMock.mockGetString()
        given(safeRepoMock.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadPendingSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.checkSafe(MockUtils.any())).willReturn(Observable.error(UnknownHostException()))

        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("0x1f81fff89Bd57811983a35650296681f99C65C7E").subscribe(observer)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.error_check_internet_connection.toString())))

        then(safeRepoMock).should().loadSafe(TEST_SAFE)
        then(safeRepoMock).should().loadPendingSafe(TEST_SAFE)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).should().checkSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeInvalidSafe() {
        contextMock.mockGetString()
        given(safeRepoMock.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadPendingSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.checkSafe(MockUtils.any())).willReturn(Observable.just(false))

        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("0x1f81fff89Bd57811983a35650296681f99C65C7E").subscribe(observer)
        observer.assertResult(DataResult(false))

        then(safeRepoMock).should().loadSafe(TEST_SAFE)
        then(safeRepoMock).should().loadPendingSafe(TEST_SAFE)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).should().checkSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun checkSafeValidSafe() {
        contextMock.mockGetString()
        given(safeRepoMock.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadPendingSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.loadRecoveringSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepoMock.checkSafe(MockUtils.any())).willReturn(Observable.just(true))

        val observer = TestObserver<Result<Boolean>>()
        viewModel.checkSafe("0x1f81fff89Bd57811983a35650296681f99C65C7E").subscribe(observer)
        observer.assertResult(DataResult(true))

        then(safeRepoMock).should().loadSafe(TEST_SAFE)
        then(safeRepoMock).should().loadPendingSafe(TEST_SAFE)
        then(safeRepoMock).should().loadRecoveringSafe(TEST_SAFE)
        then(safeRepoMock).should().checkSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_TX_HASH = "0xdae721569a948b87c269ebacaa5a4a67728095e32f9e7e4626f109f27a73b40f".hexAsBigInteger()
        private val TEST_PENDING_SAFE = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_RECOVERING_SAFE = "0xb36574155395D41b92664e7A215103262a14244A".asEthereumAddress()!!
        private val TEST_PAYMENT_TOKEN = ERC20Token.ETHER_TOKEN.address
        private val TEST_PAYMENT_AMOUNT = Wei.ether("0.1").value
    }
}
