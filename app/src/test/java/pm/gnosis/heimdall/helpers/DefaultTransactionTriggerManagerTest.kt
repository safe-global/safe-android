package pm.gnosis.heimdall.helpers

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.reset
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.function.Consumer

@RunWith(MockitoJUnitRunner::class)
class DefaultTransactionTriggerManagerTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var pushServiceRepositoryMock: PushServiceRepository

    @Mock
    lateinit var transactionExecutionRepositoryMock: TransactionExecutionRepository

    @Mock
    lateinit var transactionInfoRepositoryMock: TransactionInfoRepository

    private lateinit var manager: DefaultTransactionTriggerManager

    @Before
    fun setUp() {
        manager = DefaultTransactionTriggerManager(pushServiceRepositoryMock, transactionExecutionRepositoryMock, transactionInfoRepositoryMock)
    }

    @Test
    fun init() {
        manager.init()
        then(transactionExecutionRepositoryMock).should().addTransactionEventsCallback(manager)
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(transactionInfoRepositoryMock).shouldHaveZeroInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()
    }

    private fun testTransactionSubmitted(data: SubmittedTestData) {
        given(transactionInfoRepositoryMock.parseTransactionData(MockUtils.any())).willReturn(Single.just(data.data))

        data.pushRepoSetup.accept(pushServiceRepositoryMock)

        val transaction = SafeTransaction(Transaction(TEST_SAFE), TransactionExecutionRepository.Operation.CALL)
        manager.onTransactionSubmitted(TEST_SAFE, transaction, "SomeHash", null)

        data.pushRepoCheck.accept(pushServiceRepositoryMock)
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        then(transactionExecutionRepositoryMock).shouldHaveZeroInteractions()
        then(transactionInfoRepositoryMock).should().parseTransactionData(transaction)
        then(transactionInfoRepositoryMock).shouldHaveNoMoreInteractions()
        reset(transactionExecutionRepositoryMock)
        reset(transactionInfoRepositoryMock)
        reset(pushServiceRepositoryMock)
    }

    @Test
    fun onTransactionSubmitted() {
        TransactionData::class.nestedClasses.filter { it != TransactionData.Companion::class }.forEach {
            testTransactionSubmitted(SUCCESS_TEST_DATA[it] ?: throw IllegalStateException("Missing test for ${it.simpleName}"))
        }
    }

    data class SubmittedTestData(
        val data: TransactionData,
        val pushRepoSetup: Consumer<PushServiceRepository> = Consumer { },
        val pushRepoCheck: Consumer<PushServiceRepository> = Consumer { }
    )

    companion object {
        private val TEST_SAFE = Solidity.Address(BigInteger.ONE)
        private val TEST_ADDRESS = "0x42".asEthereumAddress()!!

        private val REPLACE_RECOVERY_PHRASE_TX =
            SafeTransaction(Transaction(address = TEST_SAFE), TransactionExecutionRepository.Operation.DELEGATE_CALL)

        private val SUCCESS_TEST_DATA = mapOf(
            TransactionData.Generic::class to SubmittedTestData(TransactionData.Generic(TEST_SAFE, BigInteger.ONE, null)),
            TransactionData.AssetTransfer::class to SubmittedTestData(
                TransactionData.AssetTransfer(
                    TEST_SAFE,
                    BigInteger.ONE,
                    Solidity.Address(BigInteger.TEN)
                )
            ),
            TransactionData.ReplaceRecoveryPhrase::class to SubmittedTestData(TransactionData.ReplaceRecoveryPhrase(REPLACE_RECOVERY_PHRASE_TX)),
            TransactionData.ConnectAuthenticator::class to SubmittedTestData(
                TransactionData.ConnectAuthenticator(TEST_ADDRESS),
                pushRepoSetup = Consumer { given(it.propagateSafeCreation(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete()) },
                pushRepoCheck = Consumer { then(it).should().propagateSafeCreation(TEST_SAFE, setOf(TEST_ADDRESS)) }
            ),
            TransactionData.UpdateMasterCopy::class to SubmittedTestData(
                TransactionData.UpdateMasterCopy(TEST_ADDRESS)
            ),
            TransactionData.MultiSend::class to SubmittedTestData(
                TransactionData.MultiSend(emptyList(), TEST_ADDRESS)
            )
        )
    }
}
