package pm.gnosis.heimdall.ui.safe.main

import android.app.Application
import android.content.Context
import io.reactivex.Completable
import io.reactivex.Flowable
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
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.*
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
    private lateinit var safeRepository: GnosisSafeRepository

    private lateinit var preferencesManager: PreferencesManager

    private lateinit var viewModel: SafeMainViewModel

    @Before
    fun setUp() {
        BDDMockito.given(application.getSharedPreferences(anyString(), anyInt())).willReturn(preferences)
        preferencesManager = PreferencesManager(application)
        viewModel = SafeMainViewModel(context, preferencesManager, safeRepository)
    }

    @Test
    fun observeSafes() {
        val safeProcessor = BehaviorProcessor.create<List<AbstractSafe>>()
        given(safeRepository.observeSafes()).willReturn(safeProcessor)

        val safeSubscriber = TestSubscriber<Result<Adapter.Data<AbstractSafe>>>()
        viewModel.observeSafes().subscribe(safeSubscriber)

        then(safeRepository).should().observeSafes()
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
        then(safeRepository).should(times(2)).observeSafes()
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
                PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE,  TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
            )
        )

        safeSubscriber.assertValueCount(4).assertValueAt(3) {
            (it as DataResult).data.let {
                it.entries == listOf(
                    PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE,  TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
                )
                        && it.diff != null
                        && it.parentId == parentId
            }
        }

        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSelectedSafeNoSafes() {
        preferences.remove(KEY_SELECTED_SAFE)
        given(safeRepository.observeSafes()).willReturn(Flowable.just(emptyList()))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.loadSelectedSafe().subscribe(safeObserver)

        safeObserver.assertFailure(NoSuchElementException::class.java)

        then(safeRepository).should().observeSafes()
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSelectedSafeNoSelectedSafe() {
        preferences.remove(KEY_SELECTED_SAFE)
        given(safeRepository.observeSafes()).willReturn(
            Flowable.just(
                listOf(
                    Safe(TEST_SAFE),
                    PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE,  TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
                )
            )
        )

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.loadSelectedSafe().subscribe(safeObserver)

        safeObserver.assertResult(Safe(TEST_SAFE))

        then(safeRepository).should().observeSafes()
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSelectedSafe() {
        preferences.putString(KEY_SELECTED_SAFE, TEST_SAFE.asEthereumAddressString())
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.just(Safe(TEST_SAFE)))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.loadSelectedSafe().subscribe(safeObserver)

        safeObserver.assertResult(Safe(TEST_SAFE))

        then(safeRepository).should().loadSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSelectedSafePending() {
        val pendingSafe = PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE,  TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
        preferences.putString(KEY_SELECTED_SAFE, TEST_TX_HASH.asTransactionHash())
        given(safeRepository.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.loadSelectedSafe().subscribe(safeObserver)

        safeObserver.assertResult(pendingSafe)

        then(safeRepository).should().loadPendingSafe(TEST_TX_HASH)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun selectSafe() {
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.just(Safe(TEST_SAFE)))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.selectSafe(TEST_SAFE.value).subscribe(safeObserver)

        assertEquals(preferences.getString(KEY_SELECTED_SAFE, null), TEST_SAFE.value.toHexString())
        safeObserver.assertResult(Safe(TEST_SAFE))

        then(safeRepository).should().loadSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun selectSafePending() {
        val pendingSafe = PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE,  TEST_PAYMENT_TOKEN, TEST_PAYMENT_AMOUNT)
        given(safeRepository.loadPendingSafe(MockUtils.any())).willReturn(Single.just(pendingSafe))

        val safeObserver = TestObserver<AbstractSafe>()
        viewModel.selectSafe(TEST_TX_HASH).subscribe(safeObserver)

        assertEquals(preferences.getString(KEY_SELECTED_SAFE, null), TEST_TX_HASH.toHexString())
        safeObserver.assertResult(pendingSafe)

        then(safeRepository).should().loadPendingSafe(TEST_TX_HASH)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun syncWithChromeExtension() {
        val sendCompletable = Completable.complete()
        given(safeRepository.sendSafeCreationPush(MockUtils.any())).willReturn(sendCompletable)

        assertEquals(viewModel.syncWithChromeExtension(TEST_SAFE), sendCompletable)

        then(safeRepository).should().sendSafeCreationPush(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateSafeName() {
        val safe = Safe(TEST_SAFE, "Old Name")
        given(safeRepository.updateSafe(MockUtils.any())).willReturn(Completable.complete())

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertResult()

        then(safeRepository).should().updateSafe(Safe(TEST_SAFE, null))
        then(safeRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertResult()

        then(safeRepository).should().updateSafe(Safe(TEST_SAFE, "New Name"))
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateSafeNameError() {
        val safe = Safe(TEST_SAFE, "Old Name")
        val error = NoSuchElementException()
        given(safeRepository.updateSafe(MockUtils.any())).willReturn(Completable.error(error))

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertError(error)

        then(safeRepository).should().updateSafe(Safe(TEST_SAFE, null))
        then(safeRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertError(error)

        then(safeRepository).should().updateSafe(Safe(TEST_SAFE, "New Name"))
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updatePendingSafeName() {
        val safe = PendingSafe(TEST_TX_HASH, "Old Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        given(safeRepository.updatePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertResult()

        then(safeRepository).should().updatePendingSafe(PendingSafe(TEST_TX_HASH, null, TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO))
        then(safeRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertResult()

        then(safeRepository).should().updatePendingSafe(PendingSafe(TEST_TX_HASH, "New Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO))
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updatePendingSafeNameError() {
        val safe = PendingSafe(TEST_TX_HASH, "Old Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        val error = NoSuchElementException()
        given(safeRepository.updatePendingSafe(MockUtils.any())).willReturn(Completable.error(error))

        val nullObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, null).subscribe(nullObserver)
        nullObserver.assertError(error)

        then(safeRepository).should().updatePendingSafe(PendingSafe(TEST_TX_HASH, null, TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO))
        then(safeRepository).shouldHaveNoMoreInteractions()

        val nameObserver = TestObserver<Unit>()
        viewModel.updateSafeName(safe, "New Name").subscribe(nameObserver)
        nameObserver.assertError(error)

        then(safeRepository).should().updatePendingSafe(PendingSafe(TEST_TX_HASH, "New Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO))
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSafe() {
        context.mockGetString()
        val safe = Safe(TEST_SAFE, "Old Name")
        val safeProcessor = PublishProcessor.create<Safe>()
        given(safeRepository.observeSafe(MockUtils.any())).willReturn(safeProcessor)

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(safe).subscribe(safeSubscriber)

        safeProcessor.offer(Safe(TEST_SAFE, "Old Name"))
        safeSubscriber
            .assertValueCount(1)
            .assertValueAt(0, "Old Name" to "0x1f81...C65C7E")

        safeProcessor.offer(Safe(TEST_SAFE, null))
        safeSubscriber
            .assertValueCount(2)
            .assertValueAt(1, R.string.your_safe.toString() to "0x1f81...C65C7E")

        then(safeRepository).should().observeSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSafeError() {
        val error = NoSuchElementException()
        given(safeRepository.observeSafe(MockUtils.any())).willReturn(Flowable.error(error))

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(Safe(TEST_SAFE, "Old Name")).subscribe(safeSubscriber)

        safeSubscriber.assertFailure(Predicate { it == error })

        then(safeRepository).should().observeSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observePendingSafe() {
        context.mockGetString()
        val safe = PendingSafe(TEST_TX_HASH, "Old Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        val safeProcessor = PublishProcessor.create<PendingSafe>()
        given(safeRepository.observePendingSafe(MockUtils.any())).willReturn(safeProcessor)

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(safe).subscribe(safeSubscriber)

        safeProcessor.offer(PendingSafe(TEST_TX_HASH, "Old Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO))
        safeSubscriber
            .assertValueCount(1)
            .assertValueAt(0, "Old Name" to "0x1f81...C65C7E")

        safeProcessor.offer(PendingSafe(TEST_TX_HASH, null, TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO))
        safeSubscriber
            .assertValueCount(2)
            .assertValueAt(1, R.string.your_safe.toString() to "0x1f81...C65C7E")

        then(safeRepository).should().observePendingSafe(TEST_TX_HASH)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observePendingSafeError() {
        val error = NoSuchElementException()
        given(safeRepository.observePendingSafe(MockUtils.any())).willReturn(Flowable.error(error))

        val safeSubscriber = TestSubscriber<Pair<String, String>>()
        viewModel.observeSafe(PendingSafe(TEST_TX_HASH, "Old Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)).subscribe(safeSubscriber)

        safeSubscriber.assertFailure(Predicate { it == error })

        then(safeRepository).should().observePendingSafe(TEST_TX_HASH)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removeSafe() {
        val safe = Safe(TEST_SAFE, "Old Name")
        given(safeRepository.removeSafe(MockUtils.any())).willReturn(Completable.complete())

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertResult()

        then(safeRepository).should().removeSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removeSafeError() {
        val safe = Safe(TEST_SAFE, "Old Name")
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
        val safe = PendingSafe(TEST_TX_HASH, "Old Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        given(safeRepository.removePendingSafe(MockUtils.any())).willReturn(Completable.complete())

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertResult()

        then(safeRepository).should().removePendingSafe(TEST_TX_HASH)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removePendingSafeError() {
        val safe = PendingSafe(TEST_TX_HASH, "Old Name", TEST_SAFE, TEST_PAYMENT_TOKEN, BigInteger.ZERO)
        val error = NoSuchElementException()
        given(safeRepository.removePendingSafe(MockUtils.any())).willReturn(Completable.error(error))

        val testObserver = TestObserver<Unit>()
        viewModel.removeSafe(safe).subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == error })

        then(safeRepository).should().removePendingSafe(TEST_TX_HASH)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_TX_HASH = "0xdae721569a948b87c269ebacaa5a4a67728095e32f9e7e4626f109f27a73b40f".hexAsBigInteger()
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_PENDING_SAFE = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_PAYMENT_TOKEN = ERC20Token.ETHER_TOKEN.address
        private val TEST_PAYMENT_AMOUNT = Wei.ether("0.1").value
        private const val KEY_SELECTED_SAFE = "safe_main.string.selected_safe"
    }
}
