package pm.gnosis.heimdall.ui.safe.pending

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.ethereum.EthBalance
import pm.gnosis.ethereum.EthRequest
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import java.util.concurrent.TimeUnit


@RunWith(MockitoJUnitRunner::class)
class PendingSafeViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var gnosisSafeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var ethereumRepositoryMock: EthereumRepository

    private lateinit var viewModel: PendingSafeViewModel

    @Before
    fun setup() {
        viewModel = PendingSafeViewModel(gnosisSafeRepositoryMock, ethereumRepositoryMock)
    }

    @Test
    fun observePendingSafe() {
        val transactionHash = "1"
        val testObserver = TestObserver.create<PendingSafe>()
        val pendingSafe = PendingSafe(BigInteger.ONE, "", Solidity.Address(BigInteger.ZERO), Wei.ZERO)
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))

        viewModel.setup(transactionHash)
        viewModel.observePendingSafe().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(transactionHash.hexAsBigInteger())
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(pendingSafe)

        assertEquals(
            pendingSafe.hash,
            viewModel.getTransactionHash()
        )
    }

    @Test
    fun observePendingSafeError() {
        val transactionHash = "1"
        val testObserver = TestObserver.create<PendingSafe>()
        val exception = Exception()
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.error(exception))

        viewModel.setup(transactionHash)
        viewModel.observePendingSafe().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(transactionHash.hexAsBigInteger())
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(Exception::class.java)
    }

    @Test
    fun observeHashEnoughDeployBalance() {
        val transactionHash = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(BigInteger.ONE, "", Solidity.Address(BigInteger.ZERO), Wei(1.toBigInteger()))
        val ethBalance = EthBalance(Solidity.Address(BigInteger.TEN)).apply {
            response = EthRequest.Response.Success(Wei(100.toBigInteger()))
        }
        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(ethereumRepositoryMock.request(MockUtils.any<EthBalance>())).willReturn(Observable.just(ethBalance))
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(transactionHash)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(transactionHash.hexAsBigInteger())
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        testObserver.assertResult(Unit)
    }

    @Test
    fun observeHasEnoughDeployBalanceNotEnoughFunds() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }
        val transactionHash = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(BigInteger.ONE, "", Solidity.Address(BigInteger.TEN), Wei(1.toBigInteger()))
        val ethBalance = EthBalance(Solidity.Address(BigInteger.TEN))
        var enoughBalance = false

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(ethereumRepositoryMock.request(MockUtils.any<EthBalance>())).willReturn(Observable.fromCallable {
            ethBalance.apply {
                response = EthRequest.Response.Success(
                    if (enoughBalance) Wei(2.toBigInteger())
                    else Wei.ZERO
                )
            }
        })
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(transactionHash)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        testObserver.assertEmpty()
        enoughBalance = true
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        testObserver.assertResult(Unit)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(transactionHash.hexAsBigInteger())
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceErrorUpdatingDb() {
        val transactionHash = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(BigInteger.ONE, "", Solidity.Address(BigInteger.TEN), Wei(1.toBigInteger()))
        val ethBalance = EthBalance(Solidity.Address(BigInteger.TEN))
        val exception = Exception()

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(ethereumRepositoryMock.request(MockUtils.any<EthBalance>())).willReturn(Observable.just(ethBalance.apply {
            response = EthRequest.Response.Success(Wei(100.toBigInteger()))
        }))
        given(gnosisSafeRepositoryMock.updatePendingSafe(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.setup(transactionHash)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertError(exception)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(transactionHash.hexAsBigInteger())
        then(gnosisSafeRepositoryMock).should().updatePendingSafe(pendingSafe.copy(isFunded = true))
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployBalanceRequestError() {
        val transactionHash = "1"
        val testObserver = TestObserver.create<Unit>()
        val pendingSafe = PendingSafe(BigInteger.ONE, "", Solidity.Address(BigInteger.TEN), Wei(1.toBigInteger()))
        val exception = Exception()

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.just(pendingSafe))
        given(ethereumRepositoryMock.request(MockUtils.any<EthBalance>())).willReturn(Observable.error(exception))

        viewModel.setup(transactionHash)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertError(exception)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(transactionHash.hexAsBigInteger())
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeHasEnoughDeployObservePendingSafeError() {
        val transactionHash = "1"
        val testObserver = TestObserver.create<Unit>()
        val exception = Exception()

        given(gnosisSafeRepositoryMock.observePendingSafe(MockUtils.any())).willReturn(Flowable.error(exception))

        viewModel.setup(transactionHash)
        viewModel.observeHasEnoughDeployBalance().subscribe(testObserver)

        testObserver.assertError(exception)

        then(gnosisSafeRepositoryMock).should().observePendingSafe(transactionHash.hexAsBigInteger())
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
    }
}
