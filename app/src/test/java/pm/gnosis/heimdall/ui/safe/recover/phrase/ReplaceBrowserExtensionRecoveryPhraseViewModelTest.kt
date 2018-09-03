package pm.gnosis.heimdall.ui.safe.recover.phrase

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.model.Solidity
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils

@RunWith(MockitoJUnitRunner::class)
class ReplaceBrowserExtensionRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var recoverSafeOwnersHelperMock: RecoverSafeOwnersHelper

    private lateinit var viewModel: ReplaceBrowserExtensionRecoveryPhraseViewModel

    @Before
    fun setup() {
        viewModel = ReplaceBrowserExtensionRecoveryPhraseViewModel(recoverSafeOwnersHelperMock)
    }

    @Test
    fun process() {
        val testObserver = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        val input = InputRecoveryPhraseContract.Input(
            phrase = Observable.just(""),
            retry = Observable.just(Unit),
            create = Observable.just(Unit)
        )
        val safeAddress = 35.toBigInteger().let { Solidity.Address(it) }
        val extensionAddress = 36.toBigInteger().let { Solidity.Address(it) }
        val viewUpdate = InputRecoveryPhraseContract.ViewUpdate.ValidMnemonic

        given(
            recoverSafeOwnersHelperMock.process(
                MockUtils.any(), MockUtils.any(), MockUtils.any()
            )
        ).willReturn(Observable.just(viewUpdate))

        viewModel.process(input, safeAddress, extensionAddress).subscribe(testObserver)

        then(recoverSafeOwnersHelperMock).should().process(input, safeAddress, extensionAddress)
        then(recoverSafeOwnersHelperMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(viewUpdate)
    }

    @Test
    fun processError() {
        val testObserver = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        val input = InputRecoveryPhraseContract.Input(
            phrase = Observable.just(""),
            retry = Observable.just(Unit),
            create = Observable.just(Unit)
        )
        val safeAddress = 35.toBigInteger().let { Solidity.Address(it) }
        val extensionAddress = 36.toBigInteger().let { Solidity.Address(it) }
        val exception = Exception()

        given(
            recoverSafeOwnersHelperMock.process(
                MockUtils.any(), MockUtils.any(), MockUtils.any()
            )
        ).willReturn(Observable.error(exception))

        viewModel.process(input, safeAddress, extensionAddress).subscribe(testObserver)

        then(recoverSafeOwnersHelperMock).should().process(input, safeAddress, extensionAddress)
        then(recoverSafeOwnersHelperMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(Exception::class.java)
    }
}
