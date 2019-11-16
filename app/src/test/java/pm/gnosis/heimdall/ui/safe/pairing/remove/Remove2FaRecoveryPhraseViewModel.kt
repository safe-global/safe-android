package pm.gnosis.heimdall.ui.safe.pairing.remove

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.asOwner

@RunWith(MockitoJUnitRunner::class)
class Remove2FaRecoveryPhraseViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var recoverSafeOwnersHelperMock: RecoverSafeOwnersHelper

    private lateinit var viewModel: Remove2FaRecoveryPhraseViewModel

    @Before
    fun setup() {
        viewModel = Remove2FaRecoveryPhraseViewModel(recoverSafeOwnersHelperMock)
    }

    @Test
    fun process() {
        val testObserver = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        val input = InputRecoveryPhraseContract.Input(
            phrase = Observable.just(""),
            retry = Observable.just(Unit),
            create = Observable.just(Unit)
        )
        val safeOwner = 34.toBigInteger().let { Solidity.Address(it) }.asOwner()
        val safeAddress = 35.toBigInteger().let { Solidity.Address(it) }
        val extensionAddress = 36.toBigInteger().let { Solidity.Address(it) }
        val authenticatorInfo = AuthenticatorSetupInfo(
            safeOwner,
            AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, extensionAddress)
        )
        val viewUpdate = InputRecoveryPhraseContract.ViewUpdate.ValidMnemonic

        BDDMockito.given(
            recoverSafeOwnersHelperMock.process(
                MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()
            )
        ).willReturn(Observable.just(viewUpdate))

        viewModel.process(input, safeAddress, authenticatorInfo).subscribe(testObserver)

        BDDMockito.then(recoverSafeOwnersHelperMock).should().process(input, safeAddress, null, safeOwner)
        BDDMockito.then(recoverSafeOwnersHelperMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(viewUpdate)
    }

    @Test
    fun processNoAuthenticatorInfo() {
        val testObserver = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        val input = InputRecoveryPhraseContract.Input(
            phrase = Observable.just(""),
            retry = Observable.just(Unit),
            create = Observable.just(Unit)
        )
        val safeAddress = 35.toBigInteger().let { Solidity.Address(it) }

        viewModel.process(input, safeAddress, null).subscribe(testObserver)

        BDDMockito.then(recoverSafeOwnersHelperMock).shouldHaveNoMoreInteractions()
        testObserver
            .assertValueCount(1)
            .assertValueAt(0) {
                it is InputRecoveryPhraseContract.ViewUpdate.RecoverDataError &&
                        it.error is IllegalStateException
            }
    }

    @Test
    fun processError() {
        val testObserver = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        val input = InputRecoveryPhraseContract.Input(
            phrase = Observable.just(""),
            retry = Observable.just(Unit),
            create = Observable.just(Unit)
        )
        val safeOwner = 34.toBigInteger().let { Solidity.Address(it) }.asOwner()
        val safeAddress = 35.toBigInteger().let { Solidity.Address(it) }
        val extensionAddress = 36.toBigInteger().let { Solidity.Address(it) }
        val authenticatorInfo = AuthenticatorSetupInfo(
            safeOwner,
            AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, extensionAddress)
        )
        val exception = Exception()

        BDDMockito.given(
            recoverSafeOwnersHelperMock.process(
                MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()
            )
        ).willReturn(Observable.error(exception))

        viewModel.process(input, safeAddress, authenticatorInfo).subscribe(testObserver)

        BDDMockito.then(recoverSafeOwnersHelperMock).should().process(input, safeAddress, null, safeOwner)
        BDDMockito.then(recoverSafeOwnersHelperMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(Exception::class.java)
    }
}
