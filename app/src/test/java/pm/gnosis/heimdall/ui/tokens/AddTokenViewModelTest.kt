package pm.gnosis.heimdall.ui.tokens

import android.content.Context
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.test.utils.TestCompletable
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.tokens.addtoken.AddTokenViewModel
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddTokenViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: AddTokenViewModel

    private val testAddress = BigInteger.ZERO
    private val testToken = ERC20Token(testAddress, decimals = 18)

    @Before
    fun setUp() {
        viewModel = AddTokenViewModel(contextMock, tokenRepositoryMock)
        given(contextMock.getString(anyInt())).willReturn("")
    }

    @Test
    fun addToken() {
        val testObserver = TestObserver<Result<Unit>>()
        val testCompletable = TestCompletable()
        given(tokenRepositoryMock.addToken(MockUtils.any())).willReturn(testCompletable)
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.just(testToken))

        viewModel.loadTokenInfo(testAddress.asEthereumAddressString()).subscribe(TestObserver())
        viewModel.addToken().subscribe(testObserver)

        then(tokenRepositoryMock).should().addToken(testToken)
        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, testCompletable.callCount)
        testObserver.assertValue(DataResult(Unit)).assertNoErrors()
    }

    @Test
    fun addTokenNoTokenSet() {
        val testObserver = TestObserver<Result<Unit>>()

        viewModel.addToken().subscribe(testObserver)

        then(tokenRepositoryMock).shouldHaveZeroInteractions()
        testObserver.assertValue { it is ErrorResult && it.error is LocalizedException }.assertNoErrors()
    }

    @Test
    fun loadTokenInfo() {
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.just(testToken))

        viewModel.loadTokenInfo(testAddress.asEthereumAddressString()).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(testToken)).assertNoErrors()
    }

    @Test
    fun loadTokenInfoInvalidAddress() {
        val testObserver = TestObserver<Result<ERC20Token>>()

        viewModel.loadTokenInfo("g").subscribe(testObserver)

        println(testObserver.values())
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
        testObserver.assertValue { it is ErrorResult && it.error is LocalizedException }.assertNoErrors()
    }

    @Test
    fun loadTokenInfoErrorLoadingInfo() {
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.just(testToken))

        viewModel.loadTokenInfo(testAddress.asEthereumAddressString()).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(testToken)).assertNoErrors()
    }
}
