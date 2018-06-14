package pm.gnosis.heimdall.ui.splash

import android.arch.persistence.room.EmptyResultSetException
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
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SplashViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var viewModel: SplashViewModel

    @Before
    fun setup() {
        viewModel = SplashViewModel(accountsRepositoryMock, encryptionManagerMock, tokenRepositoryMock)
    }

    @Test
    fun initialSetupTokenSetupErrorWithAccount() {
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(Account(Solidity.Address(BigInteger.ONE))))
        val observer = TestObserver.create<ViewAction>()

        viewModel.initialSetup().subscribe(observer)

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertTerminated()
            .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupErrorNoAccountNoSuchElement() {
        val observerNoSuchElement = TestObserver.create<ViewAction>()
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error<Account>(NoSuchElementException()))

        viewModel.initialSetup().subscribe(observerNoSuchElement)

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        observerNoSuchElement.assertNoErrors().assertTerminated()
            .assertValueCount(1).assertValue { it is StartPasswordSetup }
    }

    @Test
    fun initialSetupTokenSetupErrorNoAccountEmptyResultSet() {
        val observerEmptyResult = TestObserver.create<ViewAction>()
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error<Account>(EmptyResultSetException("")))

        viewModel.initialSetup().subscribe(observerEmptyResult)

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        observerEmptyResult.assertNoErrors().assertTerminated()
            .assertValueCount(1).assertValue { it is StartPasswordSetup }
    }

    @Test
    fun initialSetupTokenSetupErrorAccountError() {
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error<Account>(IllegalStateException()))
        val observer = TestObserver.create<ViewAction>()

        viewModel.initialSetup().subscribe(observer)

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertTerminated()
            .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupCompletedWithAccount() {
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(Account(Solidity.Address(BigInteger.ONE))))
        val observer = TestObserver.create<ViewAction>()

        viewModel.initialSetup().subscribe(observer)

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertTerminated()
            .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupCompletedNoAccountNoSuchElement() {
        val observerNoSuchElement = TestObserver.create<ViewAction>()
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error<Account>(NoSuchElementException()))

        viewModel.initialSetup().subscribe(observerNoSuchElement)

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        observerNoSuchElement.assertNoErrors().assertTerminated()
            .assertValueCount(1).assertValue { it is StartPasswordSetup }
    }

    @Test
    fun initialSetupTokenSetupCompletedNoAccountEmptyResultSet() {
        val observerEmptyResult = TestObserver.create<ViewAction>()
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error<Account>(EmptyResultSetException("")))

        viewModel.initialSetup().subscribe(observerEmptyResult)

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        observerEmptyResult.assertNoErrors().assertTerminated()
            .assertValueCount(1).assertValue { it is StartPasswordSetup }
    }

    @Test
    fun initialSetupTokenSetupCompletedAccountError() {
        given(encryptionManagerMock.initialized()).willReturn(Single.just(true))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error<Account>(IllegalStateException()))
        val observer = TestObserver.create<ViewAction>()

        viewModel.initialSetup().subscribe(observer)

        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().initialized()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertTerminated()
            .assertValueCount(1).assertValue { it is StartMain }
    }
}
