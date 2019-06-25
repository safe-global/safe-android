package pm.gnosis.heimdall.ui.walletconnect.link

import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.safe.main.SafeMainViewModelTest
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.asEthereumAddress
import java.lang.IllegalStateException

@RunWith(MockitoJUnitRunner::class)
class WalletConnectLinkViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    private lateinit var viewModel: WalletConnectLinkViewModel

    @Before
    fun setUp() {
        viewModel = WalletConnectLinkViewModel(safeRepository)
    }

    @Test
    fun observeSafes() {
        val safeProcessor = BehaviorProcessor.create<List<Safe>>()
        given(safeRepository.observeSafes()).willReturn(safeProcessor)

        val safeSubscriber = TestSubscriber<Result<Adapter.Data<Safe>>>()
        viewModel.observeSafes().subscribe(safeSubscriber)

        then(safeRepository).should().observeSafes()
        then(safeRepository).shouldHaveNoMoreInteractions()


        safeSubscriber.assertValueCount(1).assertValueAt(0) {
            (it as DataResult).data.let { data ->
                data.entries == emptyList<Safe>()
                        && data.diff == null
                        && data.parentId == null
            }
        }

        val parentId = (safeSubscriber.values().last() as DataResult).data.id

        safeProcessor.offer(
            listOf(
                Safe(TEST_SAFE)
            )
        )

        safeSubscriber.assertValueCount(2).assertValueAt(1) {
            (it as DataResult).data.let { data ->
                data.entries == listOf(
                    Safe(TEST_SAFE)
                )
                        && data.diff != null
                        && data.parentId == parentId
            }
        }

        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeSafesError() {
        val error = IllegalStateException()
        given(safeRepository.observeSafes()).willReturn(Flowable.error(error))

        val safeSubscriber = TestSubscriber<Result<Adapter.Data<Safe>>>()
        viewModel.observeSafes().subscribe(safeSubscriber)

        then(safeRepository).should().observeSafes()
        then(safeRepository).shouldHaveNoMoreInteractions()

        safeSubscriber.assertResult(DataResult(Adapter.Data()), ErrorResult(error))

        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
    }
}