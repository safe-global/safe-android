package pm.gnosis.heimdall.ui.safe.pending

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
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
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
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
    private lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var qrGeneratorMock: QrCodeGenerator

    @Mock
    private lateinit var safeQrCodeMock: Bitmap

    private lateinit var viewModel: SafeCreationFundViewModel

    @Before
    fun setup() {
        viewModel = SafeCreationFundViewModel(contextMock, gnosisSafeRepositoryMock, tokenRepositoryMock, ethereumRepositoryMock, qrGeneratorMock)
    }

    @Test
    fun observeCreationInfo() {
        val safeAddress = Solidity.Address(BigInteger.ZERO).asEthereumAddressChecksumString()
        val testObserver = TestObserver.create<Result<SafeCreationFundContract.CreationInfo>>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.ZERO), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(ethereumRepositoryMock.getBalance(Solidity.Address(BigInteger.ZERO))).willReturn(Observable.just(Wei.ZERO))
        val tokenTestSingle = TestSingleFactory<ERC20Token>()
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(tokenTestSingle.get())
        given(qrGeneratorMock.generateQrCode(safeAddress)).willReturn(Single.just(safeQrCodeMock))

        viewModel.setup(safeAddress)
        viewModel.observeCreationInfo().subscribe(testObserver)

        testObserver.assertValues(
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    null,
                    BigInteger.ONE,
                    null
                )
            )
        )

        tokenTestSingle.success(ERC20Token.ETHER_TOKEN)
        testObserver.assertResult(
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    null,
                    BigInteger.ONE,
                    null
                )
            ),
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, BigInteger.ZERO),
                    BigInteger.ONE,
                    safeQrCodeMock
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
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.ZERO), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
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
                    BigInteger.ONE,
                    null
                )
            )
        )

        tokenTestSingle.error(UnknownHostException())
        testObserver.assertResult(
            DataResult(
                SafeCreationFundContract.CreationInfo(
                    pendingSafe.address.asEthereumAddressChecksumString(),
                    null,
                    BigInteger.ONE,
                    null
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
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.ZERO), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(gnosisSafeRepositoryMock.checkSafe(MockUtils.any())).willReturn(Observable.just(null to false))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.just(listOf(ERC20Token.ETHER_TOKEN to BigInteger.valueOf(100))))
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertResult(Unit)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).should().checkSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadTokenBalances(
            pendingSafe.address,
            listOf(ERC20Token(pendingSafe.paymentToken, decimals = 0, name = "", symbol = ""))
        )
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceNotEnoughFunds() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        var enoughBalance = false

        given(gnosisSafeRepositoryMock.checkSafe(MockUtils.any())).willReturn(Observable.just(null to false))
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
        then(gnosisSafeRepositoryMock).should().checkSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadTokenBalances(
            pendingSafe.address,
            listOf(ERC20Token(pendingSafe.paymentToken, decimals = 0, name = "", symbol = ""))
        )
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceNoBalance() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        var enoughBalance = false

        given(gnosisSafeRepositoryMock.checkSafe(MockUtils.any())).willReturn(Observable.just(null to false))
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
        then(gnosisSafeRepositoryMock).should().checkSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadTokenBalances(
            pendingSafe.address,
            listOf(ERC20Token(pendingSafe.paymentToken, decimals = 0, name = "", symbol = ""))
        )
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceErrorUpdatingDb() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        val exception = Exception()

        given(gnosisSafeRepositoryMock.checkSafe(MockUtils.any())).willReturn(Observable.just(null to false))
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.just(listOf(ERC20Token.ETHER_TOKEN to BigInteger.valueOf(100))))
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertError(exception).assertNoValues()

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).should().checkSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadTokenBalances(
            pendingSafe.address,
            listOf(ERC20Token(pendingSafe.paymentToken, decimals = 0, name = "", symbol = ""))
        )
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceRequestError() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.ONE), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        var shouldThrow = true
        val exception = Exception()

        given(gnosisSafeRepositoryMock.checkSafe(MockUtils.any())).willReturn(Observable.just(null to false))
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.fromCallable {
                if (shouldThrow) throw exception
                listOf(ERC20Token.ETHER_TOKEN to BigInteger.valueOf(100))
            })

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        testObserver.assertEmpty()
        shouldThrow = false
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testObserver.assertResult(Unit)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().checkSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadTokenBalances(
            safeAddress.asEthereumAddress()!!,
            listOf(ERC20Token(pendingSafe.paymentToken, decimals = 0, name = "", symbol = ""))
        )
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceRequestInvalidResponse() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        var shouldReturnInvalid = true

        given(gnosisSafeRepositoryMock.checkSafe(MockUtils.any())).willReturn(Observable.just(null to false))
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any()))
            .willReturn(Observable.fromCallable {
                if (shouldReturnInvalid) emptyList()
                else listOf(ERC20Token.ETHER_TOKEN to BigInteger.valueOf(100))
            })

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        testObserver.assertEmpty()
        shouldReturnInvalid = false
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testObserver.assertResult(Unit)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().checkSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadTokenBalances(
            pendingSafe.address,
            listOf(ERC20Token(pendingSafe.paymentToken, decimals = 0, name = "", symbol = ""))
        )
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceAlreadyDeployed() {
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.TEN), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)

        given(gnosisSafeRepositoryMock.checkSafe(MockUtils.any())).willReturn(Observable.just(SafeContractUtils.currentMasterCopy() to false))
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(gnosisSafeRepositoryMock.pendingSafeToDeployedSafe(MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertResult(Unit)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().checkSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().pendingSafeToDeployedSafe(pendingSafe)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun observeHasEnoughDeployCheckRequestError() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        val safeAddress = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(Solidity.Address(BigInteger.ONE), ERC20Token.ETHER_TOKEN.address, BigInteger.ONE)
        var shouldThrow = true
        val exception = Exception()

        given(gnosisSafeRepositoryMock.checkSafe(MockUtils.any()))
            .willReturn(Observable.fromCallable {
                if (shouldThrow) throw exception
                SafeContractUtils.currentMasterCopy() to true
            })
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(gnosisSafeRepositoryMock.pendingSafeToDeployedSafe(MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(safeAddress)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        testObserver.assertEmpty()
        shouldThrow = false
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testObserver.assertResult(Unit)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().checkSafe(safeAddress.asEthereumAddress()!!)
        then(gnosisSafeRepositoryMock).should().pendingSafeToDeployedSafe(pendingSafe)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
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
