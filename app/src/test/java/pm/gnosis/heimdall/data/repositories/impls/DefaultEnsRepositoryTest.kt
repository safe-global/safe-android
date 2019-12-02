package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthRequest
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.lang.IllegalArgumentException
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class DefaultEnsRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var repository: DefaultEnsRepository

    @Mock
    private lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    private lateinit var normalizerMock: EnsNormalizer


    @Before
    fun setUp() {
        repository = DefaultEnsRepository(normalizerMock, ethereumRepositoryMock)
    }

    @Test
    fun processNormalizeFail() {
        given(normalizerMock.normalize(MockUtils.any())).willThrow(IllegalArgumentException())
        val address = ""
        val testObserver = TestObserver<Solidity.Address>()
        repository.resolve(address).subscribe(testObserver)

        testObserver.assertFailure(IllegalArgumentException::class.java)

        then(ethereumRepositoryMock).shouldHaveZeroInteractions()
        then(normalizerMock).should().normalize(address)
        then(normalizerMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun processGetResolverFail() {
        given(normalizerMock.normalize(MockUtils.any())).willAnswer { it.arguments.first() as String }
        given(ethereumRepositoryMock.request(MockUtils.any<EthRequest<*>>())).willReturn(Observable.error(UnknownHostException()))
        val address = ""
        val testObserver = TestObserver<Solidity.Address>()
        repository.resolve(address).subscribe(testObserver)

        testObserver.assertFailure(UnknownHostException::class.java)

        then(ethereumRepositoryMock).should().request(
            ArgumentMatchers.argThat(
                CallMatcher(
                    EthCall(
                        transaction = Transaction(
                            ENS_ADDRESS,
                            data = GET_RESOLVER + "0000000000000000000000000000000000000000000000000000000000000000"
                        )
                    )
                )
            )
        )
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
        then(normalizerMock).should().normalize(address)
        then(normalizerMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun processGetAddressFail() {
        given(normalizerMock.normalize(MockUtils.any())).willAnswer { it.arguments.first() as String }
        given(ethereumRepositoryMock.request(MockUtils.any<EthRequest<*>>())).will {
            val request = it.arguments.first() as EthCall
            if (request.transaction?.address == ENS_ADDRESS) {
                request.response = EthRequest.Response.Success(TEST_ADDRESS.asEthereumAddressString())
                Observable.just(request)
            } else
                Observable.error(UnknownHostException())
        }
        val address = "eth"
        val testObserver = TestObserver<Solidity.Address>()
        repository.resolve(address).subscribe(testObserver)

        testObserver.assertFailure(UnknownHostException::class.java)

        then(ethereumRepositoryMock).should().request(
            ArgumentMatchers.argThat(
                CallMatcher(
                    EthCall(
                        transaction = Transaction(
                            ENS_ADDRESS,
                            data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                        )
                    )
                )
            )
        )
        then(ethereumRepositoryMock).should().request(
            ArgumentMatchers.argThat(
                CallMatcher(
                    EthCall(
                        transaction = Transaction(
                            TEST_ADDRESS,
                            data = GET_ADDRESS + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                        )
                    )
                )
            )
        )
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
        then(normalizerMock).should().normalize(address)
        then(normalizerMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun process() {
        given(normalizerMock.normalize(MockUtils.any())).willAnswer { it.arguments.first() as String }
        given(ethereumRepositoryMock.request(MockUtils.any<EthRequest<*>>())).will {
            val request = it.arguments.first() as EthCall
            request.response = if (request.transaction?.address == ENS_ADDRESS)
                EthRequest.Response.Success(TEST_ADDRESS.asEthereumAddressString())
            else
                EthRequest.Response.Success(TEST_SAFE.asEthereumAddressString())
            Observable.just(request)
        }
        val address = "eth"
        val testObserver = TestObserver<Solidity.Address>()
        repository.resolve(address).subscribe(testObserver)

        testObserver.assertResult(TEST_SAFE)

        then(ethereumRepositoryMock).should().request(
            ArgumentMatchers.argThat(
                CallMatcher(
                    EthCall(
                        transaction = Transaction(
                            ENS_ADDRESS,
                            data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                        )
                    )
                )
            )
        )
        then(ethereumRepositoryMock).should().request(
            ArgumentMatchers.argThat(
                CallMatcher(
                    EthCall(
                        transaction = Transaction(
                            TEST_ADDRESS,
                            data = GET_ADDRESS + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                        )
                    )
                )
            )
        )
        then(ethereumRepositoryMock).shouldHaveNoMoreInteractions()
        then(normalizerMock).should().normalize(address)
        then(normalizerMock).shouldHaveNoMoreInteractions()
    }

    private class CallMatcher(private val expected: EthCall) : ArgumentMatcher<EthCall> {


        override fun matches(argument: EthCall?): Boolean {
            if (argument == null) return false
            if (expected.transaction != argument.transaction) return false
            if (expected.from != argument.from) return false
            if (expected.block != argument.block) return false
            return true
        }
    }

    companion object {
        private val TEST_ADDRESS = "0xbaddad".asEthereumAddress()!!
        private val TEST_SAFE = "0xdadada".asEthereumAddress()!!
        private val ENS_ADDRESS = BuildConfig.ENS_REGISTRY.asEthereumAddress()!!
        private const val GET_ADDRESS = "0x3b3b57de"
        private const val GET_RESOLVER = "0x0178b8bf"
    }
}
