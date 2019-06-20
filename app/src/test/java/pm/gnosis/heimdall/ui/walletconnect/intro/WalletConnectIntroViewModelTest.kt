package pm.gnosis.heimdall.ui.walletconnect.intro

import io.reactivex.Completable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.tests.utils.ImmediateSchedulersRule

@RunWith(MockitoJUnitRunner::class)
class WalletConnectIntroViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var viewModel: WalletConnectIntroViewModel

    @Mock
    private lateinit var bridgeRepoMock: BridgeRepository

    @Before
    fun setUp() {
        viewModel = WalletConnectIntroViewModel(bridgeRepoMock)
    }

    @Test
    fun markIntroDone() {
        given(bridgeRepoMock.markIntroDone()).willReturn(Completable.complete())
        val testObserver = TestObserver<Unit>()
        viewModel.markIntroDone().subscribe(testObserver)
        testObserver.assertResult()
        then(bridgeRepoMock).should().markIntroDone()
        then(bridgeRepoMock).shouldHaveNoMoreInteractions()
    }
}