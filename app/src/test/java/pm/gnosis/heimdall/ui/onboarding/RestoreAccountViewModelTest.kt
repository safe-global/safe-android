package pm.gnosis.heimdall.ui.onboarding

import io.reactivex.Single
import io.reactivex.internal.operators.completable.CompletableError
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.TestCompletable

@RunWith(MockitoJUnitRunner::class)
class RestoreAccountViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var accountsRepository: AccountsRepository

    private lateinit var viewModel: RestoreAccountContract

    private val testMnemonic = "abstract inspire axis monster urban order rookie over volume poverty horse rack"

    @Before
    fun setUp() {
        viewModel = RestoreAccountViewModel(accountsRepository)
    }

    @Test
    fun isValidMnemonicWithValidMnemonic() {
        val testObserver = TestObserver.create<Boolean>()
        given(accountsRepository.validateMnemonic(anyString())).willReturn(Single.just(true))

        viewModel.isValidMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepository).should().validateMnemonic(testMnemonic)
        testObserver.assertValue(true)
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun isValidMnemonicLess12Words() {
        val testObserver = TestObserver.create<Boolean>()
        val invalidMnemonic = "less than 12 words"

        viewModel.isValidMnemonic(invalidMnemonic).subscribe(testObserver)

        then(accountsRepository).should(never()).validateMnemonic(anyString())
        testObserver.assertValue(false)
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun isValidMnemonicWithInvalidMnemonic() {
        val testObserver = TestObserver.create<Boolean>()
        given(accountsRepository.validateMnemonic(anyString())).willReturn(Single.just(false))

        viewModel.isValidMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepository).should().validateMnemonic(testMnemonic)
        testObserver.assertValue(false)
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun saveAccountWithMnemonic() {
        val testObserver = TestObserver.create<Unit>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val saveMnemonicCompletable = TestCompletable()
        given(accountsRepository.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepository.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        assertEquals(1, saveMnemonicCompletable.callCount)
        testObserver.assertNoValues()
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun saveAccountWithMnemonicErrorOnSaveAccount() {
        val testObserver = TestObserver.create<Unit>()
        val saveMnemonicCompletable = TestCompletable()
        val exception = Exception()
        given(accountsRepository.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(CompletableError(exception))
        given(accountsRepository.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        assertEquals(0, saveMnemonicCompletable.callCount)
        testObserver.assertNoValues()
                .assertError(exception)
                .assertTerminated()
    }

    @Test
    fun saveAccountWithMnemonicErrorOnSaveMnemonic() {
        val testObserver = TestObserver.create<Unit>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val exception = Exception()
        given(accountsRepository.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepository.saveMnemonic(anyString())).willReturn(CompletableError(exception))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        testObserver.assertNoValues()
                .assertError(exception)
                .assertTerminated()
    }
}
