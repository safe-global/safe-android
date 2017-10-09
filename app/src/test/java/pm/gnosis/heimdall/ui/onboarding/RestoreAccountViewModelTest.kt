package pm.gnosis.heimdall.ui.onboarding

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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.util.DataResult
import pm.gnosis.heimdall.common.util.ErrorResult
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.TestCompletable

@RunWith(MockitoJUnitRunner::class)
class RestoreAccountViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var accountsRepository: AccountsRepository

    @Mock
    lateinit var context: Context

    private lateinit var viewModel: RestoreAccountContract

    private val testMnemonic = "abstract inspire axis monster urban order rookie over volume poverty horse rack"

    @Before
    fun setUp() {
        viewModel = RestoreAccountViewModel(context, accountsRepository)
    }

    @Test
    fun saveAccountWithValidMnemonic() {
        val testObserver = TestObserver.create<Result<Intent>>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val saveMnemonicCompletable = TestCompletable()
        given(accountsRepository.validateMnemonic(anyString())).willReturn(Single.just(testMnemonic))
        given(accountsRepository.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepository.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepository).should().validateMnemonic(testMnemonic)
        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        assertEquals(1, saveMnemonicCompletable.callCount)
        testObserver.assertValue({ it is DataResult })
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun saveAccountWithInvalidMnemonic() {
        val testObserver = TestObserver.create<Result<Intent>>()
        given(accountsRepository.validateMnemonic(anyString())).willReturn(SingleError({ Exception() }))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepository).should().validateMnemonic(testMnemonic)
        testObserver.assertValue({ it is ErrorResult })
                .assertNoErrors()
                .assertTerminated()
    }

    @Test
    fun saveAccountWithMnemonicErrorOnSaveAccount() {
        val testObserver = TestObserver.create<Result<Intent>>()
        val saveMnemonicCompletable = TestCompletable()
        val exception = Exception()
        given(accountsRepository.validateMnemonic(anyString())).willReturn(Single.just(testMnemonic))
        given(accountsRepository.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(CompletableError(exception))
        given(accountsRepository.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepository).should().validateMnemonic(testMnemonic)
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
        given(accountsRepository.validateMnemonic(anyString())).willReturn(Single.just(testMnemonic))
        given(accountsRepository.saveAccountFromMnemonic(anyString(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepository.saveMnemonic(anyString())).willReturn(CompletableError(exception))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(accountsRepository).should().validateMnemonic(testMnemonic)
        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        testObserver.assertValue({ it is ErrorResult })
                .assertNoErrors()
                .assertTerminated()
    }
}
