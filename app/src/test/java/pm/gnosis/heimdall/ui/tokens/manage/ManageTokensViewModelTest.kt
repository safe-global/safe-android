package pm.gnosis.heimdall.ui.tokens.manage

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.assertEquals
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
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class ManageTokensViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: ManageTokensViewModel

    @Before
    fun setUp() {
        viewModel = ManageTokensViewModel(contextMock, tokenRepositoryMock)
    }

    @Test
    fun observeVerifiedTokens() {
        val enabledTokensProcessor = PublishProcessor.create<List<ERC20Token>>()
        val testObserver = TestObserver<Adapter.Data<ManageTokensContract.ERC20TokenEnabled>>()
        val verifiedToken = ERC20Token(Solidity.Address(BigInteger.TEN), "", "", 10, "")
        val enabledToken = TEST_TOKEN
        val loadEvents = PublishSubject.create<String>()
        given(tokenRepositoryMock.loadVerifiedTokens(MockUtils.any())).willReturn(Single.just(listOf(verifiedToken)))
        given(tokenRepositoryMock.observeEnabledTokens()).willReturn(enabledTokensProcessor)


        loadEvents.compose(viewModel.observeVerifiedTokens()).subscribe(testObserver)
        enabledTokensProcessor.offer(listOf(enabledToken))
        loadEvents.onNext("")

        testObserver
            .assertValueCount(2)
            .assertValueAt(0, {
                it.parentId == null && it.entries == emptyList<ManageTokensContract.ERC20TokenEnabled>()
            })
            .assertValueAt(1, {
                it.entries == listOf(
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(enabledToken, enabled = true)
                ) && it.parentId == testObserver.values()[0].id
            })
            .assertNoErrors()

        val newToken = ERC20Token(Solidity.Address(2.toBigInteger()), "", "", 18, "")
        enabledTokensProcessor.offer(listOf(enabledToken, newToken))

        testObserver
            .assertValueCount(3)
            .assertValueAt(2, {
                it.entries == listOf(
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(enabledToken, enabled = true),
                    ManageTokensContract.ERC20TokenEnabled(newToken, enabled = true)
                ) && it.parentId == testObserver.values()[1].id
            })
            .assertNoErrors()

        then(tokenRepositoryMock).should().loadVerifiedTokens("")
        then(tokenRepositoryMock).should().observeEnabledTokens()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeVerifiedTokensFilter() {
        val enabledTokensProcessor = PublishProcessor.create<List<ERC20Token>>()
        val testObserver = TestObserver<Adapter.Data<ManageTokensContract.ERC20TokenEnabled>>()
        val verifiedToken = ERC20Token(Solidity.Address(BigInteger.TEN), "", "", 10, "")
        val verifiedToken2 = ERC20Token(Solidity.Address(23.toBigInteger()), "Not Filtered Token", "NFT", 10, "")
        val enabledToken = TEST_TOKEN
        val enabledToken2 = ERC20Token(Solidity.Address(42.toBigInteger()), "Filtered Token", "FT", 10, "")
        val loadEvents = PublishSubject.create<String>()
        given(tokenRepositoryMock.loadVerifiedTokens(MockUtils.any())).willReturn(Single.just(listOf(verifiedToken, verifiedToken2)))
        given(tokenRepositoryMock.observeEnabledTokens()).willReturn(enabledTokensProcessor)


        loadEvents.compose(viewModel.observeVerifiedTokens()).subscribe(testObserver)
        then(tokenRepositoryMock).should().observeEnabledTokens()

        enabledTokensProcessor.offer(listOf(enabledToken, enabledToken2))
        loadEvents.onNext(TEST_TOKEN.name)
        then(tokenRepositoryMock).should().loadVerifiedTokens(TEST_TOKEN.name)

        // Backend tokens should not be filtered (only local tokens)
        testObserver
            .assertValueCount(2)
            .assertValueAt(0, {
                it.parentId == null && it.entries == emptyList<ManageTokensContract.ERC20TokenEnabled>()
            })
            .assertValueAt(1, {
                it.entries == listOf(
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken2, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(enabledToken, enabled = true)
                ) && it.parentId == testObserver.values()[0].id
            })
            .assertNoErrors()

        loadEvents.onNext("FT")
        then(tokenRepositoryMock).should().loadVerifiedTokens("FT")

        testObserver
            .assertValueCount(3)
            .assertValueAt(2, {
                it.entries == listOf(
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken2, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(enabledToken2, enabled = true)
                ) && it.parentId == testObserver.values()[1].id
            })
            .assertNoErrors()

        loadEvents.onNext("")
        then(tokenRepositoryMock).should().loadVerifiedTokens("")

        testObserver
            .assertValueCount(4)
            .assertValueAt(3, {
                it.entries == listOf(
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(verifiedToken2, enabled = false),
                    ManageTokensContract.ERC20TokenEnabled(enabledToken, enabled = true),
                    ManageTokensContract.ERC20TokenEnabled(enabledToken2, enabled = true)
                ) && it.parentId == testObserver.values()[2].id
            })
            .assertNoErrors()

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeVerifiedTokensError() {
        val testSubscriber = TestObserver<Adapter.Data<ManageTokensContract.ERC20TokenEnabled>>()
        val errorObserver = TestObserver<Throwable>()
        val exception = Exception()
        val loadEvents = PublishSubject.create<String>()
        given(tokenRepositoryMock.loadVerifiedTokens(MockUtils.any())).willReturn(Single.error(exception))
        given(tokenRepositoryMock.observeEnabledTokens()).willReturn(Flowable.just(listOf()))

        loadEvents.compose(viewModel.observeVerifiedTokens()).subscribe(testSubscriber)
        viewModel.observeErrors().subscribe(errorObserver)

        loadEvents.onNext("")

        errorObserver.assertValues(exception).assertNoErrors()
        testSubscriber
            .assertValueAt(0, {
                it == Adapter.Data<ManageTokensContract.ERC20TokenEnabled>()
            })
            .assertValueAt(1, {
                it.parentId == testSubscriber.values()[0].id && it.entries == listOf<ManageTokensContract.ERC20TokenEnabled>()
            }).assertNotTerminated()

        then(tokenRepositoryMock).should().loadVerifiedTokens("")
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
