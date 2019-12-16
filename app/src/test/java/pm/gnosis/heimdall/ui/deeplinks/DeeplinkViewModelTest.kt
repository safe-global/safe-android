package pm.gnosis.heimdall.ui.deeplinks

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.reactivex.Flowable
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.RestrictedTransactionException
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.Operation.CALL
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestLifecycleRule
import pm.gnosis.tests.utils.TestLiveDataObserver
import pm.gnosis.tests.utils.testAppDispatchers
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class DeeplinkViewModelTest {

    @JvmField
    @Rule
    val lifecycleRule = TestLifecycleRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var deeplinkTransactionParser: DeeplinkTransactionParser

    @Mock
    lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    lateinit var transactionRepositoryMock: TransactionInfoRepository

    private lateinit var viewModel: DeeplinkViewModel

    private val initialState = DeeplinkContract.State(false, Adapter.Data(), null)

    @Before
    fun setUp() {
        viewModel = DeeplinkViewModel(contextMock, testAppDispatchers, safeRepositoryMock, transactionRepositoryMock, deeplinkTransactionParser)
    }

    @Test
    fun observeWithoutSetup() {
        val testValues = mutableListOf<DeeplinkContract.State>()
        viewModel.state.observe(lifecycleRule, Observer { testValues.add(it) })
        assertEquals(
            initialState.copy(
                loading = true,
                viewAction = DeeplinkContract.CloseScreenWithError(SimpleLocalizedException("Invalid transaction in deeplink"))
            ),
            testValues.last()
        )
    }

    @Test
    fun observeNoSafes() {
        val testValues = mutableListOf<DeeplinkContract.State>()
        val link = "ethereum://0xc257274276a4e539741ca11b590b9447b26a8051?value=0x1&data=0xdeadbeef"
        viewModel.setup(link)

        val safeTransaction = SafeTransaction(Transaction(TEST_ADDRESS, Wei(BigInteger.ONE), data = "0xdeadbeef"), CALL)
        given(deeplinkTransactionParser.parse(MockUtils.any()))
            .willReturn(
                safeTransaction to null
            )

        given(transactionRepositoryMock.parseTransactionData(MockUtils.any()))
            .willReturn(
                Single.just(
                    TransactionData.Generic(
                        TEST_ADDRESS,
                        BigInteger.ONE,
                        "0xdeadbeef",
                        CALL
                    )
                )
            )

        given(safeRepositoryMock.observeSafes())
            .willReturn(Flowable.just(listOf()))

        viewModel.state.observe(lifecycleRule, Observer { testValues.add(it) })
        assertEquals(
            initialState.copy(
                loading = true,
                viewAction = DeeplinkContract.CloseScreenWithError(SimpleLocalizedException("No Safe available"))
            ),
            testValues.last()
        )

        then(deeplinkTransactionParser).should().parse(link)
        then(deeplinkTransactionParser).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().parseTransactionData(safeTransaction)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(safeRepositoryMock).should().observeSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSingleSafesRestrictedTx() {
        val testValues = mutableListOf<DeeplinkContract.State>()
        val link = "ethereum://0xc257274276a4e539741ca11b590b9447b26a8051?value=0x1&data=0xdeadbeef?referrer=some_referrer"
        viewModel.setup(link)

        given(transactionRepositoryMock.checkRestrictedTransaction(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(RestrictedTransactionException.DataCallToSafe))

        val safeTransaction = SafeTransaction(Transaction(TEST_ADDRESS, Wei(BigInteger.ONE), data = "0xdeadbeef"), CALL)
        given(deeplinkTransactionParser.parse(MockUtils.any()))
            .willReturn(
                safeTransaction to null
            )

        val transactionData = TransactionData.Generic(
            TEST_ADDRESS,
            BigInteger.ONE,
            "0xdeadbeef",
            CALL
        )
        given(transactionRepositoryMock.parseTransactionData(MockUtils.any()))
            .willReturn(
                Single.just(
                    transactionData
                )
            )

        given(safeRepositoryMock.observeSafes())
            .willReturn(Flowable.just(listOf(Safe(TEST_SAFE)), listOf()))

        viewModel.state.observe(lifecycleRule, Observer { testValues.add(it) })
        assertEquals(
            initialState.copy(
                loading = false,
                viewAction = DeeplinkContract.CloseScreenWithError(SimpleLocalizedException("Interaction with the Safe are not allowed"))
            ),
            testValues.last()
        )

        then(deeplinkTransactionParser).should().parse(link)
        then(deeplinkTransactionParser).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().checkRestrictedTransaction(TEST_SAFE, safeTransaction)
        then(transactionRepositoryMock).should().parseTransactionData(safeTransaction)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(safeRepositoryMock).should().observeSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSingleSafesCheckError() {
        val testValues = mutableListOf<DeeplinkContract.State>()
        val link = "ethereum://0xc257274276a4e539741ca11b590b9447b26a8051?value=0x1&data=0xdeadbeef?referrer=some_referrer"
        viewModel.setup(link)

        given(transactionRepositoryMock.checkRestrictedTransaction(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(IllegalStateException()))

        val safeTransaction = SafeTransaction(Transaction(TEST_ADDRESS, Wei(BigInteger.ONE), data = "0xdeadbeef"), CALL)
        given(deeplinkTransactionParser.parse(MockUtils.any()))
            .willReturn(
                safeTransaction to null
            )

        val transactionData = TransactionData.Generic(
            TEST_ADDRESS,
            BigInteger.ONE,
            "0xdeadbeef",
            CALL
        )
        given(transactionRepositoryMock.parseTransactionData(MockUtils.any()))
            .willReturn(
                Single.just(
                    transactionData
                )
            )

        given(safeRepositoryMock.observeSafes())
            .willReturn(Flowable.just(listOf(Safe(TEST_SAFE)), listOf()))

        viewModel.state.observe(lifecycleRule, Observer { testValues.add(it) })
        assertEquals(
            initialState.copy(
                loading = false,
                viewAction = DeeplinkContract.CloseScreenWithError(SimpleLocalizedException("It is not allowed to perform this transaction"))
            ),
            testValues.last()
        )

        then(deeplinkTransactionParser).should().parse(link)
        then(deeplinkTransactionParser).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().checkRestrictedTransaction(TEST_SAFE, safeTransaction)
        then(transactionRepositoryMock).should().parseTransactionData(safeTransaction)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(safeRepositoryMock).should().observeSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSingleSafes() {

        val testValues = mutableListOf<DeeplinkContract.State>()
        val link = "ethereum://0xc257274276a4e539741ca11b590b9447b26a8051?value=0x1&data=0xdeadbeef?referrer=some_referrer"
        viewModel.setup(link)

        val safeTransaction = SafeTransaction(Transaction(TEST_ADDRESS, Wei(BigInteger.ONE), data = "0xdeadbeef"), CALL)
        given(deeplinkTransactionParser.parse(MockUtils.any()))
            .willReturn(
                safeTransaction to null
            )
        given(transactionRepositoryMock.checkRestrictedTransaction(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(safeTransaction))

        val transactionData = TransactionData.Generic(
            TEST_ADDRESS,
            BigInteger.ONE,
            "0xdeadbeef",
            CALL
        )
        given(transactionRepositoryMock.parseTransactionData(MockUtils.any()))
            .willReturn(
                Single.just(
                    transactionData
                )
            )

        val safes = listOf(Safe(TEST_SAFE))
        given(safeRepositoryMock.observeSafes())
            .willReturn(Flowable.just(safes, listOf()))

        viewModel.state.observe(lifecycleRule, Observer { testValues.add(it) })

        val state = testValues.last()
        assertEquals(state.loading, false)
        assertTrue(state.viewAction is BaseStateViewModel.ViewAction.StartActivity)

        then(deeplinkTransactionParser).should().parse(link)
        then(deeplinkTransactionParser).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().checkRestrictedTransaction(TEST_SAFE, safeTransaction)
        then(transactionRepositoryMock).should().parseTransactionData(safeTransaction)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(safeRepositoryMock).should().observeSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeManySafes() {
        val lifecycleObserver = mutableListOf<LifecycleEventObserver>()
        val lifecycle = Mockito.mock(Lifecycle::class.java)
        given(lifecycle.currentState).willReturn(Lifecycle.State.STARTED)
        given(lifecycle.addObserver(MockUtils.any())).willAnswer {
            lifecycleObserver.add(it.arguments.first() as LifecycleEventObserver)
            Unit
        }
        val lifecycleOwner = Mockito.mock(LifecycleOwner::class.java)
        given(lifecycleOwner.lifecycle).willReturn(lifecycle)

        val testValues = mutableListOf<DeeplinkContract.State>()
        val link = "ethereum://0xc257274276a4e539741ca11b590b9447b26a8051?value=0x1&data=0xdeadbeef?referrer=some_referrer"
        viewModel.setup(link)

        val safeTransaction = SafeTransaction(Transaction(TEST_ADDRESS, Wei(BigInteger.ONE), data = "0xdeadbeef"), CALL)
        given(deeplinkTransactionParser.parse(MockUtils.any()))
            .willReturn(
                safeTransaction to null
            )
        given(transactionRepositoryMock.checkRestrictedTransaction(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(safeTransaction))

        val transactionData = TransactionData.Generic(
            TEST_ADDRESS,
            BigInteger.ONE,
            "0xdeadbeef",
            CALL
        )
        given(transactionRepositoryMock.parseTransactionData(MockUtils.any()))
            .willReturn(
                Single.just(
                    transactionData
                )
            )

        val safes = listOf(Safe(TEST_SAFE), Safe(TEST_ADDRESS))
        given(safeRepositoryMock.observeSafes())
            .willReturn(Flowable.just(safes, listOf()))

        viewModel.state.observe(lifecycleOwner, Observer { testValues.add(it) })
        assertTrue(testValues.isEmpty())

        // Get observer and trigger onStart
        lifecycleObserver.forEach { it.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_START) }

        assertEquals(initialState.copy(loading = false, safes = Adapter.Data(entries = safes)), testValues.last())

        then(deeplinkTransactionParser).should().parse(link)
        then(deeplinkTransactionParser).shouldHaveNoMoreInteractions()

        then(transactionRepositoryMock).should().parseTransactionData(safeTransaction)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()

        then(safeRepositoryMock).should().observeSafes()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()

        viewModel.selectSafe(TEST_SAFE)
        val state = testValues.last()
        assertEquals(state.loading, false)
        assertTrue(state.viewAction is BaseStateViewModel.ViewAction.StartActivity)

        then(transactionRepositoryMock).should().checkRestrictedTransaction(TEST_SAFE, safeTransaction)
        then(transactionRepositoryMock).shouldHaveNoMoreInteractions()
        then(deeplinkTransactionParser).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_ADDRESS = "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!
        private val TEST_SAFE = "0xa7e15e2e76ab469f8681b576cff168f37aa246ec".asEthereumAddress()!!
    }
}