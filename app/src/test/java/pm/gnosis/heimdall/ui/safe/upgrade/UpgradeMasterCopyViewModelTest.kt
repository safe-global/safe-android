package pm.gnosis.heimdall.ui.safe.upgrade

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.base.BaseStateViewModel.ViewAction.ShowError
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class UpgradeMasterCopyViewModelTest {

    @JvmField
    @Rule
    val lifecycleRule = TestLifecycleRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var addressBookRepositoryMock: AddressBookRepository

    @Mock
    lateinit var executionRepositoryMock: TransactionExecutionRepository

    @Mock
    lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: UpgradeMasterCopyViewModel

    @Before
    fun setup() {
        viewModel =
            UpgradeMasterCopyViewModel(
                contextMock,
                testAppDispatchers,
                addressBookRepositoryMock,
                executionRepositoryMock,
                safeRepositoryMock,
                tokenRepositoryMock
            )
    }

    private val initialState = UpgradeMasterCopyContract.State(null, null, null, null, null, false, false, null)

    @Test
    fun observe() {
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_SAFE, "Flash Gordon", "")))

        viewModel.setup(TEST_SAFE)
        val lifecycleObserver = mutableListOf<LifecycleEventObserver>()
        val lifecycle = mock(Lifecycle::class.java)
        given(lifecycle.currentState).willReturn(Lifecycle.State.STARTED)
        given(lifecycle.addObserver(MockUtils.any())).willAnswer {
            lifecycleObserver.add(it.arguments.first() as LifecycleEventObserver)
            Unit
        }
        val lifecycleOwner = mock(LifecycleOwner::class.java)
        given(lifecycleOwner.lifecycle).willReturn(lifecycle)
        // Start observing
        val testValues = mutableListOf<UpgradeMasterCopyContract.State>()
        viewModel.observe(lifecycleOwner) { testValues.add(it) }
        then(lifecycle).should(times(2)).addObserver(MockUtils.any())
        assertTrue(testValues.isEmpty())

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any()))
            .willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(safeRepositoryMock.checkSafe(MockUtils.any()))
            .willReturn(Observable.just("0xAC6072986E985aaBE7804695EC2d8970Cf7541A2".asEthereumAddress()!! to true))
        given(executionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(
                Single.just(
                    TransactionExecutionRepository.ExecuteInformation(
                        TEST_TRANSACTION_HASH,
                        TEST_TRANSACTION,
                        TEST_OWNERS[2],
                        TEST_OWNERS.size,
                        TEST_OWNERS,
                        SemVer(1, 0, 0),
                        ERC20Token.ETHER_TOKEN.address,
                        BigInteger.ONE,
                        BigInteger.TEN,
                        BigInteger.ZERO,
                        BigInteger.ZERO,
                        BigInteger.valueOf(100)
                    )
                )
            )
        // Get observer and trigger onStart
        lifecycleObserver.forEach { it.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START) }

        assertEquals(4, testValues.size)
        var currentState = initialState.copy(safeName = "Flash Gordon")
        assertEquals(currentState, testValues[0])
        currentState = currentState.copy(loading = true)
        assertEquals(currentState, testValues[1])
        currentState = currentState.copy(feeToken = ERC20Token.ETHER_TOKEN)
        assertEquals(currentState, testValues[2])
        currentState = currentState.copy(
            loading = false,
            fees = BigInteger.TEN,
            safeBalance = BigInteger.valueOf(100),
            safeBalanceAfterTx = BigInteger.valueOf(90),
            showFeeError = false
        )
        assertEquals(currentState, testValues[3])
    }

    @Test
    fun observeLoading() {
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_SAFE, "Flash Gordon", "")))

        viewModel.setup(TEST_SAFE)
        val lifecycleObserver = mutableListOf<LifecycleEventObserver>()
        val lifecycle = mock(Lifecycle::class.java)
        given(lifecycle.currentState).willReturn(Lifecycle.State.STARTED)
        given(lifecycle.addObserver(MockUtils.any())).willAnswer {
            lifecycleObserver.add(it.arguments.first() as LifecycleEventObserver)
            Unit
        }
        val lifecycleOwner = mock(LifecycleOwner::class.java)
        given(lifecycleOwner.lifecycle).willReturn(lifecycle)
        // Start observing
        val testValues = mutableListOf<UpgradeMasterCopyContract.State>()
        viewModel.observe(lifecycleOwner) { testValues.add(it) }
        then(lifecycle).should(times(2)).addObserver(MockUtils.any())
        assertTrue(testValues.isEmpty())

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any()))
            .willReturn(TestSingleFactory<ERC20Token>().get())
        // Get observer and trigger onStart
        lifecycleObserver.forEach { it.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START) }

        assertEquals(2, testValues.size)
        var currentState = initialState.copy(safeName = "Flash Gordon")
        assertEquals(currentState, testValues[0])
        currentState = currentState.copy(loading = true)
        assertEquals(currentState, testValues[1])

        then(tokenRepositoryMock).should().loadPaymentToken(TEST_SAFE)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

        viewModel.loadEstimates()
        assertEquals(2, testValues.size)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeLoadPaymentTokenError() {
        contextMock.mockGetString()
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_SAFE, "Flash Gordon", "")))

        viewModel.setup(TEST_SAFE)
        val lifecycleObserver = mutableListOf<LifecycleEventObserver>()
        val lifecycle = mock(Lifecycle::class.java)
        given(lifecycle.currentState).willReturn(Lifecycle.State.STARTED)
        given(lifecycle.addObserver(MockUtils.any())).willAnswer {
            lifecycleObserver.add(it.arguments.first() as LifecycleEventObserver)
            Unit
        }
        val lifecycleOwner = mock(LifecycleOwner::class.java)
        given(lifecycleOwner.lifecycle).willReturn(lifecycle)
        // Start observing
        val testValues = mutableListOf<UpgradeMasterCopyContract.State>()
        viewModel.observe(lifecycleOwner) { testValues.add(it) }
        then(lifecycle).should(times(2)).addObserver(MockUtils.any())
        assertTrue(testValues.isEmpty())

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any()))
            .willReturn(Single.error(UnknownHostException()))
        // Get observer and trigger onStart
        lifecycleObserver.forEach { it.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START) }

        assertEquals(4, testValues.size)
        var currentState = initialState.copy(safeName = "Flash Gordon")
        assertEquals(currentState, testValues[0])
        currentState = currentState.copy(loading = true)
        assertEquals(currentState, testValues[1])
        currentState =
            currentState.copy(loading = false, viewAction = ShowError(SimpleLocalizedException(R.string.error_check_internet_connection.toString())))
        assertEquals(currentState, testValues[2])
        assertEquals(currentState, testValues[3])
    }

    @Test
    fun observeCheckSafeError() {
        contextMock.mockGetString()
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_SAFE, "Flash Gordon", "")))

        viewModel.setup(TEST_SAFE)
        val lifecycleObserver = mutableListOf<LifecycleEventObserver>()
        val lifecycle = mock(Lifecycle::class.java)
        given(lifecycle.currentState).willReturn(Lifecycle.State.STARTED)
        given(lifecycle.addObserver(MockUtils.any())).willAnswer {
            lifecycleObserver.add(it.arguments.first() as LifecycleEventObserver)
            Unit
        }
        val lifecycleOwner = mock(LifecycleOwner::class.java)
        given(lifecycleOwner.lifecycle).willReturn(lifecycle)
        // Start observing
        val testValues = mutableListOf<UpgradeMasterCopyContract.State>()
        viewModel.observe(lifecycleOwner) { testValues.add(it) }
        then(lifecycle).should(times(2)).addObserver(MockUtils.any())
        assertTrue(testValues.isEmpty())

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any()))
            .willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(safeRepositoryMock.checkSafe(MockUtils.any()))
            .willReturn(Observable.error(UnknownHostException()))
        // Get observer and trigger onStart
        lifecycleObserver.forEach { it.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START) }

        assertEquals(5, testValues.size)
        var currentState = initialState.copy(safeName = "Flash Gordon")
        assertEquals(currentState, testValues[0])
        currentState = currentState.copy(loading = true)
        assertEquals(currentState, testValues[1])
        currentState = currentState.copy(feeToken = ERC20Token.ETHER_TOKEN)
        assertEquals(currentState, testValues[2])
        currentState =
            currentState.copy(loading = false, viewAction = ShowError(SimpleLocalizedException(R.string.error_check_internet_connection.toString())))
        assertEquals(currentState, testValues[3])
        assertEquals(currentState, testValues[4])
    }

    @Test
    fun observeLoadExecuteInformationError() {
        contextMock.mockGetString()
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_SAFE, "Flash Gordon", "")))

        viewModel.setup(TEST_SAFE)
        val lifecycleObserver = mutableListOf<LifecycleEventObserver>()
        val lifecycle = mock(Lifecycle::class.java)
        given(lifecycle.currentState).willReturn(Lifecycle.State.STARTED)
        given(lifecycle.addObserver(MockUtils.any())).willAnswer {
            lifecycleObserver.add(it.arguments.first() as LifecycleEventObserver)
            Unit
        }
        val lifecycleOwner = mock(LifecycleOwner::class.java)
        given(lifecycleOwner.lifecycle).willReturn(lifecycle)
        // Start observing
        val testValues = mutableListOf<UpgradeMasterCopyContract.State>()
        viewModel.observe(lifecycleOwner) { testValues.add(it) }
        then(lifecycle).should(times(2)).addObserver(MockUtils.any())
        assertTrue(testValues.isEmpty())

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any()))
            .willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(safeRepositoryMock.checkSafe(MockUtils.any()))
            .willReturn(Observable.just("0xAC6072986E985aaBE7804695EC2d8970Cf7541A2".asEthereumAddress()!! to true))
        given(executionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(UnknownHostException()))
        // Get observer and trigger onStart
        lifecycleObserver.forEach { it.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START) }

        assertEquals(5, testValues.size)
        var currentState = initialState.copy(safeName = "Flash Gordon")
        assertEquals(currentState, testValues[0])
        currentState = currentState.copy(loading = true)
        assertEquals(currentState, testValues[1])
        currentState = currentState.copy(feeToken = ERC20Token.ETHER_TOKEN)
        assertEquals(currentState, testValues[2])
        currentState =
            currentState.copy(loading = false, viewAction = ShowError(SimpleLocalizedException(R.string.error_check_internet_connection.toString())))
        assertEquals(currentState, testValues[3])
        assertEquals(currentState, testValues[4])
    }

    @Test
    fun observeNotEnoughBalance() {
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(AddressBookEntry(TEST_SAFE, "Flash Gordon", "")))

        viewModel.setup(TEST_SAFE)
        val lifecycleObserver = mutableListOf<LifecycleEventObserver>()
        val lifecycle = mock(Lifecycle::class.java)
        given(lifecycle.currentState).willReturn(Lifecycle.State.STARTED)
        given(lifecycle.addObserver(MockUtils.any())).willAnswer {
            lifecycleObserver.add(it.arguments.first() as LifecycleEventObserver)
            Unit
        }
        val lifecycleOwner = mock(LifecycleOwner::class.java)
        given(lifecycleOwner.lifecycle).willReturn(lifecycle)
        // Start observing
        val testValues = mutableListOf<UpgradeMasterCopyContract.State>()
        viewModel.observe(lifecycleOwner) { testValues.add(it) }
        then(lifecycle).should(times(2)).addObserver(MockUtils.any())
        assertTrue(testValues.isEmpty())

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any()))
            .willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(safeRepositoryMock.checkSafe(MockUtils.any()))
            .willReturn(Observable.just(TEST_MASTER_COPY to true))
        given(executionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(
                Single.just(
                    TransactionExecutionRepository.ExecuteInformation(
                        TEST_TRANSACTION_HASH,
                        TEST_TRANSACTION,
                        TEST_OWNERS[2],
                        TEST_OWNERS.size,
                        TEST_OWNERS,
                        SemVer(1, 0, 0),
                        ERC20Token.ETHER_TOKEN.address,
                        BigInteger.ONE,
                        BigInteger.TEN,
                        BigInteger.ZERO,
                        BigInteger.ZERO,
                        BigInteger.ZERO
                    )
                )
            )
        // Get observer and trigger onStart
        lifecycleObserver.forEach { it.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START) }

        assertEquals(4, testValues.size)
        var currentState = initialState.copy(safeName = "Flash Gordon")
        assertEquals(currentState, testValues[0])
        currentState = currentState.copy(loading = true)
        assertEquals(currentState, testValues[1])
        currentState = currentState.copy(feeToken = ERC20Token.ETHER_TOKEN)
        assertEquals(currentState, testValues[2])
        currentState = currentState.copy(
            loading = false,
            fees = BigInteger.TEN,
            safeBalance = BigInteger.ZERO,
            safeBalanceAfterTx = BigInteger.valueOf(-10),
            showFeeError = true
        )
        assertEquals(currentState, testValues[3])
    }

    @Test
    fun next() {
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any()))
            .willReturn(TestSingleFactory<AddressBookEntry>().get())
        viewModel.setup(TEST_SAFE)
        val stateObserver = TestLiveDataObserver<UpgradeMasterCopyContract.State>()
        viewModel.state.observeForever(stateObserver)
        stateObserver
            .assertValueCount(1)
            .assertValueAt(0) { assertNull(it.viewAction) }

        viewModel.next()
        // Need to call estimate before
        stateObserver
            .assertValueCount(1)

        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any())).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(safeRepositoryMock.checkSafe(MockUtils.any())).willReturn(Observable.just(TEST_MASTER_COPY to false))
        viewModel.loadEstimates()

        stateObserver
            .assertValueCount(5)

        viewModel.next()
        // Needs successful estimate
        stateObserver
            .assertValueCount(5)

        given(executionRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(
                Single.just(
                    TransactionExecutionRepository.ExecuteInformation(
                        TEST_TRANSACTION_HASH,
                        TEST_TRANSACTION,
                        TEST_OWNERS[2],
                        TEST_OWNERS.size,
                        TEST_OWNERS,
                        SemVer(1, 0, 0),
                        ERC20Token.ETHER_TOKEN.address,
                        BigInteger.ONE,
                        BigInteger.TEN,
                        BigInteger.ZERO,
                        BigInteger.ZERO,
                        BigInteger.ZERO
                    )
                )
            )

        viewModel.loadEstimates()

        stateObserver
            .assertValueCount(8)
        viewModel.next()

        stateObserver
            .assertValueCount(9)
            .assertValueAt(8) { assertTrue(it.viewAction is BaseStateViewModel.ViewAction.StartActivity) }

        then(addressBookRepositoryMock).should().loadAddressBookEntry(TEST_SAFE)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun changeFeeToken() {
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any()))
            .willReturn(TestSingleFactory<AddressBookEntry>().get())
        viewModel.setup(TEST_SAFE)
        val stateObserver = TestLiveDataObserver<UpgradeMasterCopyContract.State>()
        viewModel.state.observeForever(stateObserver)
        stateObserver
            .assertValueCount(1)
            .assertValueAt(0) { assertNull(it.viewAction) }

        viewModel.changeFeeToken()
        stateObserver
            .assertValueCount(2)
            .assertValueAt(1) { assertTrue(it.viewAction is BaseStateViewModel.ViewAction.StartActivity) }

        then(addressBookRepositoryMock).should().loadAddressBookEntry(TEST_SAFE)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private const val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_TRANSACTION =
            SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.TEN), TransactionExecutionRepository.Operation.CALL)
        private val TEST_SIGNERS = listOf(BigInteger.valueOf(7), BigInteger.valueOf(13)).map { Solidity.Address(it) }
        private val TEST_OWNERS = TEST_SIGNERS + Solidity.Address(BigInteger.valueOf(5))
        private val TEST_MASTER_COPY = "0xAC6072986E985aaBE7804695EC2d8970Cf7541A2".asEthereumAddress()!!
    }
}
