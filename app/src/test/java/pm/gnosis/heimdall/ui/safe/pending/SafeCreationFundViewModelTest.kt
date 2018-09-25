package pm.gnosis.heimdall.ui.safe.pending

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestSingleFactory
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit


@RunWith(MockitoJUnitRunner::class)
class SafeCreationFundViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var gnosisSafeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: SafeCreationFundViewModel

    @Before
    fun setup() {
        viewModel = SafeCreationFundViewModel(contextMock, gnosisSafeRepositoryMock, tokenRepositoryMock)
    }

    @Test
    fun observeCreationInfo() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Result<SafeCreationFundContract.CreationInfo>>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.ZERO), BigInteger.ONE, ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        val tokenTestSingle = TestSingleFactory<ERC20Token>()
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(tokenTestSingle.get())

        viewModel.setup(safeAddress)
        viewModel.observeCreationInfo().subscribe(testObserver)

        testObserver.assertValues(
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    null,
                    BigInteger.ONE
                )
            )
        )

        tokenTestSingle.success(ERC20Token.ETHER_TOKEN)
        testObserver.assertResult(
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    null,
                    BigInteger.ONE
                )
            ),
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    ERC20Token.ETHER_TOKEN,
                    BigInteger.ONE
                )
            )
        )

        then(tokenRepositoryMock).should().loadToken(ERC20Token.ETHER_TOKEN.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeCreationInfoTokenInfoError() {
        contextMock.mockGetString()
        val safeAddress = "1"
        val testObserver = TestObserver.create<Result<SafeCreationFundContract.CreationInfo>>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.ZERO), BigInteger.ONE, ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        val tokenTestSingle = TestSingleFactory<ERC20Token>()
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(tokenTestSingle.get())

        viewModel.setup(safeAddress)
        viewModel.observeCreationInfo().subscribe(testObserver)

        testObserver.assertValues(
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    null,
                    BigInteger.ONE
                )
            )
        )

        tokenTestSingle.error(UnknownHostException())
        testObserver.assertResult(
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    null,
                    BigInteger.ONE
                )
            ),
            ErrorResult(SimpleLocalizedException(R.string.error_check_internet_connection.toString()))
        )

        then(tokenRepositoryMock).should().loadToken(ERC20Token.ETHER_TOKEN.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeCreationInfoDbError() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Result<SafeCreationFundContract.CreationInfo>>()
        val exception = Exception()
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.error(exception))

        viewModel.setup(safeAddress)
        viewModel.observeCreationInfo().subscribe(testObserver)

        testObserver.assertFailure(Exception::class.java)

        then(tokenRepositoryMock).shouldHaveZeroInteractions()
        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHashEnoughDeployBalance() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.ZERO), BigInteger.ONE, ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.just(listOf(ERC20Token.ETHER_TOKEN to BigInteger.valueOf(100))))
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        testObserver.assertResult(Unit)
    }

    @Test
    fun observeHasEnoughDeployBalanceNotEnoughFunds() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), BigInteger.ONE, ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        var enoughBalance = false

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.fromCallable {
                listOf(ERC20Token.ETHER_TOKEN to if (enoughBalance) 2.toBigInteger() else BigInteger.ZERO)
            })
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        testObserver.assertEmpty()
        enoughBalance = true
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testObserver.assertResult(Unit)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceNoBalance() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), BigInteger.ONE, ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        var enoughBalance = false

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.fromCallable {
                listOf(ERC20Token.ETHER_TOKEN to if (enoughBalance) 2.toBigInteger() else null)
            })
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        testObserver.assertEmpty()
        enoughBalance = true
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testObserver.assertResult(Unit)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceErrorUpdatingDb() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), BigInteger.ONE, ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        val exception = Exception()

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.just(listOf(ERC20Token.ETHER_TOKEN to BigInteger.valueOf(100))))
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertError(exception).assertNoValues()

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceRequestError() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), BigInteger.ONE, ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        val exception = Exception()

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.error(exception))

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertError(exception).assertNoValues()

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceRequestInvalidResponse() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), BigInteger.ONE, ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.just(emptyList()))

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertFailure(NoSuchElementException::class.java)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployObserveCreationInfoError() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val exception = Exception()

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.error(exception))

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertError(exception)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }
}
