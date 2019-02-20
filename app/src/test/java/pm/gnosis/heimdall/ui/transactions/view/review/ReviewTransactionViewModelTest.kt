package pm.gnosis.heimdall.ui.transactions.view.review

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class ReviewTransactionViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var relayRepositoryMock: TransactionExecutionRepository

    @Mock
    private lateinit var submitTransactionHelper: SubmitTransactionHelper

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: ReviewTransactionViewModel

    @Before
    fun setUp() {
        viewModel = ReviewTransactionViewModel(submitTransactionHelper, relayRepositoryMock, tokenRepositoryMock)
    }

    @Test
    fun setup() {
        var executionInfo: ((SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>)? = null
        given(submitTransactionHelper.setup(MockUtils.any(), MockUtils.any(), MockUtils.any())).will {
            executionInfo = it.arguments[1] as ((SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>)?
            Unit
        }
        viewModel.setup(TEST_SAFE, null)

        then(submitTransactionHelper).should().setup(TEST_SAFE, executionInfo!!)
        then(submitTransactionHelper).shouldHaveNoMoreInteractions()

        val info = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH,
            TEST_TRANSACTION, TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS,
            TEST_GAS_TOKEN, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO,
            Wei.ether("23").value
        )
        given(tokenRepositoryMock.loadPaymentToken()).willReturn(Single.just(ERC20Token(TEST_GAS_TOKEN, "I neeeeed some GAS", "INSG", 18)))
        given(relayRepositoryMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Single.just(info))
        executionInfo!!.invoke(TEST_TRANSACTION).subscribe()
        then(relayRepositoryMock).should().loadExecuteInformation(TEST_SAFE, TEST_GAS_TOKEN, TEST_TRANSACTION)
        then(relayRepositoryMock).shouldHaveNoMoreInteractions()
        // Used cached
        executionInfo!!.invoke(TEST_TRANSACTION).subscribe()
        then(relayRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observe() {
        val events = SubmitTransactionHelper.Events(Observable.empty(), Observable.empty(), Observable.empty())
        val transactionData = mock(TransactionData::class.java)
        val observable = Observable.empty<Result<SubmitTransactionHelper.ViewUpdate>>()
        given(submitTransactionHelper.observe(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(observable)
        assertEquals(observable, viewModel.observe(events, transactionData))
        then(submitTransactionHelper).should().observe(events, transactionData)
        then(submitTransactionHelper).shouldHaveNoMoreInteractions()
        then(relayRepositoryMock).shouldHaveZeroInteractions()
    }

    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private const val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_TRANSACTION =
            SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.TEN), TransactionExecutionRepository.Operation.CALL)
        private val TEST_SIGNERS = listOf(BigInteger.valueOf(7), BigInteger.valueOf(13)).map { Solidity.Address(it) }
        private val TEST_OWNERS = TEST_SIGNERS + Solidity.Address(BigInteger.valueOf(5))
        private val TEST_GAS_TOKEN = "0xdeadbeef".asEthereumAddress()!!
    }
}
