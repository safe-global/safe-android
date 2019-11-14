package pm.gnosis.heimdall.ui.transactions.view.details

import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionViewModel
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class MultiSendDetailsViewModelTest {

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var transactionInfoRepositoryMock: TransactionInfoRepository

    private lateinit var viewModel: MultiSendDetailsViewModel

    @Before
    fun setUp() {
        viewModel = MultiSendDetailsViewModel(tokenRepositoryMock, transactionInfoRepositoryMock)
    }

    @Test
    fun loadTransactionData() {
        given(transactionInfoRepositoryMock.parseTransactionData(MockUtils.any())).willReturn(Single.just(TEST_TRANSACTION_DATA))
        val testObserver = TestObserver<TransactionData>()
        viewModel.loadTransactionData(TEST_TRANSACTION).subscribe(testObserver)
        testObserver.assertResult(TEST_TRANSACTION_DATA)
        then(transactionInfoRepositoryMock).should().parseTransactionData(TEST_TRANSACTION)
        then(transactionInfoRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadTransactionDataError() {
        val error = IllegalArgumentException("Test Exception")
        given(transactionInfoRepositoryMock.parseTransactionData(MockUtils.any())).willReturn(Single.error(error))
        val testObserver = TestObserver<TransactionData>()
        viewModel.loadTransactionData(TEST_TRANSACTION).subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == error })
        then(transactionInfoRepositoryMock).should().parseTransactionData(TEST_TRANSACTION)
        then(transactionInfoRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadTokenInfo() {
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.just(TEST_TOKEN))
        val testObserver = TestObserver<ERC20Token>()
        viewModel.loadTokenInfo(TEST_TOKEN_ADDRESS).subscribe(testObserver)
        testObserver.assertResult(TEST_TOKEN)
        then(tokenRepositoryMock).should().loadToken(TEST_TOKEN_ADDRESS)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionInfoRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadTokenInfoError() {
        val error = IllegalArgumentException("Test Exception")
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.error(error))
        val testObserver = TestObserver<ERC20Token>()
        viewModel.loadTokenInfo(TEST_TOKEN_ADDRESS).subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == error })
        then(tokenRepositoryMock).should().loadToken(TEST_TOKEN_ADDRESS)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionInfoRepositoryMock).shouldHaveZeroInteractions()
    }

    companion object {
        private val TEST_TOKEN_ADDRESS = "0x31b98d14007bDEE637298986988A0bbd31184523".asEthereumAddress()!!
        private val TEST_TOKEN = ERC20Token(TEST_TOKEN_ADDRESS, "Test Token", "TT", 0)
        private val TEST_TRANSACTION_DATA =
            TransactionData.Generic("0x0".asEthereumAddress()!!, BigInteger.ZERO, null)
        private val TEST_TRANSACTION =
            SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO)), TransactionExecutionRepository.Operation.CALL)
    }
}
