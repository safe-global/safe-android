package pm.gnosis.heimdall.ui.safe.add

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TxExecutorRepository
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.helpers.AddressStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.*
import pm.gnosis.tests.utils.MockUtils.any
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.models.Currency
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsEthereumAddress
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import java.math.BigDecimal
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class AddSafeViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var accountsRepository: AccountsRepository

    @Mock
    private lateinit var addressStore: AddressStore

    @Mock
    private lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var tickerRepositoryMock: TickerRepository

    @Mock
    private lateinit var txExecutorRepositoryMock: TxExecutorRepository

    private lateinit var viewModel: AddSafeViewModel

    @Before
    fun setUp() {
        context.mockGetString()
        viewModel = AddSafeViewModel(
            context, accountsRepository, addressStore,
            safeRepositoryMock, tickerRepositoryMock,
            txExecutorRepositoryMock
        )
    }

    @Test
    fun addExistingSafe() {
        val testObserver = TestObserver.create<Result<Unit>>()
        given(safeRepositoryMock.add(any(), anyString())).willReturn(Completable.complete())

        viewModel.addExistingSafe("test", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(DataResult(Unit))
        then(safeRepositoryMock).should().add(BigInteger.ZERO, "test")
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingSafeEmptyName() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.addExistingSafe("", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.message == R.string.error_blank_name.toString()
        }
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingInvalidAddress() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.addExistingSafe("test", "test").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.message == R.string.invalid_ethereum_address.toString()
        }
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun addExistingRepoError() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val error = IllegalStateException()
        given(safeRepositoryMock.add(any(), anyString())).willReturn(Completable.error(error))

        viewModel.addExistingSafe("test", "0x0").subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error == error
        }
        then(safeRepositoryMock).should().add(BigInteger.ZERO, "test")
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    private fun subscribeDeploySafe(name: String, price: Wei?) =
        TestObserver.create<Result<Unit>>().apply {
            viewModel.deployNewSafe(name, price).subscribe(this)
        }

    private fun testDeploySafe(
        name: String, additionalOwners: Set<BigInteger>, expectedError: Exception? = null,
        storeException: Exception? = null, repoException: Exception? = null
    ) {
        val hasStoreInteractions = expectedError == null || repoException != null || storeException != null
        val hasRepoInteractions = expectedError == null || (storeException == null && repoException != null)
        if (hasStoreInteractions) {
            val storeResult = storeException?.let { Single.error<Set<BigInteger>>(it) } ?: Single.just(additionalOwners)
            given(addressStore.load()).willReturn(storeResult)
        }
        if (hasRepoInteractions) {
            // Avoid unnecessary mocks
            val repoResult = repoException?.let { Completable.error(it) } ?: Completable.complete()
            given(safeRepositoryMock.deploy(any(), any(), anyInt(), any())).willReturn(repoResult)
        }

        val result = expectedError?.let { ErrorResult<Unit>(it) } ?: DataResult(Unit)
        subscribeDeploySafe(name, null).assertNoErrors().assertValue(result)
        if (hasStoreInteractions) {
            then(addressStore).should().load()
        }
        if (hasRepoInteractions) {
            then(safeRepositoryMock).should().deploy(name, additionalOwners, Math.max(1, additionalOwners.size), null)
        }
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()

        subscribeDeploySafe(name, Wei(BigInteger.TEN)).assertNoErrors().assertValue(result)
        if (hasStoreInteractions) {
            then(addressStore).should(times(2)).load()
        }
        if (hasRepoInteractions) {
            then(safeRepositoryMock).should().deploy(name, additionalOwners, Math.max(1, additionalOwners.size), Wei(BigInteger.TEN))
        }
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(addressStore).shouldHaveNoMoreInteractions()

        Mockito.reset(safeRepositoryMock)
        Mockito.reset(addressStore)
    }

    @Test
    fun deployNewSafe() {
        testDeploySafe("Test", emptySet())
        testDeploySafe("Test", setOf(BigInteger.ONE))
        testDeploySafe("Test", setOf(BigInteger.ONE, BigInteger.TEN))

        // Invalid input
        testDeploySafe("", emptySet(), SimpleLocalizedException(R.string.error_blank_name.toString()))
        testDeploySafe("     ", emptySet(), SimpleLocalizedException(R.string.error_blank_name.toString()))

        // Errors
        val error = IllegalStateException()
        testDeploySafe("test", emptySet(), error, repoException = error)
        testDeploySafe("test", emptySet(), error, storeException = error)
    }


    @Test
    fun observeEstimate() {
        val addressPublisher = PublishSubject.create<Set<BigInteger>>()

        then(safeRepositoryMock).shouldHaveNoMoreInteractions()

        addressPublisher.onNext(emptySet())

        then(safeRepositoryMock).shouldHaveNoMoreInteractions()

        addressPublisher.onNext(setOf(BigInteger.ONE, BigInteger.TEN))

        then(safeRepositoryMock).shouldHaveNoMoreInteractions()

        val error = IllegalStateException()
        addressPublisher.onError(error)

        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeAdditionalOwners() {
        val addressPublisher = PublishSubject.create<Set<BigInteger>>()
        given(addressStore.observe()).willReturn(addressPublisher)

        val testObserver = TestObserver<List<BigInteger>>()
        viewModel.observeAdditionalOwners().subscribe(testObserver)
        testObserver.assertEmpty()

        addressPublisher.onNext(setOf(BigInteger.ONE))
        testObserver.assertValuesOnly(listOf(BigInteger.ONE))

        addressPublisher.onNext(setOf(BigInteger.ONE, BigInteger.TEN))
        testObserver.assertValuesOnly(
            listOf(BigInteger.ONE),
            listOf(BigInteger.ONE, BigInteger.TEN) // New Value
        )

        addressPublisher.onNext(setOf(BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO))
        testObserver.assertValuesOnly(
            listOf(BigInteger.ONE),
            listOf(BigInteger.ONE, BigInteger.TEN),
            listOf(BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN) // New Value
        )

        val error = IllegalStateException()
        addressPublisher.onError(error)
        testObserver.assertFailure(
            Predicate { it == error }, // New Error
            listOf(BigInteger.ONE),
            listOf(BigInteger.ONE, BigInteger.TEN),
            listOf(BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN)
        )

        then(addressStore).should().observe()
        then(addressStore).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFiatConversion() {
        val wei = Wei((BigDecimal.valueOf(1.5) * BigDecimal.TEN.pow(18)).toBigInteger())
        val currency = Currency("testId", "testName", "testSymbol", 0, 0, BigDecimal.TEN, Currency.FiatSymbol.USD)
        given(tickerRepositoryMock.loadCurrency(MockUtils.any())).willReturn(Single.just(currency))

        val testObserver = TestObserver.create<Result<Pair<BigDecimal, Currency>>>()
        viewModel.loadFiatConversion(wei).subscribe(testObserver)
        testObserver.assertResult(DataResult(currency.convert(wei) to currency))

        then(tickerRepositoryMock).should().loadCurrency()
        then(tickerRepositoryMock).shouldHaveNoMoreInteractions()

        val cachedTestObserver = TestObserver.create<Result<Pair<BigDecimal, Currency>>>()
        viewModel.loadFiatConversion(wei).subscribe(cachedTestObserver)
        cachedTestObserver.assertResult(DataResult(currency.convert(wei) to currency))

        then(tickerRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFiatConversionError() {
        val wei = Wei(BigInteger.ONE)
        val exception = Exception()
        val testObserver = TestObserver.create<Result<Pair<BigDecimal, Currency>>>()
        given(tickerRepositoryMock.loadCurrency(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.loadFiatConversion(wei).subscribe(testObserver)

        then(tickerRepositoryMock).should().loadCurrency()
        then(tickerRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun removeAdditionalOwner() {
        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.removeAdditionalOwner(BigInteger.TEN).subscribe(testObserver)
        testObserver.assertResult(DataResult(Unit))

        then(addressStore).should().remove(BigInteger.TEN)
        then(addressStore).shouldHaveNoMoreInteractions()
    }

    private fun testAddAdditionalOwner(input: String, output: Result<Unit>, valueExists: Boolean? = null) {
        valueExists?.let {
            given(addressStore.contains(any())).willReturn(it)
        }

        val testObserver = TestObserver.create<Result<Unit>>()
        viewModel.addAdditionalOwner(input).subscribe(testObserver)
        testObserver.assertResult(output)

        input.hexAsEthereumAddressOrNull()?.let { address ->
            if (output is DataResult) {
                then(addressStore).should().add(address)
            }
            valueExists?.let {
                then(addressStore).should().contains(address)
            }
        }
        then(addressStore).shouldHaveNoMoreInteractions()
        Mockito.reset(addressStore)
    }

    @Test
    fun addAdditionalOwner() {
        context.mockGetStringWithArgs()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(Account(BigInteger.ONE)))
        viewModel.setupDeploy().subscribe(TestObserver<BigInteger>())
        then(addressStore).should().clear()

        testAddAdditionalOwner("0x10", DataResult(Unit), false)
        testAddAdditionalOwner("qwzrer0", localizedExceptionResult(R.string.invalid_ethereum_address))
        testAddAdditionalOwner("1", localizedExceptionResult(R.string.error_owner_already_added))
        testAddAdditionalOwner("7353", localizedExceptionResult(R.string.error_owner_already_added), true)
    }

    @Test
    fun setupDeploy() {
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(Account(BigInteger.ONE)))

        val testObserver = TestObserver<BigInteger>()
        viewModel.setupDeploy().subscribe(testObserver)
        testObserver.assertResult(BigInteger.ONE)

        then(addressStore).should().clear()
        then(addressStore).shouldHaveNoMoreInteractions()

        // Setting up with the same account should not erase the store
        val keepObserver = TestObserver<BigInteger>()
        viewModel.setupDeploy().subscribe(keepObserver)
        keepObserver.assertResult(BigInteger.ONE)
        then(addressStore).shouldHaveNoMoreInteractions()

        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(Account(BigInteger.TEN)))

        // Setting up with a different account should erase the store
        val clearObserver = TestObserver<BigInteger>()
        viewModel.setupDeploy().subscribe(clearObserver)
        clearObserver.assertResult(BigInteger.TEN)
        then(addressStore).should(times(2)).clear()
        then(addressStore).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSafeInfo() {
        val testObserver = TestObserver<Result<SafeInfo>>()
        val safeInfo = SafeInfo("0x0", Wei(BigInteger.ONE), 1, emptyList(), false)
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(safeInfo))

        viewModel.loadSafeInfo("0x0").subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo("0x0".hexAsEthereumAddress())
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(DataResult(safeInfo))
    }

    @Test
    fun loadSafeInfoWithInvalidAddress() {
        val testObserver = TestObserver<Result<SafeInfo>>()

        viewModel.loadSafeInfo("0xz").subscribe(testObserver)

        then(safeRepositoryMock).shouldHaveZeroInteractions()
        testObserver.assertValue { it is ErrorResult && it.error is InvalidAddressException }
            .assertNoErrors()
    }

    @Test
    fun loadSafeInfoError() {
        val testObserver = TestObserver<Result<SafeInfo>>()
        val exception = Exception()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadSafeInfo("0x0").subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo("0x0".hexAsEthereumAddress())
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun loadActiveAccount() {
        val testObserver = TestObserver<Account>()
        val account = Account(BigInteger.TEN)
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(account))

        viewModel.loadActiveAccount().subscribe(testObserver)

        then(accountsRepository).should().loadActiveAccount()
        then(accountsRepository).shouldHaveNoMoreInteractions()
        testObserver.assertResult(account)
    }

    @Test
    fun loadActiveAccountError() {
        val testObserver = TestObserver<Account>()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error(Exception()))

        viewModel.loadActiveAccount().subscribe(testObserver)

        then(accountsRepository).should().loadActiveAccount()
        then(accountsRepository).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertNoValues().assertComplete()
    }

    private fun localizedExceptionResult(res: Int) =
        ErrorResult<Unit>(SimpleLocalizedException(context.getTestString(res)))
}
