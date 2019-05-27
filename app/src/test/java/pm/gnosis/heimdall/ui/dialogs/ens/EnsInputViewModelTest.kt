package pm.gnosis.heimdall.ui.dialogs.ens

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.EnsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.lang.IllegalStateException

@RunWith(MockitoJUnitRunner::class)
class EnsInputViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var ensRepositoryMock: EnsRepository

    private lateinit var viewModel: EnsInputViewModel

    @Before
    fun setUp() {
        viewModel = EnsInputViewModel(ensRepositoryMock)
    }

    @Test
    fun processEnsInput() {
        val address = "safe.gnosis.xyz"
        val result = "0xbaddad".asEthereumAddress()!!
        given(ensRepositoryMock.resolve(MockUtils.any())).willReturn(Single.just(result))
        val ensInput = PublishSubject.create<CharSequence>()
        val testObserver = TestObserver<Result<Solidity.Address>>()

        ensInput.compose(viewModel.processEnsInput()).subscribe(testObserver)
        testObserver.assertEmpty()
        then(ensRepositoryMock).shouldHaveZeroInteractions()

        ensInput.onNext(address)
        testObserver
            .assertValues(DataResult(result))
            .assertSubscribed()
            .assertNotComplete()
            .assertNoErrors()
        then(ensRepositoryMock).should().resolve(address)
        then(ensRepositoryMock).shouldHaveNoMoreInteractions()

    }

    @Test
    fun processEnsInputFail() {
        val error = IllegalStateException()
        given(ensRepositoryMock.resolve(MockUtils.any())).willReturn(Single.error(error))
        val ensInput = PublishSubject.create<CharSequence>()
        val testObserver = TestObserver<Result<Solidity.Address>>()

        ensInput.compose(viewModel.processEnsInput()).subscribe(testObserver)
        testObserver.assertEmpty()
        then(ensRepositoryMock).shouldHaveZeroInteractions()

        ensInput.onNext("test")
        testObserver
            .assertValues(ErrorResult(error))
            .assertSubscribed()
            .assertNotComplete()
            .assertNoErrors()
        then(ensRepositoryMock).should().resolve("test")
        then(ensRepositoryMock).shouldHaveNoMoreInteractions()

    }
}
