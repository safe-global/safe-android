package io.gnosis.data.repositories

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import pm.gnosis.ethereum.*
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.net.UnknownHostException

class EnsRepositoryTest {

    private lateinit var repository: EnsRepository

    private val normalizerMock = mockk<EnsNormalizer>()
    private val ethereumRepository = mockk<EthereumRepository>()


    @Before
    fun setup() {
        repository = EnsRepository(normalizerMock, ethereumRepository)
    }

    @Test
    fun `resolve (normalize failure) should throw`() = runBlocking {
        every { normalizerMock.normalize(any()) } throws IllegalArgumentException()
        val address = ""

        val actual = runCatching { repository.resolve(address) }

        with(actual) {
            assert(isFailure)
            assert(exceptionOrNull() is IllegalArgumentException)
        }

        coVerify(exactly = 1) { normalizerMock.normalize(address) }
        coVerify { ethereumRepository wasNot Called }
    }

    @Test
    fun `resolve (bad resolver address) should throw`() = runBlocking {
        coEvery { ethereumRepository.request(any<EthRequest<*>>()) } throws UnknownHostException()
        every { normalizerMock.normalize(any()) } returns ""
        val address = ""

        val actual = runCatching { repository.resolve(address) }


        with(actual) {
            assert(isFailure)
            assert(exceptionOrNull() is UnknownHostException)
        }
        coVerifySequence {
            normalizerMock.normalize(address)
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction(
                        ENS_ADDRESS,
                        data = GET_RESOLVER + "0000000000000000000000000000000000000000000000000000000000000000"
                    ),
                    block = Block.LATEST
                )
            )
        }
    }

    @Test
    fun `resolve (resolver request failure) should throw`() = runBlocking {
        val address = "eth"
        coEvery { ethereumRepository.request(any<EthRequest<*>>()) } returns
                EthCall(
                    transaction = Transaction(
                        ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                    )
                ).apply {
                    response = EthRequest.Response.Success(TEST_ADDRESS.asEthereumAddressString())
                } andThenThrows UnknownHostException()
        every { normalizerMock.normalize(any()) } returns address

        val actual = runCatching { repository.resolve(address) }

        with(actual) {
            assert(isFailure)
            assert(exceptionOrNull() is UnknownHostException)
        }

        coVerifySequence {
            normalizerMock.normalize(address)
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction(
                        ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                    ),
                    block = Block.LATEST
                )
            )
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction(
                        TEST_ADDRESS,
                        data = GET_ADDRESS + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                    ),
                    block = Block.LATEST
                )
            )
        }

    }

    @Test
    fun `resolve (valid url and conditions) should return address`() = runBlocking {
        val address = "eth"
        coEvery { ethereumRepository.request(any<EthRequest<*>>()) } returns
                EthCall(
                    transaction = Transaction(
                        ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                    )
                ).apply {
                    response = EthRequest.Response.Success(TEST_ADDRESS.asEthereumAddressString())
                } andThen
                EthCall(
                    transaction = Transaction(
                        ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                    )
                ).apply {
                    response = EthRequest.Response.Success(TEST_SAFE.asEthereumAddressString())
                }

        every { normalizerMock.normalize(any()) } returns address

        val actual = runCatching { repository.resolve(address) }

        with(actual) {
            assert(isSuccess)
            assert(getOrNull() == TEST_SAFE)
        }

        coVerifySequence {
            normalizerMock.normalize(address)
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction(
                        ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                    ),
                    block = Block.LATEST
                )
            )
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction(
                        TEST_ADDRESS,
                        data = GET_ADDRESS + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae"
                    ),
                    block = Block.LATEST
                )
            )
        }
    }

    companion object {
        private val TEST_ADDRESS = "0xbaddad".asEthereumAddress()!!
        private val TEST_SAFE = "0xdadada".asEthereumAddress()!!
        private val ENS_ADDRESS = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e".asEthereumAddress()!!
        private const val GET_RESOLVER = "0x0178b8bf"
        private const val GET_ADDRESS = "0x3b3b57de"
    }
}
