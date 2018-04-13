package pm.gnosis.heimdall.ui.settings.tokens

import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class TokenManagementViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: TokenManagementViewModel

    @Before
    fun setup() {
        viewModel = TokenManagementViewModel(tokenRepositoryMock)
    }

    @Test
    fun testObserveVerifiedTokens() {
        val processor = PublishProcessor.create<List<ERC20Token>>()
        val testSubscriber = TestSubscriber<Adapter.Data<ERC20Token>>()
        val list = listOf(ERC20Token(Solidity.Address(BigInteger.ONE), decimals = 18))
        given(tokenRepositoryMock.observeTokens()).willReturn(processor)

        viewModel.observeVerifiedTokens().subscribe(testSubscriber)
        processor.offer(list)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testSubscriber
            .assertValueCount(2)
            .assertValueAt(0, Adapter.Data())
            .assertValueAt(1, { it.entries == list })

        val previous = testSubscriber.values()[1]
        val list2 = listOf(ERC20Token(Solidity.Address(BigInteger.ZERO), decimals = 18))
        processor.offer(list2)
        testSubscriber.assertValueCount(3)
            .assertValueAt(2, { it.diff != null && it.parentId == previous.id && it.entries == list2 })
    }

    @Test
    fun testObserveVerifiedTokensError() {
        val testSubscriber = TestSubscriber<Adapter.Data<ERC20Token>>()
        val exception = Exception()
        given(tokenRepositoryMock.observeTokens()).willReturn(Flowable.error(exception))

        viewModel.observeVerifiedTokens().subscribe(testSubscriber)

        then(tokenRepositoryMock).should().observeTokens()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testSubscriber.assertValueCount(1)
            .assertValueAt(0, Adapter.Data())
            .assertError(exception)
    }
}
