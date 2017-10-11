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
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule

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
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(Account("0f")))
        val observer = TestObserver.create<ViewAction>()
        viewModel.initialSetup().subscribe(observer)
        observer.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupErrorNoAccount() {
        given(tokenRepository.setup()).willReturn(Completable.error(IllegalStateException()))

        val observerNoSuchElement = TestObserver.create<ViewAction>()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(NoSuchElementException()))
        viewModel.initialSetup().subscribe(observerNoSuchElement)
        observerNoSuchElement.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartSetup }

        val observerEmptyResult = TestObserver.create<ViewAction>()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(EmptyResultSetException("")))
        viewModel.initialSetup().subscribe(observerEmptyResult)
        observerEmptyResult.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartSetup }
    }

    @Test
    fun initialSetupTokenSetupErrorAccountError() {
        given(tokenRepository.setup()).willReturn(Completable.error(IllegalStateException()))
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(IllegalStateException()))

        val observer = TestObserver.create<ViewAction>()
        viewModel.initialSetup().subscribe(observer)
        observer.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupCompletedWithAccount() {
        given(tokenRepository.setup()).willReturn(Completable.complete())
        given(accountsRepository.loadActiveAccount()).willReturn(Single.just(Account("0f")))
        val observer = TestObserver.create<ViewAction>()
        viewModel.initialSetup().subscribe(observer)
        observer.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartMain }
    }

    @Test
    fun initialSetupTokenSetupCompletedNoAccount() {
        given(tokenRepository.setup()).willReturn(Completable.complete())

        val observerNoSuchElement = TestObserver.create<ViewAction>()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(NoSuchElementException()))
        viewModel.initialSetup().subscribe(observerNoSuchElement)
        observerNoSuchElement.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartSetup }

        val observerEmptyResult = TestObserver.create<ViewAction>()
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(EmptyResultSetException("")))
        viewModel.initialSetup().subscribe(observerEmptyResult)
        observerEmptyResult.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartSetup }
    }

    @Test
    fun initialSetupTokenSetupCompletedAccountError() {
        given(tokenRepository.setup()).willReturn(Completable.complete())
        given(accountsRepository.loadActiveAccount()).willReturn(Single.error<Account>(IllegalStateException()))

        val observer = TestObserver.create<ViewAction>()
        viewModel.initialSetup().subscribe(observer)
        observer.assertNoErrors().assertTerminated()
                .assertValueCount(1).assertValue { it is StartMain }
    }

}
