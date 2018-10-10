package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
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
import pm.gnosis.heimdall.ui.recoveryphrase.SetupRecoveryPhraseViewModel
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.words

@RunWith(MockitoJUnitRunner::class)
class SetupRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var bip39Mock: Bip39

    private lateinit var viewModel: SetupRecoveryPhraseViewModel

    @Before
    fun setup() {
        viewModel = SetupRecoveryPhraseViewModel(context, bip39Mock)
    }

    @Test
    fun generateRecoveryPhrase() {
        val mnemonic = "duty response mind sport casual response radar girl fetch robust weird lumber"
        val testObserver = TestObserver.create<List<String>>()
        given(bip39Mock.generateMnemonic(anyInt(), anyInt())).willReturn(mnemonic)

        viewModel.generateRecoveryPhrase().subscribe(testObserver)

        testObserver.assertResult(mnemonic.words())
        then(bip39Mock).should().generateMnemonic(languageId = R.id.english)
        then(bip39Mock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun generateRecoveryPhraseError() {
        val testObserver = TestObserver.create<List<String>>()
        given(bip39Mock.generateMnemonic(anyInt(), anyInt())).willThrow(IllegalArgumentException())

        viewModel.generateRecoveryPhrase().subscribe(testObserver)

        testObserver.assertFailure(IllegalArgumentException::class.java)
        then(bip39Mock).should().generateMnemonic(languageId = R.id.english)
        then(bip39Mock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun getRecoveryPhrase() {
        val mnemonic = "duty response mind sport casual response radar girl fetch robust weird lumber"
        val testObserver = TestObserver.create<List<String>>()
        given(bip39Mock.generateMnemonic(anyInt(), anyInt())).willReturn(mnemonic)

        viewModel.generateRecoveryPhrase().subscribe(testObserver)
        assertEquals(mnemonic, viewModel.getRecoveryPhrase())
    }
}
