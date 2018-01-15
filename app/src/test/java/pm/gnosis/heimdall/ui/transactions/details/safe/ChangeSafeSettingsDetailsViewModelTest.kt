package pm.gnosis.heimdall.ui.transactions.details.safe

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
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
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.details.safe.ChangeSafeSettingsDetailsViewModel.Companion.EMPTY_FORM_DATA
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.tests.utils.mockGetStringWithArgs
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class ChangeSafeSettingsDetailsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var detailsRepoMock: TransactionDetailsRepository

    @Mock
    lateinit var safeRepoMock: GnosisSafeRepository

    private lateinit var viewModel: ChangeSafeSettingsDetailsViewModel

    @Before
    fun setup() {
        viewModel = ChangeSafeSettingsDetailsViewModel(contextMock, detailsRepoMock, safeRepoMock)
    }

    @Test
    fun loadFormData() {
        // We have no preset, so repo should not be called
        val noPresetObserver = TestObserver<Pair<String, Int>>()
        viewModel.loadFormData(null).subscribe(noPresetObserver)

        noPresetObserver.assertResult(EMPTY_FORM_DATA)
        then(detailsRepoMock).shouldHaveNoMoreInteractions()

        // We don't have an add transaction, this should return empty form data and should not cache
        val removeDetails = TransactionDetails(null, TransactionType.REMOVE_SAFE_OWNER, RemoveSafeOwnerData(BigInteger.ONE, 1), TEST_TX)
        given(detailsRepoMock.loadTransactionDetails(MockUtils.any())).willReturn(Single.just(removeDetails))

        val removeOwnerObserver = TestObserver<Pair<String, Int>>()
        viewModel.loadFormData(TEST_TX).subscribe(removeOwnerObserver)

        removeOwnerObserver.assertResult(EMPTY_FORM_DATA)
        then(detailsRepoMock).should().loadTransactionDetails(TEST_TX)

        // We have an add transaction, so we should get the owner and new threshold
        val addDetails = TransactionDetails(null, TransactionType.ADD_SAFE_OWNER, AddSafeOwnerData(BigInteger.ONE, 1), TEST_TX)
        given(detailsRepoMock.loadTransactionDetails(MockUtils.any())).willReturn(Single.just(addDetails))

        val testObserver = TestObserver<Pair<String, Int>>()
        viewModel.loadFormData(TEST_TX).subscribe(testObserver)

        testObserver.assertResult(BigInteger.ONE.asEthereumAddressString() to 1)
        then(detailsRepoMock).should(times(2)).loadTransactionDetails(TEST_TX)

        // If we had a result it should be cached
        val cachedObserver = TestObserver<Pair<String, Int>>()
        viewModel.loadFormData(TEST_TX).subscribe(cachedObserver)

        cachedObserver.assertResult(BigInteger.ONE.asEthereumAddressString() to 1)
        then(detailsRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataClearDefaults() {
        // Address 0x0 should be cleared (=> "")
        val addDetails = TransactionDetails(null, TransactionType.ADD_SAFE_OWNER, AddSafeOwnerData(BigInteger.ZERO, 1), TEST_TX)
        given(detailsRepoMock.loadTransactionDetails(MockUtils.any())).willReturn(Single.just(addDetails))

        val testObserver = TestObserver<Pair<String, Int>>()
        viewModel.loadFormData(TEST_TX).subscribe(testObserver)

        testObserver.assertResult("" to 1)
        then(detailsRepoMock).should().loadTransactionDetails(TEST_TX)
        then(detailsRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun inputTransformerWithSafeInfo() {
        contextMock.mockGetString()

        val info = SafeInfo(TEST_SAFE.asEthereumAddressString(), Wei.ZERO, 1, listOf(TEST_OWNER), false)
        given(safeRepoMock.loadInfo(MockUtils.any())).willReturn(Observable.just(info))
        val data = GnosisSafe.AddOwner.encode(Solidity.Address(BigInteger.valueOf(2)), Solidity.UInt8(BigInteger.valueOf(1)))
        testInputTransformer(TEST_SAFE,
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, false)),
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, true)),
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, true)),
                ErrorResult(TransactionInputException(R.string.error_owner_already_added.toString(), TransactionInputException.TARGET_FIELD, true)),
                DataResult(Transaction(TEST_SAFE, data = data))
        )

        // We have no preset, so repo should not be called
        val cachedObserver = TestObserver<Pair<String, Int>>()
        viewModel.loadFormData(null).subscribe(cachedObserver)
        cachedObserver.assertResult("0x2" to 1)

        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun inputTransformerNoSafe() {
        contextMock.mockGetString()
        contextMock.mockGetStringWithArgs()

        testInputTransformer(null,
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, false)),
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, true)),
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, true)),
                ErrorResult(SimpleLocalizedException(R.string.unknown_error.asString())),
                ErrorResult(SimpleLocalizedException(R.string.unknown_error.asString()))
        )

        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun inputTransformerLoadSafeError() {
        contextMock.mockGetString()
        contextMock.mockGetStringWithArgs()

        given(safeRepoMock.loadInfo(MockUtils.any())).willReturn(Observable.error(UnknownHostException()))
        testInputTransformer(TEST_SAFE,
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, false)),
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, true)),
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, true)),
                ErrorResult(SimpleLocalizedException(R.string.unknown_error.asString())),
                ErrorResult(SimpleLocalizedException(R.string.unknown_error.asString()))
        )

        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun inputTransformerLoadSafeErrorCachedInfo() {
        contextMock.mockGetString()
        contextMock.mockGetStringWithArgs()

        // We have an add transaction, so we should get the owner and new threshold
        val addDetails = TransactionDetails(null, TransactionType.ADD_SAFE_OWNER, AddSafeOwnerData(BigInteger.ZERO, 1), TEST_TX)
        given(detailsRepoMock.loadTransactionDetails(MockUtils.any())).willReturn(Single.just(addDetails))

        val testObserver = TestObserver<Pair<String, Int>>()
        viewModel.loadFormData(TEST_TX).subscribe(testObserver)

        testObserver.assertResult("" to 1)
        then(detailsRepoMock).should().loadTransactionDetails(TEST_TX)

        // Check that the cached data is used
        given(safeRepoMock.loadInfo(MockUtils.any())).willReturn(Observable.error(UnknownHostException()))
        val data1 = GnosisSafe.AddOwner.encode(Solidity.Address(TEST_OWNER), Solidity.UInt8(BigInteger.valueOf(1)))
        val data2 = GnosisSafe.AddOwner.encode(Solidity.Address(BigInteger.valueOf(2)), Solidity.UInt8(BigInteger.valueOf(1)))
        testInputTransformer(TEST_SAFE,
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, false)),
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, true)),
                ErrorResult(TransactionInputException(R.string.invalid_ethereum_address.toString(), TransactionInputException.TARGET_FIELD, true)),
                DataResult(Transaction(TEST_SAFE, data = data1)),
                DataResult(Transaction(TEST_SAFE, data = data2))
        )

        // We have no preset, so repo should not be called
        val cachedObserver = TestObserver<Pair<String, Int>>()
        viewModel.loadFormData(null).subscribe(cachedObserver)
        cachedObserver.assertResult("0x2" to 1)

        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(detailsRepoMock).shouldHaveNoMoreInteractions()
    }

    private fun testInputTransformer(safe: BigInteger?, vararg outputs: Result<Transaction>) {

        val inputs = PublishSubject.create<CharSequence>()
        val testObserver = TestObserver<Result<Transaction>>()

        inputs.compose(viewModel.inputTransformer(safe)).subscribe(testObserver)

        inputs.onNext("")
        testObserver.assertValuesOnly(outputs.first())

        inputs.onNext("BlaBlaInvalidAddress")
        testObserver.assertValuesOnly(
                outputs.first(), outputs.getOrNull(1))

        inputs.onNext("0x0")
        testObserver.assertValuesOnly(
                outputs.first(), outputs.getOrNull(1), outputs.getOrNull(2))

        inputs.onNext(TEST_OWNER.asEthereumAddressString())
        testObserver.assertValuesOnly(
                outputs.first(), outputs.getOrNull(1), outputs.getOrNull(2), outputs.getOrNull(3))

        inputs.onNext("0x2")
        testObserver.assertValuesOnly(
                outputs.first(), outputs.getOrNull(1), outputs.getOrNull(2), outputs.getOrNull(3), outputs.getOrNull(4))
    }

    @Test
    fun loadActionNoTx() {
        val testObserver = TestObserver<ChangeSafeSettingsDetailsContract.Action>()

        viewModel.loadAction(TEST_SAFE, null).subscribe(testObserver)

        testObserver.assertFailure(IllegalStateException::class.java)

        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(detailsRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadActionUnsupportedAction() {
        val testObserver = TestObserver<ChangeSafeSettingsDetailsContract.Action>()

        val details = Single.just(TransactionDetails(null, TransactionType.ETHER_TRANSFER, null, TEST_TX))
        given(detailsRepoMock.loadTransactionDetails(MockUtils.any())).willReturn(details)

        viewModel.loadAction(TEST_SAFE, TEST_TX).subscribe(testObserver)

        testObserver.assertFailure(IllegalStateException::class.java)

        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(detailsRepoMock).should().loadTransactionDetails(TEST_TX)
        then(detailsRepoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadAddOwnerAction() {
        val testObserver = TestObserver<ChangeSafeSettingsDetailsContract.Action>()
        val data = AddSafeOwnerData(TEST_OWNER, 1)
        val details = Single.just(TransactionDetails(null, TransactionType.ADD_SAFE_OWNER, data, TEST_TX))
        given(detailsRepoMock.loadTransactionDetails(MockUtils.any())).willReturn(details)

        viewModel.loadAction(TEST_SAFE, TEST_TX).subscribe(testObserver)

        testObserver.assertResult(ChangeSafeSettingsDetailsContract.Action.AddOwner(TEST_OWNER.asEthereumAddressString()))

        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(detailsRepoMock).should().loadTransactionDetails(TEST_TX)
        then(detailsRepoMock).shouldHaveNoMoreInteractions()
    }

    private fun testLoadAction(info: SafeInfo?, details: Single<TransactionDetails>,
                               result: ChangeSafeSettingsDetailsContract.Action) {
        val testObserver = TestObserver<ChangeSafeSettingsDetailsContract.Action>()
        given(detailsRepoMock.loadTransactionDetails(MockUtils.any())).willReturn(details)

        if (info != null) {
            given(safeRepoMock.loadInfo(MockUtils.any())).willReturn(Observable.just(info))
        }

        viewModel.loadAction(info?.let { TEST_SAFE }, TEST_TX).subscribe(testObserver)

        testObserver.assertResult(result)

        if (info != null) {
            then(safeRepoMock).should().loadInfo(TEST_SAFE)
        }
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(detailsRepoMock).should().loadTransactionDetails(TEST_TX)
        then(detailsRepoMock).shouldHaveNoMoreInteractions()
        Mockito.reset(safeRepoMock)
        Mockito.reset(detailsRepoMock)
    }

    @Test
    fun loadRemoveOwnerAction() {
        contextMock.mockGetStringWithArgs()
        val data = RemoveSafeOwnerData(BigInteger.ZERO, 1)
        val details = Single.just(TransactionDetails(null, TransactionType.REMOVE_SAFE_OWNER, data, TEST_TX))

        // Build fallback string with index if we don't have safe info
        testLoadAction(null, details, ChangeSafeSettingsDetailsContract.Action.RemoveOwner(contextMock.getString(R.string.owner_x, 1)))

        // Return owner address if we can retrieve safe info
        val info = SafeInfo(TEST_SAFE.asEthereumAddressString(), Wei.ZERO, 1, listOf(TEST_OWNER), true)
        testLoadAction(info, details, ChangeSafeSettingsDetailsContract.Action.RemoveOwner(TEST_OWNER.asEthereumAddressString()))
    }

    @Test
    fun loadReplaceOwnerAction() {
        contextMock.mockGetStringWithArgs()
        val data = ReplaceSafeOwnerData(BigInteger.ZERO, TEST_OWNER_2)
        val details = Single.just(TransactionDetails(null, TransactionType.REPLACE_SAFE_OWNER, data, TEST_TX))

        // Build fallback string with index if we don't have safe info
        testLoadAction(null, details, ChangeSafeSettingsDetailsContract.Action.ReplaceOwner(TEST_OWNER_2.asEthereumAddressString(), contextMock.getString(R.string.owner_x, 1)))

        // Return owner address if we can retrieve safe info
        val info = SafeInfo(TEST_SAFE.asEthereumAddressString(), Wei.ZERO, 1, listOf(TEST_OWNER), true)
        testLoadAction(info, details, ChangeSafeSettingsDetailsContract.Action.ReplaceOwner(TEST_OWNER_2.asEthereumAddressString(), TEST_OWNER.asEthereumAddressString()))
    }

    private fun Int.asString(vararg params: Any) =
            contextMock.getString(this, params)

    companion object {
        private val TEST_OWNER = BigInteger.valueOf(40320)
        private val TEST_OWNER_2 = BigInteger.valueOf(42424242)
        private val TEST_SAFE = BigInteger.valueOf(112358)
        private val TEST_TX = Transaction(BigInteger.valueOf(314159))
    }
}