package pm.gnosis.heimdall.ui.safe.main

import android.app.Application
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.processors.BehaviorProcessor
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
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
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
import pm.gnosis.utils.*

@RunWith(MockitoJUnitRunner::class)
class SafeMainViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private val preferences = TestPreferences()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    private lateinit var preferencesManager: PreferencesManager

    private lateinit var viewModel: SafeMainViewModel

    @Before
    fun setUp() {
        BDDMockito.given(application.getSharedPreferences(anyString(), anyInt())).willReturn(preferences)
        preferencesManager = PreferencesManager(application)
        viewModel = SafeMainViewModel(preferencesManager, safeRepository)
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
                PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE, TEST_PAYMENT)
            )
        )

        safeSubscriber.assertValueCount(4).assertValueAt(3) {
            (it as DataResult).data.let {
                it.entries == listOf(
                    PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE, TEST_PAYMENT)
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
                    PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE, TEST_PAYMENT)
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
        val pendingSafe = PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE, TEST_PAYMENT)
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
        val pendingSafe = PendingSafe(TEST_TX_HASH, null, TEST_PENDING_SAFE, TEST_PAYMENT)
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

    companion object {
        private val TEST_TX_HASH = "0xdae721569a948b87c269ebacaa5a4a67728095e32f9e7e4626f109f27a73b40f".hexAsBigInteger()
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_PENDING_SAFE = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_PAYMENT = Wei.ether("0.1")
        private const val KEY_SELECTED_SAFE = "safe_main.string.selected_safe"
    }
}
