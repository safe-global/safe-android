package pm.gnosis.heimdall.ui.tokens

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.util.DataResult
import pm.gnosis.heimdall.common.util.ErrorResult
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.exceptions.InvalidAddressException
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.test.utils.TestCompletable
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddTokenViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: AddTokenViewModel

    private val testAddress = BigInteger.ZERO
    private val testToken = ERC20Token(testAddress, decimals = 18)

    @Before
    fun setUp() {
        viewModel = AddTokenViewModel(tokenRepositoryMock)
    }

    @Test
    fun addToken() {
        val testObserver = TestObserver<Result<Unit>>()
        val testCompletable = TestCompletable()
        given(tokenRepositoryMock.addToken(MockUtils.any())).willReturn(testCompletable)

        viewModel.erc20Token = testToken
        viewModel.addToken().subscribe(testObserver)

        then(tokenRepositoryMock).should().addToken(testToken)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, testCompletable.callCount)
        testObserver.assertValue(DataResult(Unit)).assertNoErrors()
    }

    @Test
    fun addTokenNoTokenSet() {
        val testObserver = TestObserver<Result<Unit>>()

        viewModel.addToken().subscribe(testObserver)

        then(tokenRepositoryMock).shouldHaveZeroInteractions()
        assertNull(viewModel.erc20Token)
        testObserver.assertValue { it is ErrorResult && it.error is IllegalStateException }.assertNoErrors()
    }

    @Test
    fun loadTokenInfo() {
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.just(testToken))

        viewModel.loadTokenInfo(testAddress.asEthereumAddressString()).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(testToken, viewModel.erc20Token)
        testObserver.assertValue(DataResult(testToken)).assertNoErrors()
    }

    @Test
    fun loadTokenInfoInvalidAddress() {
        val testObserver = TestObserver<Result<ERC20Token>>()

        viewModel.loadTokenInfo("g").subscribe(testObserver)

        then(tokenRepositoryMock).shouldHaveZeroInteractions()
        assertNull(viewModel.erc20Token)
        testObserver.assertValue { it is ErrorResult && it.error is InvalidAddressException }.assertNoErrors()
    }

    @Test
    fun loadTokenInfoErrorLoadingInfo() {
        val testObserver = TestObserver<Result<ERC20Token>>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.just(testToken))

        viewModel.loadTokenInfo(testAddress.asEthereumAddressString()).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(testToken, viewModel.erc20Token)
        testObserver.assertValue(DataResult(testToken)).assertNoErrors()
    }
}
