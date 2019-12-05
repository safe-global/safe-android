package pm.gnosis.heimdall.ui.safe.pairing

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.*
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.math.RoundingMode

@RunWith(MockitoJUnitRunner::class)
class PairingStartViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @JvmField
    @Rule
    val lifecycleRule = TestLifecycleRule()

    private lateinit var viewModel: PairingStartViewModel

    @Mock
    private lateinit var recoverSafeOwnersHelper: RecoverSafeOwnersHelper

    @Mock
    private lateinit var gnosisSafeRepository: GnosisSafeRepository

    @Mock
    private lateinit var accountsRepository: AccountsRepository

    @Mock
    private lateinit var tokenRepository: TokenRepository

    @Mock
    private lateinit var transactionExecutionRepository: TransactionExecutionRepository

    private val encryptedByteArrayConverter = EncryptedByteArray.Converter()


    @Before
    fun setup() {
        viewModel = PairingStartViewModel(
            recoverSafeOwnersHelper,
            gnosisSafeRepository,
            accountsRepository,
            tokenRepository,
            transactionExecutionRepository,
            testAppDispatchers
        )

        viewModel.setup(SAFE_ADDRESS)
    }

    @Test
    fun estimate() {

        val paymentToken = ERC20Token.ETHER_TOKEN

        val signingOwner = AccountsRepository.SafeOwner(PHONE_ADDRESS, encryptedByteArrayConverter.fromStorage("encrypted key"))

        given(tokenRepository.loadPaymentToken(SAFE_ADDRESS)).willReturn(Single.just(paymentToken))

        given(gnosisSafeRepository.loadInfo(SAFE_ADDRESS)).willReturn(Observable.just(SAFE_INFO))

        given(accountsRepository.signingOwner(SAFE_ADDRESS)).willReturn(Single.just(signingOwner))

        val executionInfo = TransactionExecutionRepository.ExecuteInformation(
            TEST_TRANSACTION_HASH,
            TEST_TRANSACTION,
            PHONE_ADDRESS,
            TEST_OWNERS.size,
            TEST_OWNERS,
            SemVer(1, 0, 0),
            ERC20Token.ETHER_TOKEN.address,
            BigInteger.ONE,
            BigInteger.TEN,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.valueOf(100)
        )
        given(transactionExecutionRepository.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(
                Single.just(
                    executionInfo
                )
            )

        val testObserver = TestLiveDataObserver<PairingStartContract.ViewUpdate>()
        viewModel.observableState.observe(lifecycleRule, testObserver)

        viewModel.estimate()

        testObserver.assertValues(
            PairingStartContract.ViewUpdate.Balance(
                paymentToken.displayString(executionInfo.balance),
                ERC20TokenWithBalance(paymentToken, executionInfo.gasCosts()).displayString(roundingMode = RoundingMode.UP),
                paymentToken.displayString(executionInfo.balance - executionInfo.gasCosts()),
                paymentToken.symbol,
                executionInfo.balance > executionInfo.gasCosts()
            )
        )
    }

    @Test
    fun loadAuthenticatorInfo() {

        val signingOwner = AccountsRepository.SafeOwner(PHONE_ADDRESS, encryptedByteArrayConverter.fromStorage("encrypted key"))
        val authenticatorInfo = AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, FACTOR_ADDRESS)

        given(accountsRepository.signingOwner(SAFE_ADDRESS)).willReturn(Single.just(signingOwner))
        given(gnosisSafeRepository.loadAuthenticatorInfo(FACTOR_ADDRESS)).willReturn(authenticatorInfo)

        val testObserver = TestLiveDataObserver<PairingStartContract.ViewUpdate>()
        viewModel.observableState.observe(lifecycleRule, testObserver)

        viewModel.loadAuthenticatorInfo(SAFE_INFO)

        testObserver.assertValues(
            PairingStartContract.ViewUpdate.Authenticator(
                AuthenticatorSetupInfo(signingOwner, authenticatorInfo)
            )
        )
    }

    @Test
    fun authenticatorInfoMissing() {

        val signingOwner = AccountsRepository.SafeOwner(PHONE_ADDRESS, encryptedByteArrayConverter.fromStorage("encrypted key"))
        val exception = NullPointerException()

        given(accountsRepository.signingOwner(SAFE_ADDRESS)).willReturn(Single.just(signingOwner))
        given(gnosisSafeRepository.loadAuthenticatorInfo(MockUtils.any())).willThrow(exception)

        val testObserver = TestLiveDataObserver<PairingStartContract.ViewUpdate>()
        viewModel.observableState.observe(lifecycleRule, testObserver)

        viewModel.loadAuthenticatorInfo(SAFE_INFO)

        testObserver.assertValues(
            PairingStartContract.ViewUpdate.Authenticator(
                AuthenticatorSetupInfo(signingOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, FACTOR_ADDRESS))
            )
        )
    }
    
    companion object {

        private val SAFE_ADDRESS = "0xdeadbeef".asEthereumAddress()!!

        private val PHONE_ADDRESS = "0x41".asEthereumAddress()!!
        private val FACTOR_ADDRESS = "0x42".asEthereumAddress()!!
        private val RECOVERY_ADDRESS_0 = "0x43".asEthereumAddress()!!
        private val RECOVERY_ADDRESS_1 = "0x44".asEthereumAddress()!!

        private val SAFE_INFO = SafeInfo(
            address = SAFE_ADDRESS,
            balance = Wei.ZERO,
            requiredConfirmations = 2L,
            owners = listOf(PHONE_ADDRESS, FACTOR_ADDRESS, RECOVERY_ADDRESS_0, RECOVERY_ADDRESS_1),
            isOwner = true,
            modules = emptyList(),
            version = SemVer(1, 0, 0)
        )

        private const val TEST_TRANSACTION_HASH = "SomeHash"
        private val TEST_TRANSACTION =
            SafeTransaction(Transaction(Solidity.Address(BigInteger.ZERO), nonce = BigInteger.TEN), TransactionExecutionRepository.Operation.CALL)
        private val TEST_SIGNERS = listOf(PHONE_ADDRESS)
        private val TEST_OWNERS = TEST_SIGNERS + FACTOR_ADDRESS
    }
}
