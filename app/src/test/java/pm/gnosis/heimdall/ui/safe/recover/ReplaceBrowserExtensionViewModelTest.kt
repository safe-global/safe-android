package pm.gnosis.heimdall.ui.safe.recover

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class ReplaceBrowserExtensionViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var gnosisSafeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var pushServiceRepositoryMock: PushServiceRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var transactionExecutionRepositoryMock: TransactionExecutionRepository

    private lateinit var viewModel: ReplaceBrowserExtensionViewModel

    @Before
    fun setUp() {
        viewModel = ReplaceBrowserExtensionViewModel(
            accountsRepositoryMock,
            gnosisSafeRepositoryMock,
            pushServiceRepositoryMock,
            tokenRepositoryMock,
            transactionExecutionRepositoryMock
        )
    }

    @Test
    fun getMaxTransactionFee() {
        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        val maxTransactionFee = viewModel.getMaxTransactionFee().balance!!
        assertEquals(11000000.toBigInteger(), maxTransactionFee)
    }

    @Test
    fun observeSafeBalance() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }

        val testObserver = TestObserver.create<Result<ERC20TokenWithBalance>>()
        val safeAddress = SAFE_TRANSACTION.wrapped.address
        val token = listOf(ERC20Token.ETHER_TOKEN)
        val tokenBalance = ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, 10.toBigInteger())
        var shouldErrorOnFetchingBalance = false

        given(
            tokenRepositoryMock.loadTokenBalances(
                safeAddress,
                token
            )
        ).willAnswer {
            Observable.fromCallable {
                if (shouldErrorOnFetchingBalance) emptyList()
                else listOf(ERC20Token.ETHER_TOKEN to 10.toBigInteger())
            }
        }

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.observeSafeBalance().subscribe(testObserver)

        // First successful emission
        testScheduler.advanceTimeBy(0, TimeUnit.SECONDS)
        then(tokenRepositoryMock).should().loadTokenBalances(safeAddress, token)
        testObserver.assertValueAt(0, DataResult(tokenBalance))

        // Second emission with error
        shouldErrorOnFetchingBalance = true
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        then(tokenRepositoryMock).should(times(2)).loadTokenBalances(safeAddress, token)
        testObserver.assertValueAt(1) { it is ErrorResult && it.error is ReplaceBrowserExtensionContract.NoTokenBalanceException }

        // Third successful emission
        shouldErrorOnFetchingBalance = false
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        then(tokenRepositoryMock).should(times(3)).loadTokenBalances(safeAddress, token)
        testObserver.assertValueAt(2, DataResult(tokenBalance))
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertValueCount(3)
    }

    @Test
    fun observeSafeBalanceError() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }

        val testObserver = TestObserver.create<Result<ERC20TokenWithBalance>>()
        val safeAddress = SAFE_TRANSACTION.wrapped.address
        val token = listOf(ERC20Token.ETHER_TOKEN)
        val exception = Exception()

        given(
            tokenRepositoryMock.loadTokenBalances(
                safeAddress,
                token
            )
        ).willReturn(Observable.error(exception))

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.observeSafeBalance().subscribe(testObserver)

        testScheduler.advanceTimeBy(0, TimeUnit.SECONDS)
        testObserver.assertValue(ErrorResult(exception))
            .assertNoErrors()
            .assertNotTerminated()
        then(tokenRepositoryMock).should().loadTokenBalances(safeAddress, token)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun getSafeTransaction() {
        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        assertEquals(SAFE_TRANSACTION, viewModel.getSafeTransaction())
    }

    @Test
    fun loadSafe() {
        val testObserver = TestObserver<Safe>()
        val safe = Safe(SAFE_TRANSACTION.wrapped.address)
        given(gnosisSafeRepositoryMock.loadSafe(MockUtils.any())).willReturn(Single.just(safe))

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.loadSafe().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().loadSafe(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(safe)
    }

    @Test
    fun loadSafeError() {
        val testObserver = TestObserver<Safe>()
        val exception = Exception()
        given(gnosisSafeRepositoryMock.loadSafe(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.loadSafe().subscribe(testObserver)

        then(gnosisSafeRepositoryMock).should().loadSafe(SAFE_TRANSACTION.wrapped.address)
        then(gnosisSafeRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(Exception::class.java)
    }

    @Test
    fun submitTransaction() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))


        given(accountsRepositoryMock.recover(TX_HASH, SIGNATURE_1)).willReturn(Single.just("0x0a".asEthereumAddress()))
        given(accountsRepositoryMock.recover(TX_HASH, SIGNATURE_2)).willReturn(Single.just("0x14".asEthereumAddress()))

        given(
            transactionExecutionRepositoryMock.submit(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean()
            )
        ).willReturn(Single.just("RANDOM_TX_HASH"))

        given(pushServiceRepositoryMock.propagateSafeCreation(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(SAFE_TRANSACTION.wrapped.address, SAFE_TRANSACTION, TX_GAS, DATA_GAS, GAS_PRICE)
        then(accountsRepositoryMock).should().recover(TX_HASH, SIGNATURE_1)
        then(accountsRepositoryMock).should().recover(TX_HASH, SIGNATURE_2)
        then(transactionExecutionRepositoryMock).should().submit(
            SAFE_TRANSACTION.wrapped.address,
            SAFE_TRANSACTION,
            mapOf(Solidity.Address(10.toBigInteger()) to SIGNATURE_1, Solidity.Address(20.toBigInteger()) to SIGNATURE_2),
            false,
            TX_GAS,
            DATA_GAS,
            GAS_PRICE,
            true
        )
        then(pushServiceRepositoryMock).should().propagateSafeCreation(SAFE_TRANSACTION.wrapped.address, setOf(NEW_CHROME_EXTENSION_ADDRESS))
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(DataResult(Unit))
    }

    @Test
    fun submitTransactionErrorSendingPush() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))

        given(accountsRepositoryMock.recover(TX_HASH, SIGNATURE_1)).willReturn(Single.just("0x0a".asEthereumAddress()))
        given(accountsRepositoryMock.recover(TX_HASH, SIGNATURE_2)).willReturn(Single.just("0x14".asEthereumAddress()))

        given(
            transactionExecutionRepositoryMock.submit(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean()
            )
        ).willReturn(Single.just("RANDOM_TX_HASH"))

        given(pushServiceRepositoryMock.propagateSafeCreation(MockUtils.any(), MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(SAFE_TRANSACTION.wrapped.address, SAFE_TRANSACTION, TX_GAS, DATA_GAS, GAS_PRICE)
        then(accountsRepositoryMock).should().recover(TX_HASH, SIGNATURE_1)
        then(accountsRepositoryMock).should().recover(TX_HASH, SIGNATURE_2)
        then(transactionExecutionRepositoryMock).should().submit(
            SAFE_TRANSACTION.wrapped.address,
            SAFE_TRANSACTION,
            mapOf(Solidity.Address(10.toBigInteger()) to SIGNATURE_1, Solidity.Address(20.toBigInteger()) to SIGNATURE_2),
            false,
            TX_GAS,
            DATA_GAS,
            GAS_PRICE,
            true
        )
        then(pushServiceRepositoryMock).should().propagateSafeCreation(SAFE_TRANSACTION.wrapped.address, setOf(NEW_CHROME_EXTENSION_ADDRESS))
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(DataResult(Unit))
    }

    @Test
    fun submitTransactionErrorSubmitting() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))

        given(accountsRepositoryMock.recover(TX_HASH, SIGNATURE_1)).willReturn(Single.just("0x0a".asEthereumAddress()))
        given(accountsRepositoryMock.recover(TX_HASH, SIGNATURE_2)).willReturn(Single.just("0x14".asEthereumAddress()))

        given(
            transactionExecutionRepositoryMock.submit(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                anyBoolean()
            )
        ).willReturn(Single.error(exception))

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(SAFE_TRANSACTION.wrapped.address, SAFE_TRANSACTION, TX_GAS, DATA_GAS, GAS_PRICE)
        then(accountsRepositoryMock).should().recover(TX_HASH, SIGNATURE_1)
        then(accountsRepositoryMock).should().recover(TX_HASH, SIGNATURE_2)
        then(transactionExecutionRepositoryMock).should().submit(
            SAFE_TRANSACTION.wrapped.address,
            SAFE_TRANSACTION,
            mapOf(Solidity.Address(10.toBigInteger()) to SIGNATURE_1, Solidity.Address(20.toBigInteger()) to SIGNATURE_2),
            false,
            TX_GAS,
            DATA_GAS,
            GAS_PRICE,
            true
        )
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertValue { it is ErrorResult && it.error == exception }
    }

    @Test
    fun submitTransactionErrorRecovering() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(TX_HASH))
        
        given(accountsRepositoryMock.recover(TX_HASH, SIGNATURE_1)).willReturn(Single.just("0x0a".asEthereumAddress()))
        given(accountsRepositoryMock.recover(TX_HASH, SIGNATURE_2)).willReturn(Single.error(exception))

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(SAFE_TRANSACTION.wrapped.address, SAFE_TRANSACTION, TX_GAS, DATA_GAS, GAS_PRICE)
        then(accountsRepositoryMock).should().recover(TX_HASH, SIGNATURE_1)
        then(accountsRepositoryMock).should().recover(TX_HASH, SIGNATURE_2)
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertValue { it is ErrorResult && it.error == exception }
    }

    @Test
    fun submitTransactionWrongHash() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.just(byteArrayOf(0, 0, 0)))

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(SAFE_TRANSACTION.wrapped.address, SAFE_TRANSACTION, TX_GAS, DATA_GAS, GAS_PRICE)
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertValue { it is ErrorResult && it.error is IllegalStateException }
    }

    @Test
    fun submitTransactionErrorCalculatingHash() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        given(
            transactionExecutionRepositoryMock.calculateHash(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Single.error(exception))

        viewModel.setup(SAFE_TRANSACTION, SIGNATURE_1, SIGNATURE_2, TX_GAS, DATA_GAS, GAS_PRICE, NEW_CHROME_EXTENSION_ADDRESS, TX_HASH)
        viewModel.submitTransaction().subscribe(testObserver)

        then(transactionExecutionRepositoryMock).should()
            .calculateHash(SAFE_TRANSACTION.wrapped.address, SAFE_TRANSACTION, TX_GAS, DATA_GAS, GAS_PRICE)
        then(transactionExecutionRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver.assertValue { it is ErrorResult && it.error == exception }
    }

    companion object {
        private val SAFE_TRANSACTION = SafeTransaction(
            wrapped = Transaction(
                address = 42.toBigInteger().let { Solidity.Address(it) }
            ),
            operation = TransactionExecutionRepository.Operation.CALL
        )

        private val SIGNATURE_1 = Signature(r = 256.toBigInteger(), s = 257.toBigInteger(), v = 27)
        private val SIGNATURE_2 = Signature(r = 356.toBigInteger(), s = 357.toBigInteger(), v = 27)
        private val TX_GAS = 5000.toBigInteger()
        private val DATA_GAS = 6000.toBigInteger()
        private val GAS_PRICE = 1000.toBigInteger()
        private val NEW_CHROME_EXTENSION_ADDRESS = 42.toBigInteger().let { Solidity.Address(it) }
        private val TX_HASH = byteArrayOf(0, 1, 2, 3, 4, 5)
    }
}
