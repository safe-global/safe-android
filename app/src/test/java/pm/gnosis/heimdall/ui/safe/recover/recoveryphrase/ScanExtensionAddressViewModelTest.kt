package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.remote.models.AddressSignedPayload
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress

@RunWith(MockitoJUnitRunner::class)
class ScanExtensionAddressViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var cryptoHelperMock: CryptoHelper

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var moshiMock: Moshi

    @Mock
    private lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var jsonAdapterMock: JsonAdapter<AddressSignedPayload>

    private lateinit var viewModel: ScanExtensionAddressViewModel

    @Before
    fun setUp() {
        contextMock.mockGetString()
        given(moshiMock.adapter<AddressSignedPayload>(MockUtils.any())).willReturn(jsonAdapterMock)
        viewModel = ScanExtensionAddressViewModel(cryptoHelperMock, contextMock, safeRepositoryMock, moshiMock)
        then(moshiMock).should().adapter<AddressSignedPayload>(AddressSignedPayload::class.java)
        viewModel.setup(SAFE_ADDRESS)
    }

    @Test
    fun getSafeAddress() {
        assertEquals(SAFE_ADDRESS, viewModel.getSafeAddress())
    }

    @Test
    fun validatePayload() {
        val testObserver = TestObserver.create<Result<Solidity.Address>>()

        val payload = "payload"
        val parsedPayload = AddressSignedPayload(
            address = SAFE_ADDRESS,
            signature = ServiceSignature(
                r = 10.toBigInteger(),
                s = 11.toBigInteger(),
                v = 27
            )
        )
        val expectedData = Sha3Utils.keccak("$SIGNATURE_PREFIX${SAFE_ADDRESS.asEthereumAddressChecksumString()}".toByteArray())

        given(jsonAdapterMock.fromJson(anyString())).willReturn(parsedPayload)
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(BROWSER_EXTENSION_ADDRESS)
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))

        viewModel.validatePayload(payload).subscribe(testObserver)

        testObserver.assertValue { it is DataResult && it.data == BROWSER_EXTENSION_ADDRESS }
        then(jsonAdapterMock).should().fromJson(payload)
        then(cryptoHelperMock).should().recover(expectedData, parsedPayload.signature.toSignature())
        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(jsonAdapterMock).shouldHaveNoMoreInteractions()
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun validatePayloadExtensionNotOwner() {
        val testObserver = TestObserver.create<Result<Solidity.Address>>()

        val payload = "payload"
        val parsedPayload = AddressSignedPayload(
            address = SAFE_ADDRESS,
            signature = ServiceSignature(
                r = 10.toBigInteger(),
                s = 11.toBigInteger(),
                v = 27
            )
        )
        val expectedData = Sha3Utils.keccak("$SIGNATURE_PREFIX${SAFE_ADDRESS.asEthereumAddressChecksumString()}".toByteArray())
        val extensionNotOwner = "0xfa".asEthereumAddress()!!

        given(jsonAdapterMock.fromJson(anyString())).willReturn(parsedPayload)
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(extensionNotOwner)
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))

        viewModel.validatePayload(payload).subscribe(testObserver)

        testObserver.assertValue(ErrorResult(SimpleLocalizedException(R.string.scan_extension_not_owner.toString())))
        then(jsonAdapterMock).should().fromJson(payload)
        then(cryptoHelperMock).should().recover(expectedData, parsedPayload.signature.toSignature())
        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(jsonAdapterMock).shouldHaveNoMoreInteractions()
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(contextMock).should().getString(R.string.scan_extension_not_owner)
        then(contextMock).shouldHaveNoMoreInteractions()
        then(moshiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun validatePayloadLoadInfoError() {
        val testObserver = TestObserver.create<Result<Solidity.Address>>()

        val payload = "payload"
        val parsedPayload = AddressSignedPayload(
            address = SAFE_ADDRESS,
            signature = ServiceSignature(
                r = 10.toBigInteger(),
                s = 11.toBigInteger(),
                v = 27
            )
        )
        val expectedData = Sha3Utils.keccak("$SIGNATURE_PREFIX${SAFE_ADDRESS.asEthereumAddressChecksumString()}".toByteArray())
        val exception = IllegalStateException()

        given(jsonAdapterMock.fromJson(anyString())).willReturn(parsedPayload)
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willReturn(BROWSER_EXTENSION_ADDRESS)
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.validatePayload(payload).subscribe(testObserver)

        testObserver.assertValue(ErrorResult(exception))
        then(jsonAdapterMock).should().fromJson(payload)
        then(cryptoHelperMock).should().recover(expectedData, parsedPayload.signature.toSignature())
        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(jsonAdapterMock).shouldHaveNoMoreInteractions()
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun validatePayloadRecoverError() {
        val testObserver = TestObserver.create<Result<Solidity.Address>>()

        val payload = "payload"
        val parsedPayload = AddressSignedPayload(
            address = SAFE_ADDRESS,
            signature = ServiceSignature(
                r = 10.toBigInteger(),
                s = 11.toBigInteger(),
                v = 27
            )
        )
        val expectedData = Sha3Utils.keccak("$SIGNATURE_PREFIX${SAFE_ADDRESS.asEthereumAddressChecksumString()}".toByteArray())
        val exception = IllegalStateException()

        given(jsonAdapterMock.fromJson(anyString())).willReturn(parsedPayload)
        given(cryptoHelperMock.recover(MockUtils.any(), MockUtils.any())).willThrow(exception)

        viewModel.validatePayload(payload).subscribe(testObserver)

        testObserver.assertValue(ErrorResult(exception))
        then(jsonAdapterMock).should().fromJson(payload)
        then(cryptoHelperMock).should().recover(expectedData, parsedPayload.signature.toSignature())
        then(jsonAdapterMock).shouldHaveNoMoreInteractions()
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(contextMock).shouldHaveZeroInteractions()
        then(moshiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun validatePayloadDifferentSafeAddress() {
        val testObserver = TestObserver.create<Result<Solidity.Address>>()

        val payload = "payload"
        val parsedPayload = AddressSignedPayload(
            address = "0xfa".asEthereumAddress()!!,
            signature = ServiceSignature(
                r = 10.toBigInteger(),
                s = 11.toBigInteger(),
                v = 27
            )
        )
        given(jsonAdapterMock.fromJson(anyString())).willReturn(parsedPayload)

        viewModel.validatePayload(payload).subscribe(testObserver)

        testObserver.assertValue(ErrorResult(SimpleLocalizedException(R.string.scan_extension_invalid_signature.toString())))
        then(jsonAdapterMock).should().fromJson(payload)
        then(jsonAdapterMock).shouldHaveNoMoreInteractions()
        then(cryptoHelperMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(contextMock).should().getString(R.string.scan_extension_invalid_signature)
        then(contextMock).shouldHaveNoMoreInteractions()
        then(moshiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun validatePayloadInvalidPayloadFormat() {
        val testObserver = TestObserver.create<Result<Solidity.Address>>()

        val payload = "payload"
        given(jsonAdapterMock.fromJson(anyString())).willReturn(null)

        viewModel.validatePayload(payload).subscribe(testObserver)

        testObserver.assertValue(ErrorResult(SimpleLocalizedException(R.string.scan_extension_invalid_format.toString())))
        then(jsonAdapterMock).should().fromJson(payload)
        then(jsonAdapterMock).shouldHaveNoMoreInteractions()
        then(cryptoHelperMock).shouldHaveZeroInteractions()
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(contextMock).should().getString(R.string.scan_extension_invalid_format)
        then(contextMock).shouldHaveNoMoreInteractions()
        then(moshiMock).shouldHaveNoMoreInteractions()
    }

    companion object {
        private const val SIGNATURE_PREFIX = "GNO"
        private val SAFE_ADDRESS = "0xf".asEthereumAddress()!!
        private val PHONE_ADDRESS = "0x41".asEthereumAddress()!!
        private val BROWSER_EXTENSION_ADDRESS = "0x42".asEthereumAddress()!!
        private val RECOVERY_ADDRESS_0 = "0x43".asEthereumAddress()!!
        private val RECOVERY_ADDRESS_1 = "0x44".asEthereumAddress()!!

        private val SAFE_INFO = SafeInfo(
            address = SAFE_ADDRESS,
            balance = Wei.ZERO,
            requiredConfirmations = 2L,
            owners = listOf(PHONE_ADDRESS, BROWSER_EXTENSION_ADDRESS, RECOVERY_ADDRESS_0, RECOVERY_ADDRESS_1),
            isOwner = true,
            modules = emptyList(),
            version = SemVer(1, 0, 0)
        )
    }
}
