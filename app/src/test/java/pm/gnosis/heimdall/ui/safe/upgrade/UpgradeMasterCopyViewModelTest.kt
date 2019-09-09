package pm.gnosis.heimdall.ui.safe.upgrade

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestLifecycleRule

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UpgradeMasterCopyViewModelTest {

    @JvmField
    @Rule
    val lifecycleRule = TestLifecycleRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var addressBookRepositoryMock: AddressBookRepository

    @Mock
    lateinit var executionRepositoryMock: TransactionExecutionRepository

    @Mock
    lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: UpgradeMasterCopyViewModel

    @Before
    fun setup() {
        viewModel =
            UpgradeMasterCopyViewModel(contextMock, addressBookRepositoryMock, executionRepositoryMock, safeRepositoryMock, tokenRepositoryMock)
    }

    //@Test
    fun observe() {
        val lifecycleObserver = mutableListOf<LifecycleEventObserver>()
        val lifecycle = mock(Lifecycle::class.java)
        given(lifecycle.currentState).willReturn(Lifecycle.State.STARTED)
        given(lifecycle.addObserver(MockUtils.any())).willAnswer {
            lifecycleObserver.add(it.arguments.first() as LifecycleEventObserver)
            Unit
        }
        val lifecycleOwner = mock(LifecycleOwner::class.java)
        given(lifecycleOwner.lifecycle).willReturn(lifecycle)
        val testValues = mutableListOf<UpgradeMasterCopyContract.State>()
        viewModel.observe(lifecycleOwner) { testValues.add(it) }
        then(lifecycle).should().addObserver(MockUtils.any())

    }
}