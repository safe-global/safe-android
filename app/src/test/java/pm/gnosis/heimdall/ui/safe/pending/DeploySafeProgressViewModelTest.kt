package pm.gnosis.heimdall.ui.safe.pending

import io.reactivex.Completable
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
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.RelaySafeFundStatus
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.model.Solidity
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class DeploySafeProgressViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var gnosisSafeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var relayServiceApiMock: RelayServiceApi

    private lateinit var viewModel: DeploySafeProgressViewModel

    @Before
    fun setup() {
        viewModel = DeploySafeProgressViewModel(gnosisSafeRepositoryMock, relayServiceApiMock)
    }

    @Test
    fun notifySafeFunded() {
        val testObserver = TestObserver<Solidity.Address>()
        val txHash = BigInteger.TEN
        val safeAddress = Solidity.Address(123.toBigInteger())
        val pendingSafe = PendingSafe(safeAddress, txHash, ERC20Token.ETHER_TOKEN.address, BigInteger.ZERO)
        given(gnosisSafeRepositoryMock.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))
        given(relayServiceApiMock.notifySafeFunded(MockUtils.any())).willReturn(Completable.complete())
        given(relayServiceApiMock.safeFundStatus(MockUtils.any())).willReturn(
            Single.just(
                RelaySafeFundStatus(true, true, "", true, "")
            )
        )
        given(gnosisSafeRepositoryMock.pendingSafeToDeployedSafe(pendingSafe)).willReturn(Completable.complete())

        viewModel.setup(safeAddress)
        viewModel.notifySafeFunded().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().loadPendingSafe(safeAddress)
        then(relayServiceApiMock).should().notifySafeFunded(safeAddress.asEthereumAddressChecksumString())
        then(relayServiceApiMock).should().safeFundStatus(safeAddress.asEthereumAddressChecksumString())
        then(gnosisSafeRepositoryMock).should().pendingSafeToDeployedSafe(pendingSafe)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(safeAddress)
    }

    @Test
    fun notifySafeFundedSafeNotDeployed() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }
        val testObserver = TestObserver<Solidity.Address>()
        val txHash = BigInteger.TEN
        val safeAddress = Solidity.Address(123.toBigInteger())
        val pendingSafe = PendingSafe(safeAddress, txHash, ERC20Token.ETHER_TOKEN.address, BigInteger.ZERO)
        var safeDeployed = false
        given(gnosisSafeRepositoryMock.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))
        given(relayServiceApiMock.notifySafeFunded(MockUtils.any())).willReturn(Completable.complete())
        given(relayServiceApiMock.safeFundStatus(MockUtils.any())).willReturn(
            Single.fromCallable { RelaySafeFundStatus(true, true, "", safeDeployed, "") }
        )
        given(gnosisSafeRepositoryMock.pendingSafeToDeployedSafe(pendingSafe)).willReturn(Completable.complete())

        viewModel.setup(safeAddress)
        viewModel.notifySafeFunded().subscribe(testObserver)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        testObserver.assertEmpty()
        safeDeployed = true
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        testObserver.assertResult(safeAddress)

        then(gnosisSafeRepositoryMock).should().loadPendingSafe(safeAddress)
        then(relayServiceApiMock).should().notifySafeFunded(safeAddress.asEthereumAddressChecksumString())
        then(relayServiceApiMock).should().safeFundStatus(safeAddress.asEthereumAddressChecksumString())
        then(gnosisSafeRepositoryMock).should().pendingSafeToDeployedSafe(pendingSafe)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun notifySafeFundedSafeDbUpdateError() {
        val testObserver = TestObserver<Solidity.Address>()
        val txHash = BigInteger.TEN
        val safeAddress = Solidity.Address(123.toBigInteger())
        val pendingSafe = PendingSafe(safeAddress, txHash, ERC20Token.ETHER_TOKEN.address, BigInteger.ZERO)
        val exception = Exception()
        given(gnosisSafeRepositoryMock.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))
        given(relayServiceApiMock.notifySafeFunded(MockUtils.any())).willReturn(Completable.complete())
        given(relayServiceApiMock.safeFundStatus(MockUtils.any())).willReturn(
            Single.fromCallable { RelaySafeFundStatus(true, true, "", true, "") }
        )
        given(gnosisSafeRepositoryMock.pendingSafeToDeployedSafe(pendingSafe)).willReturn(Completable.error(exception))

        viewModel.setup(safeAddress)
        viewModel.notifySafeFunded().subscribe(testObserver)

        testObserver.assertError(exception)

        then(gnosisSafeRepositoryMock).should().loadPendingSafe(safeAddress)
        then(relayServiceApiMock).should().notifySafeFunded(safeAddress.asEthereumAddressChecksumString())
        then(relayServiceApiMock).should().safeFundStatus(safeAddress.asEthereumAddressChecksumString())
        then(gnosisSafeRepositoryMock).should().pendingSafeToDeployedSafe(pendingSafe)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun notifySafeFundedSafeFundStatusError() {
        val testObserver = TestObserver<Solidity.Address>()
        val txHash = BigInteger.TEN
        val safeAddress = Solidity.Address(123.toBigInteger())
        val pendingSafe = PendingSafe(safeAddress, txHash, ERC20Token.ETHER_TOKEN.address, BigInteger.ZERO)
        val exception = Exception()
        given(gnosisSafeRepositoryMock.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))
        given(relayServiceApiMock.notifySafeFunded(MockUtils.any())).willReturn(Completable.complete())
        given(relayServiceApiMock.safeFundStatus(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setup(safeAddress)
        viewModel.notifySafeFunded().subscribe(testObserver)

        testObserver.assertError(exception)

        then(gnosisSafeRepositoryMock).should().loadPendingSafe(safeAddress)
        then(relayServiceApiMock).should().notifySafeFunded(safeAddress.asEthereumAddressChecksumString())
        then(relayServiceApiMock).should().safeFundStatus(safeAddress.asEthereumAddressChecksumString())
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun notifySafeFundedNotifyError() {
        val testObserver = TestObserver<Solidity.Address>()
        val txHash = BigInteger.TEN
        val safeAddress = Solidity.Address(123.toBigInteger())
        val pendingSafe = PendingSafe(safeAddress, txHash, ERC20Token.ETHER_TOKEN.address, BigInteger.ZERO)
        val exception = Exception()
        given(gnosisSafeRepositoryMock.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))
        given(relayServiceApiMock.notifySafeFunded(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.setup(safeAddress)
        viewModel.notifySafeFunded().subscribe(testObserver)

        testObserver.assertError(exception)

        then(gnosisSafeRepositoryMock).should().loadPendingSafe(safeAddress)
        then(relayServiceApiMock).should().notifySafeFunded(safeAddress.asEthereumAddressChecksumString())
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun notifySafeFundedLoadPendingSafeError() {
        val testObserver = TestObserver<Solidity.Address>()
        val safeAddress = Solidity.Address(123.toBigInteger())
        val exception = Exception()
        given(gnosisSafeRepositoryMock.loadPendingSafe(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setup(safeAddress)
        viewModel.notifySafeFunded().subscribe(testObserver)

        testObserver.assertError(exception)

        then(gnosisSafeRepositoryMock).should().loadPendingSafe(safeAddress)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
    }
}
