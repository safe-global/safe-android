package pm.gnosis.heimdall.ui.settings.security.revealmnemonic

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.tests.utils.ImmediateSchedulersRule

@RunWith(MockitoJUnitRunner::class)
class RevealMnemonicViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    private lateinit var viewModel: RevealMnemonicViewModel

    @Before
    fun setUp() {
        viewModel = RevealMnemonicViewModel(accountsRepositoryMock)
    }

    @Test
    fun loadMnemonic() {
        val testObserver = TestObserver<String>()
        val mnemonic = "mnemonic"
        given(accountsRepositoryMock.loadMnemonic()).willReturn(Single.just(mnemonic))

        viewModel.loadMnemonic().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadMnemonic()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(mnemonic)
    }

    @Test
    fun loadMnemonicError() {
        val testObserver = TestObserver<String>()
        val exception = Exception()
        given(accountsRepositoryMock.loadMnemonic()).willReturn(Single.error(exception))

        viewModel.loadMnemonic().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadMnemonic()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }
}
