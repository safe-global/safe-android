package pm.gnosis.heimdall.ui.safe.main

import android.app.Application
import android.content.Context
import androidx.room.EmptyResultSetException
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.preferences.PreferencesSafe
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.*
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeMainViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private val preferences = TestPreferences()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var bridgeRepository: BridgeRepository

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    private lateinit var safePreferences: PreferencesSafe

    private lateinit var viewModel: SafeMainViewModel

    @Before
    fun setUp() {
        given(application.getSharedPreferences(anyString(), anyInt())).willReturn(preferences)
        safePreferences = PreferencesSafe(application)
        viewModel = SafeMainViewModel(context, addressBookRepository, bridgeRepository, safePreferences, safeRepository)
    }

    @Test
    fun observeSafes() {
        val safeProcessor = BehaviorProcessor.create<List<AbstractSafe>>()
        given(safeRepository.observeAllSafes()).willReturn(safeProcessor)

        val safeSubscriber = TestSubscriber<Result<Adapter.Data<AbstractSafe>>>()
        viewModel.observeSafes().subscribe(safeSubscriber)

        then(safeRepository).should().observeAllSafes()
        then(safeRepository).shouldHaveNoMoreInteractions()


        safeSubscriber.assertValueCount(1).assertValueAt(0) {
            (it as DataResult).data.let {
                it.entries == emptyList<AbstractSafe>()
                        && it.diff == null
                        && it.parentId == null
            }
        }

        var parentId = (safeSubscriber.values().last() as DataResult).data.id

        safeProcessor.offer(
            listOf(
                Safe(TEST_SAFE)
            )
        )

        safeSubscriber.assertValueCount(2).assertValueAt(1) {
            (it as DataResult).data.let {
                it.entries == listOf(
                    Safe(TEST_SAFE)
                )
                        && it.diff != null
                        && it.parentId == parentId
            }
        }

        parentId = (safeSubscriber.values().last() as DataResult).data.id

        // When we load the selected safe the list should update
        viewModel.loadSelectedSafe().subscribe()
        then(safeRepository).should(times(2)).observeAllSafes()
        then(safeRepository).shouldHaveNoMoreInteractions()

        safeSubscriber.assertValueCount(3).assertValueAt(2) {
            (it as DataResult).data.let {
                it.entries == emptyList<AbstractSafe>()
                        && it.diff != null
                        && it.parentId == parentId
            }
        }

        parentId = (safeSubscriber.values().last() as DataResult).data.id
        safeProcessor.offer(
            listOf(
                Safe(TEST_SAFE),
                PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
            )
        )

        safeSubscriber.assertValueCount(4).assertValueAt(3) {
            (it as DataResult).data.let {
                it.entries == listOf(
                    PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
                )
                        && it.diff != null
                        && it.parentId == parentId
            }
        }

        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun loadSelectedSafeNoSafes() {
        safePreferences.selectedSafe = null
        given(safeRepository.observeAllSafes()).willReturn(Flowable.just(emptyList()))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.loadSelectedSafe().subscribe(safeObserver)

        safeObserver.assertFailure(NoSuchElementException::class.java)

        then(safeRepository).should().observeAllSafes()
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun loadSelectedSafeNoSelectedSafe() {
        safePreferences.selectedSafe = null
        given(safeRepository.observeAllSafes()).willReturn(
            Flowable.just(
                listOf(
                    Safe(TEST_SAFE),
                    PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
                )
            )
        )

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.loadSelectedSafe().subscribe(safeObserver)

        safeObserver.assertResult(Safe(TEST_SAFE))

        then(safeRepository).should().observeAllSafes()
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun loadSelectedSafe() {
        safePreferences.selectedSafe = TEST_SAFE
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.just(Safe(TEST_SAFE)))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.loadSelectedSafe().subscribe(safeObserver)

        safeObserver.assertResult(Safe(TEST_SAFE))

        then(safeRepository).should().loadSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun loadSelectedSafePending() {
        val pendingSafe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
        safePreferences.selectedSafe = TEST_PENDING_SAFE
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepository.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.loadSelectedSafe().subscribe(safeObserver)

        safeObserver.assertResult(pendingSafe)

        then(safeRepository).should().loadSafe(TEST_PENDING_SAFE)
        then(safeRepository).should().loadPendingSafe(TEST_PENDING_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun selectSafe() {
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.just(Safe(TEST_SAFE)))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.selectSafe(TEST_SAFE).subscribe(safeObserver)

        assertEquals(safePreferences.selectedSafe, TEST_SAFE)
        safeObserver.assertResult(Safe(TEST_SAFE))

        then(safeRepository).should().loadSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun selectSafePending() {
        val pendingSafe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepository.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.selectSafe(TEST_PENDING_SAFE).subscribe(safeObserver)

        assertEquals(safePreferences.selectedSafe, TEST_PENDING_SAFE)
        safeObserver.assertResult(pendingSafe)

        then(safeRepository).should().loadSafe(TEST_PENDING_SAFE)
        then(safeRepository).should().loadPendingSafe(TEST_PENDING_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun selectSafeRecovering() {
        val recoveringSafe = testRecoveringSafe(
            TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, gasToken = TEST_PAYMENT_TOKEN
        )
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepository.loadPendingSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(safeRepository.loadRecoveringSafe(MockUtils.any())).willReturn(Single.just(recoveringSafe))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.selectSafe(TEST_RECOVERING_SAFE).subscribe(safeObserver)

        assertEquals(safePreferences.selectedSafe, TEST_RECOVERING_SAFE)
        safeObserver.assertResult(recoveringSafe)

        then(safeRepository).should().loadSafe(TEST_RECOVERING_SAFE)
        then(safeRepository).should().loadPendingSafe(TEST_RECOVERING_SAFE)
        then(safeRepository).should().loadRecoveringSafe(TEST_RECOVERING_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun syncWithChromeExtension() {
        val sendCompletable = Completable.complete()
        given(safeRepository.sendSafeCreationPush(MockUtils.any())).willReturn(sendCompletable)

        assertEquals(viewModel.syncWithChromeExtension(TEST_SAFE), sendCompletable)

        then(safeRepository).should().sendSafeCreationPush(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun updateSafeName() {
        context.mockGetString()
        val safe = Safe(TEST_SAFE)
        given(addressBookRepository.updateAddressBookEntry(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertResult()

        then(addressBookRepository).should().updateAddressBookEntry(TEST_SAFE, R.string.default_safe_name.toString())
        then(addressBookRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertResult()

        then(addressBookRepository).should().updateAddressBookEntry(TEST_SAFE, "New Name")
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun updateSafeNameError() {
        context.mockGetString()
        val safe = Safe(TEST_SAFE)
        val error = NoSuchElementException()
        given(addressBookRepository.updateAddressBookEntry(MockUtils.any(), MockUtils.any())).willReturn(Completable.error(error))

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertError(error)

        then(addressBookRepository).should().updateAddressBookEntry(TEST_SAFE, R.string.default_safe_name.toString())
        then(addressBookRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertError(error)

        then(addressBookRepository).should().updateAddressBookEntry(TEST_SAFE, "New Name")
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun updatePendingSafeName() {
        context.mockGetString()
        val safe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        given(addressBookRepository.updateAddressBookEntry(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertResult()

        then(addressBookRepository).should().updateAddressBookEntry(TEST_PENDING_SAFE, R.string.default_safe_name.toString())
        then(addressBookRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertResult()

        then(addressBookRepository).should().updateAddressBookEntry(TEST_PENDING_SAFE, "New Name")
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun updatePendingSafeNameError() {
        context.mockGetString()
        val safe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        val error = NoSuchElementException()
        given(addressBookRepository.updateAddressBookEntry(MockUtils.any(), MockUtils.any())).willReturn(Completable.error(error))

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertError(error)

        then(addressBookRepository).should().updateAddressBookEntry(TEST_PENDING_SAFE, R.string.default_safe_name.toString())
        then(addressBookRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertError(error)

        then(addressBookRepository).should().updateAddressBookEntry(TEST_PENDING_SAFE, "New Name")
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun updateRecoveringSafeName() {
        context.mockGetString()
        val safe = testRecoveringSafe(
            TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, gasToken = TEST_PAYMENT_TOKEN
        )
        given(addressBookRepository.updateAddressBookEntry(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertResult()

        then(addressBookRepository).should().updateAddressBookEntry(TEST_RECOVERING_SAFE, R.string.default_safe_name.toString())
        then(addressBookRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertResult()

        then(addressBookRepository).should().updateAddressBookEntry(TEST_RECOVERING_SAFE, "New Name")
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun updateRecoveringSafeNameError() {
        context.mockGetString()
        val safe = testRecoveringSafe(
            TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, gasToken = TEST_PAYMENT_TOKEN
        )
        val error = NoSuchElementException()
        given(addressBookRepository.updateAddressBookEntry(MockUtils.any(), MockUtils.any())).willReturn(Completable.error(error))

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertError(error)

        then(addressBookRepository).should().updateAddressBookEntry(TEST_RECOVERING_SAFE, R.string.default_safe_name.toString())
        then(addressBookRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertError(error)

        then(addressBookRepository).should().updateAddressBookEntry(TEST_RECOVERING_SAFE, "New Name")
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun observeSafe() {
        context.mockGetString()
        val safe = Safe(TEST_SAFE)
        val infoProcessor = PublishProcessor.create<AddressBookEntry>()
        given(addressBookRepository.observeAddressBookEntry(MockUtils.any())).willReturn(infoProcessor)

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(safe).subscribe(safeSubscriber)

        infoProcessor.offer(AddressBookEntry(TEST_SAFE, "Old Name", ""))
        safeSubscriber
            .assertValueCount(1)
            .assertValueAt(0, "Old Name" to "0x1f...5C7E")

        infoProcessor.offer(AddressBookEntry(TEST_SAFE, "", ""))
        safeSubscriber
            .assertValueCount(2)
            .assertValueAt(1, "" to "0x1f...5C7E")

        then(addressBookRepository).should().observeAddressBookEntry(TEST_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun observeSafeError() {
        val error = NoSuchElementException()
        given(addressBookRepository.observeAddressBookEntry(MockUtils.any())).willReturn(Flowable.error(error))

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(Safe(TEST_SAFE)).subscribe(safeSubscriber)

        safeSubscriber.assertFailure(Predicate { it == error })

        then(addressBookRepository).should().observeAddressBookEntry(TEST_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun observePendingSafe() {
        context.mockGetString()
        val safe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        val infoProcessor = PublishProcessor.create<AddressBookEntry>()
        given(addressBookRepository.observeAddressBookEntry(MockUtils.any())).willReturn(infoProcessor)

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(safe).subscribe(safeSubscriber)

        infoProcessor.offer(AddressBookEntry(TEST_PENDING_SAFE, "Old Name", ""))
        safeSubscriber
            .assertValueCount(1)
            .assertValueAt(0, "Old Name" to "0xC2...8132")

        infoProcessor.offer(AddressBookEntry(TEST_PENDING_SAFE, "", ""))
        safeSubscriber
            .assertValueCount(2)
            .assertValueAt(1, "" to "0xC2...8132")

        then(addressBookRepository).should().observeAddressBookEntry(TEST_PENDING_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun observePendingSafeError() {
        val error = NoSuchElementException()
        given(addressBookRepository.observeAddressBookEntry(MockUtils.any())).willReturn(Flowable.error(error))

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)).subscribe(safeSubscriber)

        safeSubscriber.assertFailure(Predicate { it == error })

        then(addressBookRepository).should().observeAddressBookEntry(TEST_PENDING_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun observeRecoveringSafe() {
        context.mockGetString()
        val safe = testRecoveringSafe(
            TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, gasToken = TEST_PAYMENT_TOKEN
        )
        val infoProcessor = PublishProcessor.create<AddressBookEntry>()
        given(addressBookRepository.observeAddressBookEntry(MockUtils.any())).willReturn(infoProcessor)

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(safe).subscribe(safeSubscriber)

        infoProcessor.offer(AddressBookEntry(TEST_RECOVERING_SAFE, "Old Name", ""))
        safeSubscriber
            .assertValueCount(1)
            .assertValueAt(0, "Old Name" to "0xb3...244A")

        infoProcessor.offer(AddressBookEntry(TEST_RECOVERING_SAFE, "", ""))
        safeSubscriber
            .assertValueCount(2)
            .assertValueAt(1, "" to "0xb3...244A")

        then(addressBookRepository).should().observeAddressBookEntry(TEST_RECOVERING_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun observeRecoveringSafeError() {
        val safe = testRecoveringSafe(
            TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, gasToken = TEST_PAYMENT_TOKEN
        )
        val error = NoSuchElementException()
        given(addressBookRepository.observeAddressBookEntry(MockUtils.any())).willReturn(Flowable.error(error))

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(safe).subscribe(safeSubscriber)

        safeSubscriber.assertFailure(Predicate { it == error })

        then(addressBookRepository).should().observeAddressBookEntry(TEST_RECOVERING_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun removeSafe() {
        val safe = Safe(TEST_SAFE)
        given(safeRepository.removeSafe(MockUtils.any())).willReturn(Completable.complete())

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertResult()

        then(safeRepository).should().removeSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removeSafeError() {
        val safe = Safe(TEST_SAFE)
        val error = NoSuchElementException()
        given(safeRepository.removeSafe(MockUtils.any())).willReturn(Completable.error(error))

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == error })

        then(safeRepository).should().removeSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removePendingSafe() {
        val safe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        given(safeRepository.removePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertResult()

        then(safeRepository).should().removePendingSafe(TEST_PENDING_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removePendingSafeError() {
        val safe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        val error = NoSuchElementException()
        given(safeRepository.removePendingSafe(MockUtils.any())).willReturn(Completable.error(error))

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == error })

        then(safeRepository).should().removePendingSafe(TEST_PENDING_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removeRecoveringSafe() {
        val safe = testRecoveringSafe(
            TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, gasToken = TEST_PAYMENT_TOKEN
        )
        given(safeRepository.removeRecoveringSafe(MockUtils.any())).willReturn(Completable.complete())

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertResult()

        then(safeRepository).should().removeRecoveringSafe(TEST_RECOVERING_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removeRecoveringSafeError() {
        val safe = testRecoveringSafe(
            TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, gasToken = TEST_PAYMENT_TOKEN
        )
        val error = NoSuchElementException()
        given(safeRepository.removeRecoveringSafe(MockUtils.any())).willReturn(Completable.error(error))

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == error })

        then(safeRepository).should().removeRecoveringSafe(TEST_RECOVERING_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun isConnectedToBrowserExtension() {
        val testObserver = TestObserver.create<Result<Pair<Solidity.Address?, Boolean>>>()
        given(safeRepository.observePendingTransactions(MockUtils.any())).willReturn(Flowable.just(emptyList()))
        given(safeRepository.checkSafe(MockUtils.any())).willReturn(Observable.just(SafeContractUtils.currentMasterCopy() to true))

        viewModel.loadSafeConfig(Safe(TEST_SAFE)).subscribe(testObserver)

        then(safeRepository).should().observePendingTransactions(TEST_SAFE)
        then(safeRepository).should().checkSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        testObserver.assertResult(DataResult(null to true))
    }

    @Test
    fun isConnectedToBrowserExtensionPendingTx() {
        val testObserver = TestObserver.create<Result<Pair<Solidity.Address?, Boolean>>>()
        val testFlowable = TestFlowableFactory<List<TransactionStatus>>()
        given(safeRepository.observePendingTransactions(MockUtils.any())).willReturn(testFlowable.get())

        viewModel.loadSafeConfig(Safe(TEST_SAFE)).subscribe(testObserver)

        testFlowable.offer(listOf(TransactionStatus("id", 9, true)))
        then(safeRepository).should().observePendingTransactions(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        testObserver.assertEmpty()
    }

    @Test
    fun isNotConnectedToBrowserExtension() {
        val testObserver = TestObserver.create<Result<Pair<Solidity.Address?, Boolean>>>()
        given(safeRepository.observePendingTransactions(MockUtils.any())).willReturn(Flowable.just(emptyList()))
        given(safeRepository.checkSafe(MockUtils.any()))
            .willReturn(Observable.just(TEST_MASTER_COPY to false))

        viewModel.loadSafeConfig(Safe(TEST_SAFE)).subscribe(testObserver)

        then(safeRepository).should().observePendingTransactions(TEST_SAFE)
        then(safeRepository).should().checkSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        testObserver.assertResult(DataResult(SafeContractUtils.currentMasterCopy() to false))
    }

    @Test
    fun isConnectedToBrowserExtensionIsNotSafe() {
        val testObserver = TestObserver.create<Result<Pair<Solidity.Address?, Boolean>>>()
        given(safeRepository.observePendingTransactions(MockUtils.any())).willReturn(Flowable.just(emptyList()))
        given(safeRepository.checkSafe(MockUtils.any())).willReturn(Observable.just(null to false))

        viewModel.loadSafeConfig(Safe(TEST_SAFE)).subscribe(testObserver)

        then(safeRepository).should().observePendingTransactions(TEST_SAFE)
        then(safeRepository).should().checkSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        testObserver.assertResult(DataResult(null to false))
    }

    @Test
    fun isConnectedToBrowserExtensionIsPendingSafe() {
        val testObserver = TestObserver.create<Result<Pair<Solidity.Address?, Boolean>>>()
        val pendingSafe = PendingSafe(TEST_PENDING_SAFE, TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)

        viewModel.loadSafeConfig(pendingSafe).subscribe(testObserver)

        then(safeRepository).shouldHaveZeroInteractions()
        testObserver.assertResult(DataResult(null to false))
    }

    @Test
    fun isConnectedToBrowserExtensionIsRecoveringSafe() {
        val testObserver = TestObserver.create<Result<Pair<Solidity.Address?, Boolean>>>()
        val recoveringSafe = testRecoveringSafe(
            TEST_RECOVERING_SAFE, TEST_TX_HASH, TEST_SAFE, gasToken = TEST_PAYMENT_TOKEN
        )

        viewModel.loadSafeConfig(recoveringSafe).subscribe(testObserver)

        then(safeRepository).shouldHaveZeroInteractions()
        testObserver.assertResult(DataResult(null to false))
    }

    @Test
    fun isConnectedToBrowserExtensionError() {
        val testObserver = TestObserver.create<Result<Pair<Solidity.Address?, Boolean>>>()
        val exception = IllegalStateException()
        given(safeRepository.observePendingTransactions(MockUtils.any())).willReturn(Flowable.just(emptyList()))
        given(safeRepository.checkSafe(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadSafeConfig(Safe(TEST_SAFE)).subscribe(testObserver)

        then(safeRepository).should().observePendingTransactions(TEST_SAFE)
        then(safeRepository).should().checkSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun shouldShowWalletConnectIntro() {
        val exception = IllegalStateException()
        given(bridgeRepository.shouldShowIntro()).willReturn(Single.error(exception))

        val testObserver = TestObserver.create<Boolean>()
        viewModel.shouldShowWalletConnectIntro().subscribe(testObserver)

        then(bridgeRepository).should().shouldShowIntro()
        then(bridgeRepository).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(Predicate { it == exception })
    }

    @Test
    fun shouldShowWalletConnectIntroError() {
        given(bridgeRepository.shouldShowIntro()).willReturn(Single.just(true))

        val testObserver = TestObserver.create<Boolean>()
        viewModel.shouldShowWalletConnectIntro().subscribe(testObserver)

        then(bridgeRepository).should().shouldShowIntro()
        then(bridgeRepository).shouldHaveNoMoreInteractions()
        testObserver.assertResult(true)
    }

    companion object {
        private val TEST_TX_HASH = "0xdae721569a948b87c269ebacaa5a4a67728095e32f9e7e4626f109f27a73b40f".hexAsBigInteger()
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_PENDING_SAFE = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_RECOVERING_SAFE = "0xb36574155395D41b92664e7A215103262a14244A".asEthereumAddress()!!
        private val TEST_PAYMENT_TOKEN = ERC20Token.ETHER_TOKEN.address
        private val TEST_PAYMENT_AMOUNT = Wei.ether("0.1").value
        private val TEST_MASTER_COPY = BuildConfig.SAFE_MASTER_COPY_0_0_2.asEthereumAddress()!!
    }
}
