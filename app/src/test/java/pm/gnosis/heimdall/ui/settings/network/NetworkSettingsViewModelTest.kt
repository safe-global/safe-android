package pm.gnosis.heimdall.ui.settings.network

import android.content.Context
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class NetworkSettingsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var repository: SettingsRepository

    lateinit var viewModel: NetworkSettingsViewModel

    @Before
    fun setUp() {
        context.mockGetString()
        viewModel = NetworkSettingsViewModel(context, repository)
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
        given(repository.getProxyFactoryAddress()).willReturn(Solidity.Address(BigInteger.TEN))

        val testObserver = TestObserver<String>()
        viewModel.loadProxyFactoryAddress().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(Solidity.Address(BigInteger.TEN).asEthereumAddressString()).assertComplete()
        then(repository).should().getProxyFactoryAddress()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateProxyFactoryAddress() {
        val testAddress = "0x0000000000000000000000000000000000000000"
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateProxyFactoryAddress(testAddress).subscribe(testObserver)

        then(repository).should().setProxyFactoryAddress(testAddress.asEthereumAddress())
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == testAddress
        }.assertComplete()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSafeMasterCopyAddress() {
        given(repository.getSafeMasterCopyAddress()).willReturn(Solidity.Address(BigInteger.ONE))

        val testObserver = TestObserver<String>()
        viewModel.loadSafeMasterCopyAddress().subscribe(testObserver)

        testObserver.assertNoErrors().assertValue(Solidity.Address(BigInteger.ONE).asEthereumAddressString()).assertComplete()
        then(repository).should().getSafeMasterCopyAddress()
        then(repository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun updateSafeMasterCopyAddress() {
        val testAddress = "0x0000000000000000000000000000000000000000"
        val testObserver = TestObserver<Result<String>>()
        viewModel.updateSafeMasterCopyAddress(testAddress).subscribe(testObserver)

        then(repository).should().setSafeMasterCopyAddress(testAddress.asEthereumAddress())
        testObserver.assertNoErrors().assertValue {
            it is DataResult && it.data == testAddress
        }.assertComplete()
        then(repository).shouldHaveNoMoreInteractions()
    }
}
