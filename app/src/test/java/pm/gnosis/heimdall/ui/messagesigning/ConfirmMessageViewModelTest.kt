package pm.gnosis.heimdall.ui.messagesigning

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.eip712.*
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexStringToByteArray
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class ConfirmMessageViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var cryptoHelperMock: CryptoHelper

    @Mock
    private lateinit var eiP712JsonParserMock: EIP712JsonParser

    @Mock
    private lateinit var safeRepoMock: GnosisSafeRepository

    @Mock
    private lateinit var pushServiceRepositoryMock: PushServiceRepository

    private lateinit var viewModel: ConfirmMessageViewModel

    @Before
    fun setUp() {
        viewModel = ConfirmMessageViewModel(cryptoHelperMock, eiP712JsonParserMock, safeRepoMock, pushServiceRepositoryMock)
    }

    @Test
    fun defaultViewUpdateAfterSetup() {
        val testObserver = TestObserver.create<ConfirmMessageContract.ViewUpdate>()

        viewModel.setup(TEST_PAYLOAD, SAFE_ADDRESS, TEST_SIGNATURE)
        viewModel.observe().subscribe(testObserver)

        testObserver
            .assertValues(
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    error = null,
                    isLoading = false,
                    finishProcess = false
                )
            )
            .assertNoErrors()
    }

    @Test
    fun clickingConfirmSendsSignature() {
        val testObserver = TestObserver.create<ConfirmMessageContract.ViewUpdate>()

        given(eiP712JsonParserMock.parseMessage(anyString())).willReturn(PARSING_RESULT)
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(REQUESTER)
        given(safeRepoMock.sign(MockUtils.any(), MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE))
        given(
            pushServiceRepositoryMock.sendTypedDataConfirmation(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Completable.complete())

        viewModel.setup(TEST_PAYLOAD, SAFE_ADDRESS, TEST_SIGNATURE)
        viewModel.observe().subscribe(testObserver)

        // Click Confirm
        viewModel.uiEvents.onNext(ConfirmMessageContract.UIEvent.ConfirmPayloadClick)

        then(cryptoHelperMock).should().recover(SAFE_MESSAGE_HASH, TEST_SIGNATURE)
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()

        then(safeRepoMock).should().sign(SAFE_ADDRESS, SAFE_MESSAGE_HASH)
        then(safeRepoMock).shouldHaveNoMoreInteractions()

        then(pushServiceRepositoryMock).should()
            .sendTypedDataConfirmation(SAFE_MESSAGE_HASH, TEST_SIGNATURE.toString().hexStringToByteArray(), SAFE_ADDRESS, setOf(REQUESTER))
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        then(eiP712JsonParserMock).should().parseMessage(TEST_PAYLOAD)
        then(eiP712JsonParserMock).shouldHaveNoMoreInteractions()

        testObserver
            .assertValues(
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = true,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = null,
                    finishProcess = true
                )
            )
            .assertNoErrors()
    }

    @Test
    fun clickingConfirmErrorSendingPush() {
        val testObserver = TestObserver.create<ConfirmMessageContract.ViewUpdate>()
        val exception = IllegalArgumentException()

        given(eiP712JsonParserMock.parseMessage(anyString())).willReturn(PARSING_RESULT)
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(REQUESTER)
        given(safeRepoMock.sign(MockUtils.any(), MockUtils.any())).willReturn(Single.just(TEST_SIGNATURE))
        given(
            pushServiceRepositoryMock.sendTypedDataConfirmation(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Completable.error(exception))

        viewModel.setup(TEST_PAYLOAD, SAFE_ADDRESS, TEST_SIGNATURE)
        viewModel.observe().subscribe(testObserver)

        // Click Confirm
        viewModel.uiEvents.onNext(ConfirmMessageContract.UIEvent.ConfirmPayloadClick)

        then(cryptoHelperMock).should().recover(SAFE_MESSAGE_HASH, TEST_SIGNATURE)
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()

        then(safeRepoMock).should().sign(SAFE_ADDRESS, SAFE_MESSAGE_HASH)
        then(safeRepoMock).shouldHaveNoMoreInteractions()

        then(pushServiceRepositoryMock).should()
            .sendTypedDataConfirmation(SAFE_MESSAGE_HASH, TEST_SIGNATURE.toString().hexStringToByteArray(), SAFE_ADDRESS, setOf(REQUESTER))
        then(pushServiceRepositoryMock).shouldHaveNoMoreInteractions()

        then(eiP712JsonParserMock).should().parseMessage(TEST_PAYLOAD)
        then(eiP712JsonParserMock).shouldHaveNoMoreInteractions()

        testObserver
            .assertValues(
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = true,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = ConfirmMessageContract.ErrorSendingPush,
                    finishProcess = false
                )
            )
            .assertNoErrors()
    }

    @Test
    fun clickingConfirmErrorSigningPayload() {
        val testObserver = TestObserver.create<ConfirmMessageContract.ViewUpdate>()
        val exception = IllegalArgumentException()

        given(eiP712JsonParserMock.parseMessage(anyString())).willReturn(PARSING_RESULT)
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(REQUESTER)
        given(safeRepoMock.sign(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        viewModel.setup(TEST_PAYLOAD, SAFE_ADDRESS, TEST_SIGNATURE)
        viewModel.observe().subscribe(testObserver)

        // Click Confirm
        viewModel.uiEvents.onNext(ConfirmMessageContract.UIEvent.ConfirmPayloadClick)

        then(eiP712JsonParserMock).should().parseMessage(TEST_PAYLOAD)
        then(eiP712JsonParserMock).shouldHaveNoMoreInteractions()

        then(cryptoHelperMock).should().recover(SAFE_MESSAGE_HASH, TEST_SIGNATURE)
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()

        then(safeRepoMock).should().sign(SAFE_ADDRESS, SAFE_MESSAGE_HASH)
        then(safeRepoMock).shouldHaveNoMoreInteractions()

        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver
            .assertValues(
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = true,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = ConfirmMessageContract.ErrorSigningHash,
                    finishProcess = false
                )
            )
            .assertNoErrors()
    }

    @Test
    fun clickingConfirmErrorRecoveringAccount() {
        val testObserver = TestObserver.create<ConfirmMessageContract.ViewUpdate>()
        val exception = IllegalArgumentException()

        given(eiP712JsonParserMock.parseMessage(anyString())).willReturn(PARSING_RESULT)
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willThrow(exception)

        viewModel.setup(TEST_PAYLOAD, SAFE_ADDRESS, TEST_SIGNATURE)
        viewModel.observe().subscribe(testObserver)

        // Click Confirm
        viewModel.uiEvents.onNext(ConfirmMessageContract.UIEvent.ConfirmPayloadClick)

        then(eiP712JsonParserMock).should().parseMessage(TEST_PAYLOAD)
        then(eiP712JsonParserMock).shouldHaveNoMoreInteractions()

        then(cryptoHelperMock).should().recover(SAFE_MESSAGE_HASH, TEST_SIGNATURE)
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()

        then(safeRepoMock).shouldHaveZeroInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver
            .assertValues(
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = true,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = ConfirmMessageContract.ErrorRecoveringSender,
                    finishProcess = false
                )
            )
            .assertNoErrors()
    }

    @Test
    fun clickingConfirmInvalidPayload() {
        val testObserver = TestObserver.create<ConfirmMessageContract.ViewUpdate>()
        val exception = IllegalArgumentException()

        given(eiP712JsonParserMock.parseMessage(anyString())).willThrow(exception)

        viewModel.setup(TEST_PAYLOAD, SAFE_ADDRESS, TEST_SIGNATURE)
        viewModel.observe().subscribe(testObserver)

        // Click Confirm
        viewModel.uiEvents.onNext(ConfirmMessageContract.UIEvent.ConfirmPayloadClick)

        then(eiP712JsonParserMock).should().parseMessage(TEST_PAYLOAD)
        then(eiP712JsonParserMock).shouldHaveNoMoreInteractions()
        then(cryptoHelperMock).shouldHaveZeroInteractions()
        then(safeRepoMock).shouldHaveZeroInteractions()
        then(pushServiceRepositoryMock).shouldHaveZeroInteractions()

        testObserver
            .assertValues(
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = true,
                    error = null,
                    finishProcess = false
                ),
                ConfirmMessageContract.ViewUpdate(
                    payload = TEST_PAYLOAD,
                    isLoading = false,
                    error = ConfirmMessageContract.InvalidPayload,
                    finishProcess = false
                )
            )
            .assertNoErrors()
    }

    companion object {
        private const val TEST_PAYLOAD = "EIP712Payload"
        private val SAFE_ADDRESS = Solidity.Address(BigInteger.TEN)
        private val TEST_SIGNATURE = Signature(
            r = "0x0".hexAsBigInteger(),
            s = "0x1".hexAsBigInteger(),
            v = 28.toByte()
        )
        private val REQUESTER = Solidity.Address(BigInteger.ONE)

        private val PARSING_RESULT = DomainWithMessage(
            domain = Struct712(
                typeName = "TestDomain", parameters = listOf(
                    Struct712Parameter(
                        name = "address",
                        type = Literal712(
                            typeName = "address",
                            value = Solidity.Address(BigInteger.ONE)
                        )
                    )
                )
            ),
            message = Struct712(
                typeName = "TestMessage", parameters = listOf(
                    Struct712Parameter(
                        name = "address",
                        type = Literal712(
                            typeName = "address",
                            value = Solidity.Address(BigInteger.ONE)
                        )
                    )
                )
            )
        )

        private val SAFE_MESSAGE_HASH = "0b6d5337d2432df1df8e782f71a727ea3f11ade382b030d16133f7559883ec8d".hexStringToByteArray()
    }
}
