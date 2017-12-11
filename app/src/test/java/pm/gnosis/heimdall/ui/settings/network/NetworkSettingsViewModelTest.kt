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
import pm.gnosis.utils.exceptions.InvalidAddressException

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
        verify(repository).getIpfsUrl()
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun loadIpfsUrlOverride() {
        given(repository.getIpfsUrl()).willReturn(SettingsRepository.UrlOverride(true, "test.example", 1337))

        val testObserver = TestObserver<String>()
        viewModel.loadIpfsUrl().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue("https://test.example:1337").assertComplete()
        verify(repository).getIpfsUrl()
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun updateRpcUrlNoOverride() {
        given(repository.getEthereumRPCUrl()).willReturn(null)

        val testObserver = TestObserver<String>()
        viewModel.loadRpcUrl().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue("").assertComplete()
        verify(repository).getEthereumRPCUrl()
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun updateRpcUrlOverride() {
        given(repository.getEthereumRPCUrl()).willReturn(SettingsRepository.UrlOverride(true, "test.example", 1337))

        val testObserver = TestObserver<String>()
        viewModel.loadRpcUrl().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue("https://test.example:1337").assertComplete()
        verify(repository).getEthereumRPCUrl()
        verifyNoMoreInteractions(repository)
    }

    private fun testUpdateIpfsUrlValid(input: String, isHttps: Boolean, host: String?, port: Int?) {
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateIpfsUrl(input).subscribe(testObserver)

        verify(repository).setIpfsUrl(isHttps, host, port)
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == input
        }.assertComplete()
        verifyNoMoreInteractions(repository)
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
        verifyNoMoreInteractions(repository)
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

        verify(repository).setEthereumRPCUrl(isHttps, host, port)
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == input
        }.assertComplete()
        verifyNoMoreInteractions(repository)
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
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun updateRpcUrlFails() {
        testUpdateRpcUrlError("ftp://test.example:1337/", R.string.error_invalid_url_scheme)
        testUpdateRpcUrlError("http://hdsgfd 1337 a", R.string.error_invalid_url)
        testUpdateRpcUrlError("https://test.example:1337/a", R.string.error_invalid_url_path)
    }

    @Test
    fun updateSafeAddress() {
        val testAddress = "0x0000000000000000000000000000000000000000"
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateSafeFactoryAddress(testAddress).subscribe(testObserver)

        verify(repository).setSafeFactoryAddress(testAddress)
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == testAddress
        }.assertComplete()
    }

    @Test
    fun updateSafeAddressFails() {
        given(repository.setSafeFactoryAddress(anyString())).willThrow(InvalidAddressException())

        val testAddress = "0x0000000000000000000000000000000000000000"
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateSafeFactoryAddress(testAddress).subscribe(testObserver)

        testObserver.assertNoErrors().assertValue {
            it is ErrorResult && it.error is SimpleLocalizedException && it.error.localizedMessage == R.string.invalid_ethereum_address.toString()
        }.assertTerminated()
        verify(repository).setSafeFactoryAddress(testAddress)
        verifyNoMoreInteractions(repository)
    }
}
