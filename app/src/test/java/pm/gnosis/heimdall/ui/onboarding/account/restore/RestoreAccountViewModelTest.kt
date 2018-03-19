package pm.gnosis.heimdall.ui.onboarding.account.restore

import android.content.Context
import io.reactivex.internal.operators.completable.CompletableError
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
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.MnemonicNotInWordlist
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
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

    @Mock
    private lateinit var bip39Mock: Bip39

    private lateinit var viewModel: RestoreAccountViewModel

    private val testMnemonic = "abstract inspire axis monster urban order rookie over volume poverty horse rack"

    @Before
    fun setUp() {
        viewModel = RestoreAccountViewModel(contextMock, accountsRepositoryMock, bip39Mock)
    }

    @Test
    fun saveAccountWithValidMnemonic() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val saveMnemonicCompletable = TestCompletable()
        given(bip39Mock.validateMnemonic(anyString())).willReturn(testMnemonic)
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(byteArrayOf(0x0))
        given(accountsRepositoryMock.saveAccountFromMnemonicSeed(MockUtils.any(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(bip39Mock).should().validateMnemonic(testMnemonic)
        then(bip39Mock).should().mnemonicToSeed(testMnemonic)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().saveAccountFromMnemonicSeed(byteArrayOf(0x0))
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
        val testObserver = TestObserver.create<Result<Unit>>()
        given(bip39Mock.validateMnemonic(anyString())).willThrow(MnemonicNotInWordlist(testMnemonic))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(bip39Mock).should().validateMnemonic(testMnemonic)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveZeroInteractions()
        testObserver.assertValue({ it is ErrorResult })
            .assertNoErrors()
            .assertTerminated()
    }

    @Test
    fun saveAccountWithMnemonicErrorOnSaveAccount() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val saveMnemonicCompletable = TestCompletable()
        val exception = Exception()
        given(bip39Mock.validateMnemonic(anyString())).willReturn(testMnemonic)
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(byteArrayOf(0x0))
        given(accountsRepositoryMock.saveAccountFromMnemonicSeed(MockUtils.any(), anyLong())).willReturn(CompletableError(exception))
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(saveMnemonicCompletable)

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(bip39Mock).should().validateMnemonic(testMnemonic)
        then(bip39Mock).should().mnemonicToSeed(testMnemonic)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().saveAccountFromMnemonicSeed(byteArrayOf(0x0))
        then(accountsRepositoryMock).should().saveMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(0, saveMnemonicCompletable.callCount)
        testObserver.assertValue({ it is ErrorResult })
            .assertNoErrors()
            .assertTerminated()
    }

    @Test
    fun saveAccountWithMnemonicErrorOnSaveMnemonic() {
        val testObserver = TestObserver.create<Result<Unit>>()
        val saveAccountFromMnemonicCompletable = TestCompletable()
        val exception = Exception()
        given(bip39Mock.validateMnemonic(anyString())).willReturn(testMnemonic)
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(byteArrayOf(0x0))
        given(accountsRepositoryMock.saveAccountFromMnemonicSeed(MockUtils.any(), anyLong())).willReturn(saveAccountFromMnemonicCompletable)
        given(accountsRepositoryMock.saveMnemonic(anyString())).willReturn(CompletableError(exception))

        viewModel.saveAccountWithMnemonic(testMnemonic).subscribe(testObserver)

        then(bip39Mock).should().validateMnemonic(testMnemonic)
        then(bip39Mock).should().mnemonicToSeed(testMnemonic)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().saveAccountFromMnemonicSeed(byteArrayOf(0x0))
        then(accountsRepositoryMock).should().saveMnemonic(testMnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, saveAccountFromMnemonicCompletable.callCount)
        testObserver.assertValue({ it is ErrorResult })
            .assertNoErrors()
            .assertTerminated()
    }
}
