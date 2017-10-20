package pm.gnosis.heimdall.ui.splash

import android.arch.persistence.room.EmptyResultSetException
import io.reactivex.Completable
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
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SplashViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var accountsRepository: AccountsRepository

    @Mock
    lateinit var tokenRepository: TokenRepository

    lateinit var viewModel: SplashViewModel

    @Before
    fun setup() {
        viewModel = SplashViewModel(accountsRepository, tokenRepository)
    }

    @Test
    fun initialSetupTokenSetupErrorWithAccount() {
        given(tokenRepository.setup()).willReturn(Completable.error(IllegalStateException()))
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(Account(BigInteger.ONE)))
        val observer = TestObserver.create<ViewAction>()

        viewModel.initialSetup().subscribe(observer)

        then(tokenRepository).should().setup()
        then(accountsRepository).should().loadActiveAccount()
        observer.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupErrorNoAccountNoSuchElement() {
        val observerNoSuchElement = TestObserver.create<ViewAction>()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(NoSuchElementException()))
        given(tokenRepository.setup()).willReturn(Completable.error(IllegalStateException()))

        viewModel.initialSetup().subscribe(observerNoSuchElement)

        then(accountsRepository).should().loadActiveAccount()
        observerNoSuchElement.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartSetup }
    }

    @Test
    fun initialSetupTokenSetupErrorNoAccountEmptyResultSet() {
        val observerEmptyResult = TestObserver.create<ViewAction>()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(EmptyResultSetException("")))
        given(tokenRepository.setup()).willReturn(Completable.error(IllegalStateException()))

        viewModel.initialSetup().subscribe(observerEmptyResult)

        then(accountsRepository).should().loadActiveAccount()
        observerEmptyResult.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartSetup }
    }

    @Test
    fun initialSetupTokenSetupErrorAccountError() {
        given(tokenRepository.setup()).willReturn(Completable.error(IllegalStateException()))
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(IllegalStateException()))
        val observer = TestObserver.create<ViewAction>()

        viewModel.initialSetup().subscribe(observer)

        then(tokenRepository).should().setup()
        then(accountsRepository).should().loadActiveAccount()
        observer.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupCompletedWithAccount() {
        given(tokenRepository.setup()).willReturn(Completable.complete())
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(Account(BigInteger.ONE)))
        val observer = TestObserver.create<ViewAction>()

        viewModel.initialSetup().subscribe(observer)

        then(tokenRepository).should().setup()
        then(accountsRepository).should().loadActiveAccount()
        observer.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupCompletedNoAccountNoSuchElement() {
        val observerNoSuchElement = TestObserver.create<ViewAction>()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(NoSuchElementException()))
        given(tokenRepository.setup()).willReturn(Completable.complete())

        viewModel.initialSetup().subscribe(observerNoSuchElement)

        then(accountsRepository).should().loadActiveAccount()
        then(tokenRepository).should().setup()
        observerNoSuchElement.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartSetup }
    }

    @Test
    fun initialSetupTokenSetupCompletedNoAccountEmptyResultSet() {
        val observerEmptyResult = TestObserver.create<ViewAction>()
        given(tokenRepository.setup()).willReturn(Completable.complete())
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(EmptyResultSetException("")))

        viewModel.initialSetup().subscribe(observerEmptyResult)

        then(accountsRepository).should().loadActiveAccount()
        observerEmptyResult.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartSetup }
    }

    @Test
    fun initialSetupTokenSetupCompletedAccountError() {
        given(tokenRepository.setup()).willReturn(Completable.complete())
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(IllegalStateException()))
        val observer = TestObserver.create<ViewAction>()

        viewModel.initialSetup().subscribe(observer)

        then(tokenRepository).should().setup()
        then(accountsRepository).should().loadActiveAccount()
        observer.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartMain }
    }
}
