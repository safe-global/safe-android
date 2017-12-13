package pm.gnosis.heimdall.ui.onboarding.account.create

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
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.TestCompletable

@RunWith(MockitoJUnitRunner::class)
class GenerateMnemonicViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    private lateinit var viewModel: GenerateMnemonicViewModel

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
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors()
                .assertComplete()
                .assertValue { it is DataResult && it.data == testMnemonic }
    }

    @Test
    fun testGenerateMnemonicError() {
        val testObserver = TestObserver.create<Result<String>>()
        val exception = Exception()
        given(accountsRepositoryMock.generateMnemonic()).willReturn(Single.error(exception))

        viewModel.generateMnemonic().subscribe(testObserver)

        then(accountsRepositoryMock).should().generateMnemonic()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors()
                .assertComplete()
                .assertValue { it is ErrorResult && it.error == exception }
    }

    @Test
    fun testSaveAccountWithMnemonic() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val saveMnemonicCompletable = TestCompletable()
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepositoryMock).should().saveAccountFromMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        assertEquals(1, saveMnemonicCompletable.callCount)
        testObserver.assertResult(DataResult(Unit))
    }

    @Test
    fun testSaveAccountWithMnemonicErrorOnSaveAccount() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        val saveMnemonicCompletable = TestCompletable()
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(CompletableError(exception))
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepositoryMock).should().saveAccountFromMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(0, saveMnemonicCompletable.callCount)
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun testSaveAccountWithMnemonicErrorOnSaveMnemonic() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val exception = Exception()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(CompletableError(exception))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepositoryMock).should().saveAccountFromMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        testObserver.assertResult(ErrorResult(exception))
    }
}
