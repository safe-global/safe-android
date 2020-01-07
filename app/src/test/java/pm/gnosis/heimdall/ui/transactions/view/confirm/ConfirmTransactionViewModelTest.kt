package pm.gnosis.heimdall.ui.transactions.view.confirm

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.RestrictedTransactionException
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class ConfirmTransactionViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var relayRepositoryMock: TransactionExecutionRepository

    @Mock
    private lateinit var txRepositoryMock: TransactionInfoRepository

    @Mock
    private lateinit var submitTransactionHelper: SubmitTransactionHelper

    private lateinit var viewModel: ConfirmTransactionViewModel

    @Before
    fun setUp() {
        viewModel = ConfirmTransactionViewModel(submitTransactionHelper, txRepositoryMock, relayRepositoryMock)
    }

    @Test
    fun setup() {
        var executionInfo: ((SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>)? = null
        given(submitTransactionHelper.setup(MockUtils.any(), MockUtils.any(), MockUtils.any())).will {
            executionInfo = it.arguments[1] as ((SafeTransaction) -> Single<TransactionExecutionRepository.ExecuteInformation>)?
            Unit
        }
        viewModel.setup(
            TEST_SAFE, TEST_TRANSACTION_HASH,
            TEST_OPERATIONAL_GAS, TEST_DATA_GAS, TEST_TX_GAS, TEST_GAS_TOKEN, TEST_GAS_PRICE,
            TEST_NONCE, TEST_SIGNATURE
        )

        then(submitTransactionHelper).should().setup(TEST_SAFE, executionInfo!!)
        then(submitTransactionHelper).shouldHaveNoMoreInteractions()

        val info = TransactionExecutionRepository.SafeExecuteState(
            TEST_OWNERS[2], TEST_OWNERS.size - 1,
            TEST_OWNERS, BigInteger.ONE, Wei.ether("23").value,
            SemVer(1, 0, 0)
        )
        given(relayRepositoryMock.loadSafeExecuteState(MockUtils.any(), MockUtils.any())).willReturn(Single.just(info))

        val updatedTransaction = TEST_TRANSACTION.copy(wrapped = TEST_TRANSACTION.wrapped.copy(nonce = TEST_NONCE))

        // Invalid Hash
        given(relayRepositoryMock.calculateHash(
            MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())
        )
            .willReturn(Single.just("randomHash".toByteArray()))

        val invalidHashObserver = TestObserver<TransactionExecutionRepository.ExecuteInformation>()
        executionInfo!!.invoke(TEST_TRANSACTION).subscribe(invalidHashObserver)

        invalidHashObserver.assertFailure(IllegalStateException::class.java)
        then(relayRepositoryMock).should()
            .calculateHash(TEST_SAFE, updatedTransaction, TEST_TX_GAS, TEST_DATA_GAS, TEST_GAS_PRICE, TEST_GAS_TOKEN, TEST_VERSION)
        then(relayRepositoryMock).should()
            .loadSafeExecuteState(TEST_SAFE, TEST_GAS_TOKEN)
        then(relayRepositoryMock).shouldHaveNoMoreInteractions()

        // Valid Hash
        given(relayRepositoryMock.calculateHash(
            MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())
        )
            .willReturn(Single.just(TEST_TRANSACTION_HASH.hexToByteArray()))

        val validHashObserver = TestObserver<TransactionExecutionRepository.ExecuteInformation>()
        executionInfo!!.invoke(TEST_TRANSACTION).subscribe(validHashObserver)

        validHashObserver.assertValues(
            TransactionExecutionRepository.ExecuteInformation(
                TEST_TRANSACTION_HASH, updatedTransaction, TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
                TEST_GAS_TOKEN, TEST_GAS_PRICE, TEST_TX_GAS, TEST_DATA_GAS, TEST_OPERATIONAL_GAS, Wei.ether("23").value
            )
        ).assertNoErrors().assertComplete()
        then(relayRepositoryMock).should(times(2))
            .calculateHash(TEST_SAFE, updatedTransaction, TEST_TX_GAS, TEST_DATA_GAS, TEST_GAS_PRICE, TEST_GAS_TOKEN, TEST_VERSION)
        then(relayRepositoryMock).should().loadSafeExecuteState(TEST_SAFE, TEST_GAS_TOKEN)
        then(relayRepositoryMock).shouldHaveNoMoreInteractions()

        // Used cached
        val cachedHashObserver = TestObserver<TransactionExecutionRepository.ExecuteInformation>()
        executionInfo!!.invoke(TEST_TRANSACTION).subscribe(cachedHashObserver)

        cachedHashObserver.assertValues(
            TransactionExecutionRepository.ExecuteInformation(
                TEST_TRANSACTION_HASH, updatedTransaction, TEST_OWNERS[2], TEST_OWNERS.size - 1, TEST_OWNERS, TEST_VERSION,
                TEST_GAS_TOKEN, TEST_GAS_PRICE, TEST_TX_GAS, TEST_DATA_GAS, TEST_OPERATIONAL_GAS, Wei.ether("23").value
            )
        ).assertNoErrors().assertComplete()
        then(relayRepositoryMock).should(times(3))
            .calculateHash(TEST_SAFE, updatedTransaction, TEST_TX_GAS, TEST_DATA_GAS, TEST_GAS_PRICE, TEST_GAS_TOKEN, TEST_VERSION)
        then(relayRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observe() {
        val events = SubmitTransactionHelper.Events(Observable.empty(), Observable.empty(), Observable.empty())
        val transactionData = mock(TransactionData::class.java)
        val observable = Observable.empty<Result<SubmitTransactionHelper.ViewUpdate>>()
        given(txRepositoryMock.checkRestrictedTransaction(MockUtils.any(), MockUtils.any())).willReturn(Single.just(TEST_TRANSACTION))
        given(txRepositoryMock.parseTransactionData(MockUtils.any())).willReturn(Single.just(transactionData))
        given(submitTransactionHelper.observe(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(observable)

        val observer = TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>()

        viewModel.setup(
            TEST_SAFE, TEST_TRANSACTION_HASH,
            TEST_OPERATIONAL_GAS, TEST_DATA_GAS, TEST_TX_GAS, TEST_GAS_TOKEN, TEST_GAS_PRICE,
            TEST_NONCE, TEST_SIGNATURE
        )
        viewModel.observe(events, TEST_TRANSACTION).subscribe(observer)

        observer.assertNoValues().assertNoErrors().assertComplete()

        then(submitTransactionHelper).should().setup(MockUtils.any(), MockUtils.any(), MockUtils.any())
        then(submitTransactionHelper).should().observe(events, transactionData, setOf(TEST_SIGNATURE))
        then(submitTransactionHelper).shouldHaveNoMoreInteractions()

        then(relayRepositoryMock).shouldHaveZeroInteractions()

        then(txRepositoryMock).should().checkRestrictedTransaction(TEST_SAFE, TEST_TRANSACTION)
        then(txRepositoryMock).should().parseTransactionData(TEST_TRANSACTION)
        then(txRepositoryMock).shouldHaveNoMoreInteractions()
    }

    private fun testRestrictedTransaction(exception: RestrictedTransactionException, expected: Exception) {
        reset(submitTransactionHelper)
        reset(relayRepositoryMock)
        reset(txRepositoryMock)

        val events = SubmitTransactionHelper.Events(Observable.empty(), Observable.empty(), Observable.empty())
        given(txRepositoryMock.checkRestrictedTransaction(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        val observer = TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>()

        viewModel.setup(
            TEST_SAFE, TEST_TRANSACTION_HASH,
            TEST_OPERATIONAL_GAS, TEST_DATA_GAS, TEST_TX_GAS, TEST_GAS_TOKEN, TEST_GAS_PRICE,
            TEST_NONCE, TEST_SIGNATURE
        )
        viewModel.observe(events, TEST_TRANSACTION).subscribe(observer)

        observer.assertError(expected).assertNoValues()

        then(submitTransactionHelper).should().setup(MockUtils.any(), MockUtils.any(), MockUtils.any())
        then(submitTransactionHelper).shouldHaveNoMoreInteractions()

        then(relayRepositoryMock).shouldHaveNoMoreInteractions()

        then(txRepositoryMock).should().checkRestrictedTransaction(TEST_SAFE, TEST_TRANSACTION)
        then(txRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeRestrictedTransaction() {
        val testData = mapOf(
            RestrictedTransactionException.DelegateCall::class to
                    (RestrictedTransactionException.DelegateCall to
                            ConfirmTransactionContract.InvalidTransactionException(R.string.restricted_transaction_delegatecall)),
            RestrictedTransactionException.ModifyOwners::class to
                    (RestrictedTransactionException.ModifyOwners to
                            ConfirmTransactionContract.InvalidTransactionException(R.string.restricted_transaction_modify_signers)),
            RestrictedTransactionException.ModifyModules::class to
                    (RestrictedTransactionException.ModifyModules to
                            ConfirmTransactionContract.InvalidTransactionException(R.string.restricted_transaction_modify_modules)),
            RestrictedTransactionException.ChangeThreshold::class to
                    (RestrictedTransactionException.ChangeThreshold to
                            ConfirmTransactionContract.InvalidTransactionException(R.string.restricted_transaction_change_threshold)),
            RestrictedTransactionException.ChangeMasterCopy::class to
                    (RestrictedTransactionException.ChangeMasterCopy to
                            ConfirmTransactionContract.InvalidTransactionException(R.string.restricted_transaction_modify_proxy)),
            RestrictedTransactionException.SetFallbackHandler::class to
                    (RestrictedTransactionException.SetFallbackHandler to
                            ConfirmTransactionContract.InvalidTransactionException(R.string.restricted_transaction_set_fallback_handler)),
            RestrictedTransactionException.DataCallToSafe::class to
                    (RestrictedTransactionException.DataCallToSafe to
                            ConfirmTransactionContract.InvalidTransactionException(R.string.restricted_transaction_data_call_to_safe))
        )

        RestrictedTransactionException::class.nestedClasses.forEach {
            val (exception, expected) = testData[it] ?: throw IllegalStateException("Missing test for ${it.simpleName}")
            testRestrictedTransaction(exception, expected)
        }
    }

    @Test
    fun observeInvalidTransaction() {
        val events = SubmitTransactionHelper.Events(Observable.empty(), Observable.empty(), Observable.empty())
        val error = IllegalArgumentException()
        given(txRepositoryMock.checkRestrictedTransaction(MockUtils.any(), MockUtils.any())).willReturn(Single.just(TEST_TRANSACTION))
        given(txRepositoryMock.parseTransactionData(MockUtils.any())).willReturn(Single.error(error))

        val observer = TestObserver<Result<SubmitTransactionHelper.ViewUpdate>>()

        viewModel.setup(
            TEST_SAFE, TEST_TRANSACTION_HASH,
            TEST_OPERATIONAL_GAS, TEST_DATA_GAS, TEST_TX_GAS, TEST_GAS_TOKEN, TEST_GAS_PRICE,
            TEST_NONCE, TEST_SIGNATURE
        )
        viewModel.observe(events, TEST_TRANSACTION).subscribe(observer)

        observer.assertError(error).assertNoValues()

        then(submitTransactionHelper).should().setup(MockUtils.any(), MockUtils.any(), MockUtils.any())
        then(submitTransactionHelper).shouldHaveNoMoreInteractions()

        then(relayRepositoryMock).shouldHaveNoMoreInteractions()

        then(txRepositoryMock).should().checkRestrictedTransaction(TEST_SAFE, TEST_TRANSACTION)
        then(txRepositoryMock).should().parseTransactionData(TEST_TRANSACTION)
        then(txRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun rejectTransaction() {
        viewModel.setup(
            TEST_SAFE, TEST_TRANSACTION_HASH,
            TEST_OPERATIONAL_GAS, TEST_DATA_GAS, TEST_TX_GAS, TEST_GAS_TOKEN, TEST_GAS_PRICE,
            TEST_NONCE, TEST_SIGNATURE
        )

        val info = TransactionExecutionRepository.SafeExecuteState(
            TEST_OWNERS[2], TEST_OWNERS.size - 1,
            TEST_OWNERS, BigInteger.ONE, Wei.ether("23").value,
            TEST_VERSION
        )
        given(relayRepositoryMock.loadSafeExecuteState(MockUtils.any(), MockUtils.any())).willReturn(Single.just(info))
        given(
            relayRepositoryMock.notifyReject(
                MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()
            )
        )
            .willReturn(Completable.complete())

        val rejectObserver = TestObserver<Unit>()
        viewModel.rejectTransaction(TEST_TRANSACTION).subscribe(rejectObserver)
        rejectObserver.assertNoErrors().assertComplete()

        then(relayRepositoryMock).should().loadSafeExecuteState(TEST_SAFE, TEST_GAS_TOKEN)
        then(relayRepositoryMock).should().notifyReject(
            TEST_SAFE, TEST_TRANSACTION, TEST_TX_GAS, TEST_DATA_GAS, TEST_GAS_PRICE, TEST_GAS_TOKEN,
            (TEST_OWNERS - TEST_OWNERS[2]).toSet(), TEST_VERSION
        )
        then(relayRepositoryMock).shouldHaveNoMoreInteractions()

        val cachedObserver = TestObserver<Unit>()
        viewModel.rejectTransaction(TEST_TRANSACTION).subscribe(cachedObserver)
        cachedObserver.assertNoErrors().assertComplete()
        then(relayRepositoryMock).should(times(2)).notifyReject(
            TEST_SAFE, TEST_TRANSACTION, TEST_TX_GAS, TEST_DATA_GAS, TEST_GAS_PRICE, TEST_GAS_TOKEN,
            (TEST_OWNERS - TEST_OWNERS[2]).toSet(), TEST_VERSION
        )
        then(relayRepositoryMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private const val TEST_TRANSACTION_HASH = "0x255ed2f7cbd18dfdccbd729cf78297c1bd2943cd62c16bcacefb4c792d082322"
        private val TEST_TRANSACTION =
            SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO)), TransactionExecutionRepository.Operation.CALL)
        private val TEST_SIGNERS = listOf(BigInteger.valueOf(7), BigInteger.valueOf(13)).map { Solidity.Address(it) }
        private val TEST_OWNERS = TEST_SIGNERS + Solidity.Address(BigInteger.valueOf(5))
        private val TEST_OPERATIONAL_GAS = BigInteger.valueOf(12345)
        private val TEST_DATA_GAS = BigInteger.valueOf(1234)
        private val TEST_TX_GAS = BigInteger.valueOf(4321)
        private val TEST_GAS_TOKEN = "0x0".asEthereumAddress()!!
        private val TEST_GAS_PRICE = BigInteger.valueOf(987654)
        private val TEST_NONCE = BigInteger.valueOf(23)
        private val TEST_SIGNATURE = Signature(BigInteger.valueOf(11), BigInteger.valueOf(5), 27)
        private val TEST_VERSION = SemVer(1, 0, 0)
    }
}
