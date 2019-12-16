package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeDeployment
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.create.CreateSafePaymentTokenContract.ViewAction.*
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import java.net.UnknownHostException
import pm.gnosis.heimdall.ui.safe.create.CreateSafePaymentTokenContract.State as ViewState

@RunWith(MockitoJUnitRunner::class)
class CreateSafePaymentTokenViewModelTest {

    @JvmField
    @Rule
    val lifecycleRule = TestLifecycleRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: CreateSafePaymentTokenViewModel

    @Before
    fun setup() {
        viewModel = CreateSafePaymentTokenViewModel(contextMock, testAppDispatchers, accountsRepositoryMock, safeRepositoryMock, tokenRepositoryMock)
    }

    @Test
    fun loadPaymentTokenTokenTwice() {
        viewModel.setup(null, listOf(BROWSER_EXTENSION_ADDRESS, MNEMONIC_1_ADDRESS, MNEMONIC_2_ADDRESS))

        val testObserver = TestLiveDataObserver<ViewState>()
        viewModel.state.observe(lifecycleRule, testObserver)
        // Initial state
        testObserver.assertValues(ViewState(null, null, false, null)).clear()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()

        val tokenSingle = TestSingleFactory<ERC20Token>()
        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any())).willReturn(tokenSingle.get())

        viewModel.loadPaymentToken()
        testObserver.assertValues(ViewState(null, null, false, null)).clear()
        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        // Should not load again if already loading
        viewModel.loadPaymentToken()
        testObserver.assertEmpty()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        val estimatesSingle = TestSingleFactory<List<Pair<ERC20Token, BigInteger>>>()
        given(tokenRepositoryMock.loadPaymentTokensWithCreationFees(anyLong())).willReturn(estimatesSingle.get())

        // Emit payment tokens as soon as it is loaded
        tokenSingle.success(ERC20Token.ETHER_TOKEN)
        testObserver.assertValues(ViewState(ERC20Token.ETHER_TOKEN, null, true, null)).clear()
        then(tokenRepositoryMock).should().loadPaymentTokensWithCreationFees(4)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        // Emit estimates as soon as it is loaded
        estimatesSingle.success(listOf(ERC20Token.ETHER_TOKEN to BigInteger.TEN, TEST_TOKEN to BigInteger.ONE))
        testObserver.assertValues(ViewState(ERC20Token.ETHER_TOKEN, BigInteger.TEN, true, null)).clear()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        // Loading again should keep the fee
        viewModel.loadPaymentToken()
        testObserver.assertValues(ViewState(ERC20Token.ETHER_TOKEN, BigInteger.TEN, false, null)).clear()
        then(tokenRepositoryMock).should(times(2)).loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        // Emit payment tokens as soon as it is loaded
        tokenSingle.success(ERC20Token.ETHER_TOKEN)
        testObserver.assertValues(ViewState(ERC20Token.ETHER_TOKEN, BigInteger.TEN, true, null)).clear()
        then(tokenRepositoryMock).should(times(2)).loadPaymentTokensWithCreationFees(4)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        // Emit estimates should update as it is loaded
        estimatesSingle.success(listOf(ERC20Token.ETHER_TOKEN to BigInteger.valueOf(12), TEST_TOKEN to BigInteger.ONE))
        testObserver.assertValues(ViewState(ERC20Token.ETHER_TOKEN, BigInteger.valueOf(12), true, null)).clear()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadPaymentTokenNoEstimate() {
        viewModel.setup(null, listOf(BROWSER_EXTENSION_ADDRESS))

        val testObserver = TestLiveDataObserver<ViewState>()
        viewModel.state.observe(lifecycleRule, testObserver)
        // Initial state
        testObserver.assertValues(ViewState(null, null, false, null)).clear()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()

        val tokenSingle = TestSingleFactory<ERC20Token>()
        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any())).willReturn(tokenSingle.get())

        viewModel.loadPaymentToken()
        testObserver.assertValues(ViewState(null, null, false, null)).clear()
        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        // Should not load again if already loading
        viewModel.loadPaymentToken()
        testObserver.assertEmpty()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        val estimatesSingle = TestSingleFactory<List<Pair<ERC20Token, BigInteger>>>()
        given(tokenRepositoryMock.loadPaymentTokensWithCreationFees(anyLong())).willReturn(estimatesSingle.get())

        // Emit payment tokens as soon as it is loaded
        tokenSingle.success(ERC20Token.ETHER_TOKEN)
        testObserver.assertValues(ViewState(ERC20Token.ETHER_TOKEN, null, true, null)).clear()
        then(tokenRepositoryMock).should().loadPaymentTokensWithCreationFees(2)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()

        // Emit estimates as soon as it is loaded
        estimatesSingle.success(listOf(TEST_TOKEN to BigInteger.ONE))
        testObserver.assertEmpty()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadPaymentTokenError() {
        contextMock.mockGetString()
        viewModel.setup(null, listOf())

        val testObserver = TestLiveDataObserver<ViewState>()
        viewModel.state.observe(lifecycleRule, testObserver)
        // Initial state
        testObserver.assertValues(ViewState(null, null, false, null)).clear()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()

        val error = UnknownHostException()
        given(tokenRepositoryMock.loadPaymentToken(MockUtils.any())).willReturn(Single.error(error))

        viewModel.loadPaymentToken()
        testObserver.assertValues(
            ViewState(null, null, false, null),
            ViewState(null, null, true, ShowError(SimpleLocalizedException(R.string.error_check_internet_connection.toString())))
        ).clear()
        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun createSafeNoOwner() {
        viewModel.setup(null, listOf(BROWSER_EXTENSION_ADDRESS, MNEMONIC_1_ADDRESS, MNEMONIC_2_ADDRESS))

        val testObserver = TestLiveDataObserver<ViewState>()
        viewModel.state.observe(lifecycleRule, testObserver)
        testObserver.assertValues(ViewState(null, null, false, null)).clear()

        val ownerSingle = TestSingleFactory<AccountsRepository.SafeOwner>()
        given(accountsRepositoryMock.createOwner()).willReturn(ownerSingle.get())

        viewModel.createSafe()
        testObserver.assertValues(ViewState(null, null, false, null)).clear()
        then(accountsRepositoryMock).should().createOwner()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()

        // Should not trigger it again while already in progress
        viewModel.createSafe()
        testObserver.assertEmpty()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()

        given(safeRepositoryMock.triggerSafeDeployment(MockUtils.any(), anyInt()))
            .willReturn(Single.just(SafeDeployment(TEST_SAFE, TEST_TOKEN.address, BigInteger.TEN)))
        given(safeRepositoryMock.addPendingSafe(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Completable.complete())
        given(safeRepositoryMock.saveOwner(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        val owner = DEVICE_OWNER_ADDRESS.asOwner()
        ownerSingle.success(owner)
        testObserver.assertValues(ViewState(null, null, false, ShowSafe(TEST_SAFE))).clear()
        then(safeRepositoryMock).should()
            .triggerSafeDeployment(listOf(DEVICE_OWNER_ADDRESS, BROWSER_EXTENSION_ADDRESS, MNEMONIC_1_ADDRESS, MNEMONIC_2_ADDRESS), 2)
        then(safeRepositoryMock).should().addPendingSafe(TEST_SAFE, null, BigInteger.TEN, TEST_TOKEN.address)
        then(safeRepositoryMock).should().saveOwner(TEST_SAFE, owner)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun createSafeWithOwner() {
        val deviceOwner = DEVICE_OWNER_ADDRESS.asOwner()
        val info = AuthenticatorSetupInfo(deviceOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, BROWSER_EXTENSION_ADDRESS))
        viewModel.setup(info, listOf(MNEMONIC_1_ADDRESS, MNEMONIC_2_ADDRESS))

        val testObserver = TestLiveDataObserver<ViewState>()
        viewModel.state.observe(lifecycleRule, testObserver)
        testObserver.assertValues(ViewState(null, null, false, null)).clear()

        given(safeRepositoryMock.triggerSafeDeployment(MockUtils.any(), anyInt()))
            .willReturn(Single.just(SafeDeployment(TEST_SAFE, TEST_TOKEN.address, BigInteger.TEN)))
        given(safeRepositoryMock.addPendingSafe(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Completable.complete())
        given(safeRepositoryMock.saveOwner(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())

        viewModel.createSafe()
        testObserver.assertValues(
            ViewState(null, null, false, null),
            ViewState(null, null, false, ShowSafe(TEST_SAFE))
        ).clear()
        then(safeRepositoryMock).should()
            .triggerSafeDeployment(listOf(DEVICE_OWNER_ADDRESS, MNEMONIC_1_ADDRESS, MNEMONIC_2_ADDRESS), 1)
        then(safeRepositoryMock).should().addPendingSafe(TEST_SAFE, null, BigInteger.TEN, TEST_TOKEN.address)
        then(safeRepositoryMock).should().saveOwner(TEST_SAFE, info.safeOwner)
        then(safeRepositoryMock).should().saveAuthenticatorInfo(info.authenticator)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun createSafeError() {
        contextMock.mockGetString()
        viewModel.setup(null, listOf(MNEMONIC_1_ADDRESS, MNEMONIC_2_ADDRESS))

        val error = UnknownHostException()
        given(accountsRepositoryMock.createOwner()).willReturn(Single.error(error))

        val testObserver = TestLiveDataObserver<ViewState>()
        viewModel.state.observe(lifecycleRule, testObserver)
        testObserver.assertValues(ViewState(null, null, false, null)).clear()

        viewModel.createSafe()
        testObserver.assertValues(
            ViewState(null, null, false, null),
            ViewState(null, null, true, ShowError(SimpleLocalizedException(R.string.error_check_internet_connection.toString())))
        ).clear()
        then(accountsRepositoryMock).should().createOwner()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    companion object {
        private val TEST_SAFE = "0xdeadbeef".asEthereumAddress()!!
        private val DEVICE_OWNER_ADDRESS = "0xbaddad".asEthereumAddress()!!
        private val BROWSER_EXTENSION_ADDRESS = "0xdead".asEthereumAddress()!!
        private val MNEMONIC_1_ADDRESS = "0xbeef01".asEthereumAddress()!!
        private val MNEMONIC_2_ADDRESS = "0xbeef02".asEthereumAddress()!!
        private val TEST_TOKEN = ERC20Token(
            "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!,
            name = "Test Token",
            symbol = "TT",
            decimals = 6
        )
    }

}