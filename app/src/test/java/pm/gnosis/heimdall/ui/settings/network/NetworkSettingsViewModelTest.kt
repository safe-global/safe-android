package pm.gnosis.heimdall.ui.settings.network

import android.content.Context
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class NetworkSettingsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var repository: SettingsRepository

    lateinit var viewModel: NetworkSettingsViewModel

    @Before
    fun setUp() {
        context.mockGetString()
        viewModel = NetworkSettingsViewModel(context, repository)
    }

    @Test
    fun loadIpfsUrlNoOverride() {
        given(repository.getIpfsUrl()).willReturn(null)

        val testObserver = TestObserver<String>()
        viewModel.loadIpfsUrl().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue("").assertComplete()
        then(repository).should().getIpfsUrl()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadIpfsUrlOverride() {
        given(repository.getIpfsUrl()).willReturn(SettingsRepository.UrlOverride(true, "test.example", 1337))

        val testObserver = TestObserver<String>()
        viewModel.loadIpfsUrl().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue("https://test.example:1337").assertComplete()
        then(repository).should().getIpfsUrl()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateRpcUrlNoOverride() {
        given(repository.getEthereumRPCUrl()).willReturn(null)

        val testObserver = TestObserver<String>()
        viewModel.loadRpcUrl().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue("").assertComplete()
        then(repository).should().getEthereumRPCUrl()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateRpcUrlOverride() {
        given(repository.getEthereumRPCUrl()).willReturn(SettingsRepository.UrlOverride(true, "test.example", 1337))

        val testObserver = TestObserver<String>()
        viewModel.loadRpcUrl().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue("https://test.example:1337").assertComplete()
        then(repository).should().getEthereumRPCUrl()
        then(repository).shouldHaveNoMoreInteractions()
    }

    private fun testUpdateIpfsUrlValid(input: String, isHttps: Boolean, host: String?, port: Int?) {
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateIpfsUrl(input).subscribe(testObserver)

        then(repository).should().setIpfsUrl(isHttps, host, port)
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == input
        }.assertComplete()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateIpfsUrlValid() {
        // Https URL with port
        testUpdateIpfsUrlValid("https://test.example:1337", true, "test.example", 1337)
        // Http URL with port
        testUpdateIpfsUrlValid("http://test.example:1337", false, "test.example", 1337)
        // Https URL without port
        testUpdateIpfsUrlValid("https://test.example", true, "test.example", null)
        // Clear URL
        testUpdateIpfsUrlValid("", true, null, null)
    }

    private fun testUpdateIpfsUrlError(input: String, msgId: Int) {
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateIpfsUrl(input).subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.localizedMessage == msgId.toString()
        }.assertTerminated()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateIpfsUrlFails() {
        testUpdateIpfsUrlError("ftp://test.example:1337/", R.string.error_invalid_url_scheme)
        testUpdateIpfsUrlError("http://hdsgfd 1337 a", R.string.error_invalid_url)
        testUpdateIpfsUrlError("https://test.example:1337/a", R.string.error_invalid_url_path)
    }

    private fun testUpdateRpcUrlValid(input: String, isHttps: Boolean, host: String?, port: Int?) {
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateRpcUrl(input).subscribe(testObserver)

        then(repository).should().setEthereumRPCUrl(isHttps, host, port)
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == input
        }.assertComplete()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateRpcUrlValid() {
        // Https URL with port
        testUpdateRpcUrlValid("https://test.example:1337", true, "test.example", 1337)
        // Http URL with port
        testUpdateRpcUrlValid("http://test.example:1337", false, "test.example", 1337)
        // Https URL without port
        testUpdateRpcUrlValid("https://test.example", true, "test.example", null)
        // Clear URL
        testUpdateRpcUrlValid("", true, null, null)
    }

    private fun testUpdateRpcUrlError(input: String, msgId: Int) {
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateRpcUrl(input).subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.localizedMessage == msgId.toString()
        }.assertTerminated()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateRpcUrlFails() {
        testUpdateRpcUrlError("ftp://test.example:1337/", R.string.error_invalid_url_scheme)
        testUpdateRpcUrlError("http://hdsgfd 1337 a", R.string.error_invalid_url)
        testUpdateRpcUrlError("https://test.example:1337/a", R.string.error_invalid_url_path)
    }

    @Test
    fun loadProxyFactoryAddress() {
        given(repository.getProxyFactoryAddress()).willReturn(BigInteger.TEN)

        val testObserver = TestObserver<String>()
        viewModel.loadProxyFactoryAddress().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(BigInteger.TEN.asEthereumAddressString()).assertComplete()
        then(repository).should().getProxyFactoryAddress()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateProxyFactoryAddress() {
        val testAddress = "0x0000000000000000000000000000000000000000"
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateProxyFactoryAddress(testAddress).subscribe(testObserver)

        then(repository).should().setProxyFactoryAddress(testAddress)
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == testAddress
        }.assertComplete()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updatProxyFactoryAddressFails() {
        given(repository.setProxyFactoryAddress(anyString())).willThrow(InvalidAddressException())

        val testAddress = "0x0000000000000000000000000000000000000000"
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateProxyFactoryAddress(testAddress).subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.localizedMessage == R.string.invalid_ethereum_address.toString()
        }.assertTerminated()
        then(repository).should().setProxyFactoryAddress(testAddress)
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSafeMasterCopyAddress() {
        given(repository.getSafeMasterCopyAddress()).willReturn(BigInteger.ONE)

        val testObserver = TestObserver<String>()
        viewModel.loadSafeMasterCopyAddress().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(BigInteger.ONE.asEthereumAddressString()).assertComplete()
        then(repository).should().getSafeMasterCopyAddress()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateSafeMasterCopyAddress() {
        val testAddress = "0x0000000000000000000000000000000000000000"
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateSafeMasterCopyAddress(testAddress).subscribe(testObserver)

        then(repository).should().setSafeMasterCopyAddress(testAddress)
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == testAddress
        }.assertComplete()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateMasterCopyAddressFails() {
        given(repository.setSafeMasterCopyAddress(anyString())).willThrow(InvalidAddressException())

        val testAddress = "0x0000000000000000000000000000000000000000"
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateSafeMasterCopyAddress(testAddress).subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.localizedMessage == R.string.invalid_ethereum_address.toString()
        }.assertTerminated()
        then(repository).should().setSafeMasterCopyAddress(testAddress)
        then(repository).shouldHaveNoMoreInteractions()
    }
}
