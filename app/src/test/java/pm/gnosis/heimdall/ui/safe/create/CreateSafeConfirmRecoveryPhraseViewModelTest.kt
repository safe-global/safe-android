package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.asOwner
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.lang.IllegalStateException

@RunWith(MockitoJUnitRunner::class)
class CreateSafeConfirmRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    private lateinit var viewModel: CreateSafeConfirmRecoveryPhraseViewModel

    @Before
    fun setup() {
        viewModel = CreateSafeConfirmRecoveryPhraseViewModel(
            accountsRepositoryMock
        )
        // Setup parent class
        viewModel.setup(RECOVERY_PHRASE)
    }

    @Test
    fun loadOwnerDataNoBrowserExtensionNoOwner() {
        given(accountsRepositoryMock.createOwnersFromPhrase(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(listOf(MNEMONIC_1_ADDRESS.asOwner(), MNEMONIC_2_ADDRESS.asOwner())))

        viewModel.setup(null)

        val testObserver = TestObserver<Pair<AuthenticatorSetupInfo?, List<Solidity.Address>>>()
        viewModel.loadOwnerData().subscribe(testObserver)
        testObserver.assertResult(null to listOf(MNEMONIC_1_ADDRESS, MNEMONIC_2_ADDRESS))

        then(accountsRepositoryMock).should().createOwnersFromPhrase(RECOVERY_PHRASE, listOf(0, 1))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadOwnerDataOwnerAndBrowserExtension() {
        given(accountsRepositoryMock.createOwnersFromPhrase(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(listOf(MNEMONIC_1_ADDRESS.asOwner(), MNEMONIC_2_ADDRESS.asOwner())))

        val deviceOwner = DEVICE_OWNER_ADDRESS.asOwner()
        val info = AuthenticatorSetupInfo(deviceOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, BROWSER_EXTENSION_ADDRESS))
        viewModel.setup(info)

        val testObserver = TestObserver<Pair<AuthenticatorSetupInfo?, List<Solidity.Address>>>()
        viewModel.loadOwnerData().subscribe(testObserver)
        testObserver.assertResult(info to listOf(BROWSER_EXTENSION_ADDRESS, MNEMONIC_1_ADDRESS, MNEMONIC_2_ADDRESS))

        then(accountsRepositoryMock).should().createOwnersFromPhrase(RECOVERY_PHRASE, listOf(0, 1))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadOwnerDataError() {
        val error = IllegalStateException()
        given(accountsRepositoryMock.createOwnersFromPhrase(MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(error))

        val deviceOwner = DEVICE_OWNER_ADDRESS.asOwner()
        val info = AuthenticatorSetupInfo(deviceOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, BROWSER_EXTENSION_ADDRESS))
        viewModel.setup(info)

        val testObserver = TestObserver<Pair<AuthenticatorSetupInfo?, List<Solidity.Address>>>()
        viewModel.loadOwnerData().subscribe(testObserver)
        testObserver.assertFailure(Predicate { it == error })

        then(accountsRepositoryMock).should().createOwnersFromPhrase(RECOVERY_PHRASE, listOf(0, 1))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private val DEVICE_OWNER_ADDRESS = "0xbaddad".asEthereumAddress()!!
        private val BROWSER_EXTENSION_ADDRESS = "0xdead".asEthereumAddress()!!
        private val MNEMONIC_1_ADDRESS = "0xbeef01".asEthereumAddress()!!
        private val MNEMONIC_2_ADDRESS = "0xbeef02".asEthereumAddress()!!
        private const val RECOVERY_PHRASE = "degree media athlete harvest rocket plate minute obey head toward coach senior"
    }
}
