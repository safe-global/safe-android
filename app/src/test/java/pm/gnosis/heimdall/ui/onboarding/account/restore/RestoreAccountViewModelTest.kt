package pm.gnosis.heimdall.ui.onboarding.account.restore

import android.content.Context
import android.content.Intent
import io.reactivex.Single
import io.reactivex.internal.operators.completable.CompletableError
import io.reactivex.internal.operators.single.SingleError
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.TestCompletable

@RunWith(MockitoJUnitRunner::class)
class RestoreAccountViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var contextMock: Context

    private lateinit var viewModel: RestoreAccountViewModel

    private val testMnemonic = "abstract inspire axis monster urban order rookie over volume poverty horse rack"

    @Before
    fun setUp() {
        viewModel = RestoreAccountViewModel(contextMock, accountsRepositoryMock)
    }

    @Test
    fun saveAccountWithValidMnemonic() {
        val testObserver = TestObserver.create<Result<Intent>>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val saveMnemonicCompletable = TestCompletable()
        given(accountsRepositoryMock.validateMnemonic(anyString())).willReturn(Single.just(testMnemonic))
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepositoryMock).should().validateMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveAccountFromMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        assertEquals(1, saveMnemonicCompletable.callCount)
        testObserver.assertValue({ it is DataResult })
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun saveAccountWithInvalidMnemonic() {
        val testObserver = TestObserver.create<Result<Intent>>()
        given(accountsRepositoryMock.validateMnemonic(anyString())).willReturn(SingleError({ Exception() }))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepositoryMock).should().validateMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue({ it is ErrorResult })
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun saveAccountWithMnemonicErrorOnSaveAccount() {
        val testObserver = TestObserver.create<Result<Intent>>()
        val saveMnemonicCompletable = TestCompletable()
        val exception = Exception()
        given(accountsRepositoryMock.validateMnemonic(anyString())).willReturn(Single.just(testMnemonic))
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(CompletableError(exception))
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepositoryMock).should().validateMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveAccountFromMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(0, saveMnemonicCompletable.callCount)
        testObserver.assertValue({ it is ErrorResult })
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun saveAccountWithMnemonicErrorOnSaveMnemonic() {
        val testObserver = TestObserver.create<Result<Intent>>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val exception = Exception()
        given(accountsRepositoryMock.validateMnemonic(anyString())).willReturn(Single.just(testMnemonic))
        given(accountsRepositoryMock.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(CompletableError(exception))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepositoryMock).should().validateMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveAccountFromMnemonic(testMnemonic)
        then(accountsRepositoryMock).should().saveMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        testObserver.assertValue({ it is ErrorResult })
                .assertNoErrors()
                .assertTerminated()
    }
}
