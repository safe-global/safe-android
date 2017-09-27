package pm.gnosis.heimdall.ui.onboarding

import io.reactivex.Single
import io.reactivex.internal.operators.completable.CompletableError
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.TestCompletable

@RunWith(MockitoJUnitRunner::class)
class GenerateMnemonicViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var accountsRepositoryMock: AccountsRepository

    lateinit var viewModel: GenerateMnemonicContract

    private val testMnemonic = "abstract inspire axis monster urban order rookie over volume poverty horse rack"

    @Before
    fun setUp() {
        viewModel = GenerateMnemonicViewModel(accountsRepositoryMock)
    }

    @Test
    fun testGenerateMnemonic() {
        val testObserver = TestObserver.create<Result<String>>()
        given(accountsRepositoryMock.generateMnemonic()).willReturn(Single.just(testMnemonic))

        viewModel.generateMnemonic().subscribe(testObserver)

        then(accountsRepositoryMock).should().generateMnemonic()
        testObserver.assertNoErrors()
                .assertComplete()
                .assertValue { (data, error) -> data == testMnemonic && error == null }
    }

    @Test
    fun testGenerateMnemonicError() {
        val testObserver = TestObserver.create<Result<String>>()
        val exception = Exception()
        given(accountsRepositoryMock.generateMnemonic()).willReturn(Single.error(exception))

        viewModel.generateMnemonic().subscribe(testObserver)

        then(accountsRepositoryMock).should().generateMnemonic()
        testObserver.assertNoErrors()
                .assertComplete()
                .assertValue { (data, error) -> data == null && error == exception }
    }

    @Test
    fun testSaveAccountWithMnemonic() {
        val testObserver = TestObserver.create<Result<String>>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val saveMnemonicCompletable = TestCompletable()
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        assertEquals(1, saveMnemonicCompletable.callCount)
        testObserver.assertNoErrors()
                .assertTerminated()
                .assertNoValues()
    }

    @Test
    fun testSaveAccountWithMnemonicErrorOnSaveAccount() {
        val testObserver = TestObserver.create<Result<String>>()
        val exception = Exception()
        val saveMnemonicCompletable = TestCompletable()
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(CompletableError(exception))
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        assertEquals(0, saveMnemonicCompletable.callCount)
        testObserver.assertError(exception)
                .assertTerminated()
                .assertNoValues()
    }

    @Test
    fun testSaveAccountWithMnemonicErrorOnSaveMnemonic() {
        val testObserver = TestObserver.create<Result<String>>()
        val exception = Exception()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(CompletableError(exception))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        testObserver.assertError(exception)
                .assertTerminated()
                .assertNoValues()
    }
}
