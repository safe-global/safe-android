package pm.gnosis.heimdall.ui.tokens.manage

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Before
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
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class ManageTokensViewModelTest {
    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: ManageTokensViewModel

    @Before
    fun setUp() {
        viewModel = ManageTokensViewModel(tokenRepositoryMock)
    }

    @Test
    fun observeVerifiedTokens() {
        val enabledTokensProcessor = PublishProcessor.create<List<ERC20Token>>()
        val testSubscriber = TestSubscriber<Adapter.Data<ManageTokensContract.ERC20TokenEnabled>>()
        val verifiedToken = ERC20Token(Solidity.Address(BigInteger.TEN), "", "", 10, "")
        val enabledToken = TEST_TOKEN
        val refreshEvents = PublishProcessor.create<Unit>()
        given(tokenRepositoryMock.loadVerifiedTokens()).willReturn(Single.just(listOf(verifiedToken)))
        given(tokenRepositoryMock.observeEnabledTokens()).willReturn(enabledTokensProcessor)


        refreshEvents.compose(viewModel.observeVerifiedTokens()).subscribe(testSubscriber)
        enabledTokensProcessor.offer(listOf(enabledToken))
        refreshEvents.offer(Unit)

        testSubscriber
            .assertValueCount(2)
            .assertValueAt(0, {
                it.parentId == null && it.entries == emptyList<ManageTokensContract.ERC20TokenEnabled>()
            })
            .assertValueAt(1, {
                it.entries == listOf(
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(enabledToken, enabled = true)
                ) && it.parentId == testSubscriber.values()[0].id
            })
            .assertNoErrors()

        val newToken = ERC20Token(Solidity.Address(2.toBigInteger()), "", "", 18, "")
        enabledTokensProcessor.offer(listOf(enabledToken, newToken))

        testSubscriber
            .assertValueCount(3)
            .assertValueAt(2, {
                it.entries == listOf(
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(enabledToken, enabled = true),
                    ManageTokensContract.ERC20TokenEnabled(newToken, enabled = true)
                ) && it.parentId == testSubscriber.values()[1].id
            })
            .assertNoErrors()

        then(tokenRepositoryMock).should().loadVerifiedTokens()
        then(tokenRepositoryMock).should().observeEnabledTokens()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeVerifiedTokensError() {
        val testSubscriber = TestSubscriber<Adapter.Data<ManageTokensContract.ERC20TokenEnabled>>()
        val errorObserver = TestObserver<Throwable>()
        val exception = Exception()
        val refreshEvents = PublishProcessor.create<Unit>()
        given(tokenRepositoryMock.loadVerifiedTokens()).willReturn(Single.error(exception))
        given(tokenRepositoryMock.observeEnabledTokens()).willReturn(Flowable.just(listOf()))

        refreshEvents.compose(viewModel.observeVerifiedTokens()).subscribe(testSubscriber)
        viewModel.observeErrors().subscribe(errorObserver)

        refreshEvents.offer(Unit)

        errorObserver.assertValueAt(0, exception).assertNoErrors()
        testSubscriber.assertNotTerminated()

        then(tokenRepositoryMock).should().loadVerifiedTokens()
        then(tokenRepositoryMock).should().observeEnabledTokens()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun enableToken() {
        val testObserver = TestObserver<Result<Unit>>()
        val testCompletable = TestCompletable()
        given(tokenRepositoryMock.enableToken(MockUtils.any())).willReturn(testCompletable)

        viewModel.enableToken(TEST_TOKEN).subscribe(testObserver)

        testObserver.assertResult(DataResult(Unit))
        assertEquals(1, testCompletable.callCount)
        then(tokenRepositoryMock).should().enableToken(TEST_TOKEN)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun enableTokenError() {
        val testObserver = TestObserver<Result<Unit>>()
        val exception = Exception()
        given(tokenRepositoryMock.enableToken(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.enableToken(TEST_TOKEN).subscribe(testObserver)

        testObserver.assertResult(ErrorResult(exception))
        then(tokenRepositoryMock).should().enableToken(TEST_TOKEN)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun disableToken() {
        val testObserver = TestObserver<Result<Unit>>()
        val testCompletable = TestCompletable()
        given(tokenRepositoryMock.disableToken(MockUtils.any())).willReturn(testCompletable)

        viewModel.disableToken(TEST_TOKEN.address).subscribe(testObserver)

        testObserver.assertResult(DataResult(Unit))
        assertEquals(1, testCompletable.callCount)
        then(tokenRepositoryMock).should().disableToken(TEST_TOKEN.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun disableTokenError() {
        val testObserver = TestObserver<Result<Unit>>()
        val exception = Exception()
        given(tokenRepositoryMock.disableToken(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.disableToken(TEST_TOKEN.address).subscribe(testObserver)

        testObserver.assertResult(ErrorResult(exception))
        then(tokenRepositoryMock).should().disableToken(TEST_TOKEN.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        val TEST_TOKEN = ERC20Token(Solidity.Address(BigInteger.ONE), "Hello Token", "HT", 10)
    }
}
