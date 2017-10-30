package pm.gnosis.heimdall.ui.multisig.details

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class MultisigDetailsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var qrCodeGeneratorMock: QrCodeGenerator

    private lateinit var viewModel: MultisigDetailsViewModel

    private var testAddress = BigInteger.ZERO
    private var invalidAddress = "1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".hexAsBigInteger()
    private var testName = "testName"

    @Before
    fun setup() {
        viewModel = MultisigDetailsViewModel(contextMock, qrCodeGeneratorMock)
    }

    @Test
    fun testSetup() {
        viewModel.setup(testAddress, testName)

        val wallet = MultisigWallet(testAddress, testName)
        assertEquals(wallet, viewModel.getMultisigWallet())
    }

    @Test(expected = InvalidAddressException::class)
    fun testSetupWithInvalidAddress() {
        viewModel.setup(invalidAddress, testName)
    }

    @Test
    fun loadQrCode() {
        val bitmapMock = mock(Bitmap::class.java)
        val contents = "contents"
        val testObserver = TestObserver.create<Result<Bitmap>>()
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt())).willReturn(Single.just(bitmapMock))

        viewModel.loadQrCode(contents).subscribe(testObserver)

        then(qrCodeGeneratorMock).should().generateQrCode(contents)
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(bitmapMock)).assertNoErrors()
    }

    @Test
    fun loadQrCodeError() {
        val exception = Exception()
        val contents = "contents"
        val testObserver = TestObserver.create<Result<Bitmap>>()
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt())).willReturn(Single.error(exception))

        viewModel.loadQrCode(contents).subscribe(testObserver)

        then(qrCodeGeneratorMock).should().generateQrCode(contents)
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception)).assertNoErrors()
    }
}
