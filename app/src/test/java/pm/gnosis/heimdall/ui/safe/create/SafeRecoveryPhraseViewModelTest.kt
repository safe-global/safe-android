package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils

@RunWith(MockitoJUnitRunner::class)
class SafeRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var bip39Mock: Bip39

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    private lateinit var viewModel: SafeRecoveryPhraseViewModel

    @Before
    fun setup() {
        viewModel = SafeRecoveryPhraseViewModel(bip39Mock, encryptionManagerMock)
    }

    @Test
    fun generateRecoveryPhrase() {
        val mnemonic = "duty response mind sport casual response radar girl fetch robust weird lumber"
        val testObserver = TestObserver.create<String>()
        given(bip39Mock.generateMnemonic(anyInt(), anyInt())).willReturn(mnemonic)

        viewModel.generateRecoveryPhrase().subscribe(testObserver)

        testObserver.assertResult(mnemonic)
        then(bip39Mock).should().generateMnemonic(languageId = R.id.english)
        then(bip39Mock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun generateRecoveryPhraseError() {
        val testObserver = TestObserver.create<String>()
        given(bip39Mock.generateMnemonic(anyInt(), anyInt())).willThrow(IllegalArgumentException())

        viewModel.generateRecoveryPhrase().subscribe(testObserver)

        testObserver.assertFailure(IllegalArgumentException::class.java)
        then(bip39Mock).should().generateMnemonic(languageId = R.id.english)
        then(bip39Mock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadEncryptedRecoveryPhrase() {
        val mnemonic = "duty response mind sport casual response radar girl fetch robust weird lumber"
        val testObserver = TestObserver.create<String>()
        val encryptedData = EncryptionManager.CryptoData(mnemonic.toByteArray(), ByteArray(16))
        given(bip39Mock.generateMnemonic(anyInt(), anyInt())).willReturn(mnemonic)
        given(encryptionManagerMock.encrypt(MockUtils.any())).willReturn(encryptedData)

        viewModel.generateRecoveryPhrase().subscribe()
        viewModel.loadEncryptedRecoveryPhrase().subscribe(testObserver)

        testObserver.assertResult(encryptedData.toString())
        then(bip39Mock).should().generateMnemonic(languageId = R.id.english)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().encrypt(mnemonic.toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadEncryptedRecoveryPhraseError() {
        val mnemonic = "duty response mind sport casual response radar girl fetch robust weird lumber"
        val testObserver = TestObserver.create<String>()
        given(bip39Mock.generateMnemonic(anyInt(), anyInt())).willReturn(mnemonic)
        given(encryptionManagerMock.encrypt(MockUtils.any())).willThrow(IllegalStateException::class.java)

        viewModel.generateRecoveryPhrase().subscribe()
        viewModel.loadEncryptedRecoveryPhrase().subscribe(testObserver)

        testObserver.assertFailure(IllegalStateException::class.java)
        then(bip39Mock).should().generateMnemonic(languageId = R.id.english)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().encrypt(mnemonic.toByteArray())
        then(encryptionManagerMock).shouldHaveZeroInteractions()
    }

    @Test
    fun loadEncryptedRecoveryPhraseNoMnemonic() {
        val testObserver = TestObserver.create<String>()

        viewModel.loadEncryptedRecoveryPhrase().subscribe(testObserver)

        testObserver.assertFailure(IllegalStateException::class.java)
        then(encryptionManagerMock).shouldHaveZeroInteractions()
    }

    @Test
    fun getChromeExtension() {
        val address = Solidity.Address(1.toBigInteger())

        viewModel.setup(address)

        assertEquals(address, viewModel.getChromeExtensionAddress())
    }
}
