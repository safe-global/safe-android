package io.gnosis.data.repositories

import io.gnosis.data.models.Chain
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pm.gnosis.ethereum.Block
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthRequest
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.net.UnknownHostException

class EnsRepositoryTest {

    private lateinit var repository: EnsRepository

    private val normalizerMock = mockk<EnsNormalizer>()
    private val ethereumRepository = mockk<EthereumRepository>()
    private val defaultChain = Chain.DEFAULT_CHAIN

    @Before
    fun setup() {
        repository = EnsRepository(normalizerMock, ethereumRepository)
    }

    @Test
    fun `resolve (normalize failure) should throw`() = runBlocking {
        every { ethereumRepository.rpcUrl = any() } just Runs
        every { normalizerMock.normalize(any()) } throws IllegalArgumentException()
        val address = ""

        val actual = runCatching { repository.resolve(address, defaultChain) }

        with(actual) {
            assertTrue(isFailure)
            assertTrue(exceptionOrNull() is IllegalArgumentException)
        }

        coVerify { ethereumRepository.rpcUrl = any() }
        coVerify(exactly = 1) { normalizerMock.normalize(address) }
        coVerify { ethereumRepository.request(any() as EthCall) wasNot Called }
    }

    @Test
    fun `resolve (bad resolver address) should throw`() = runBlocking {
        every { ethereumRepository.rpcUrl = any() } just Runs
        coEvery { ethereumRepository.request(any<EthRequest<*>>()) } throws UnknownHostException()
        every { normalizerMock.normalize(any()) } returns ""
        val address = ""

        val actual = runCatching { repository.resolve(address, defaultChain) }

        with(actual) {
            assertTrue(isFailure)
            assertTrue(exceptionOrNull() is UnknownHostException)
        }
        coVerifySequence {
            ethereumRepository.rpcUrl = any()
            normalizerMock.normalize(address)
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction.Legacy(
                        to = defaultChain.ensRegistryAddress?.asEthereumAddress()!!,
                        data = GET_RESOLVER + "0000000000000000000000000000000000000000000000000000000000000000",
                        chainId = defaultChain.chainId
                    ),
                    block = Block.LATEST
                )
            )
        }
    }

    @Test
    fun `resolve (resolver request failure) should throw`() = runBlocking {
        val address = "eth"
        every { ethereumRepository.rpcUrl = any() } just Runs
        coEvery { ethereumRepository.request(any<EthRequest<*>>()) } returns
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae",
                        chainId = defaultChain.chainId
                    )
                ).apply {
                    response = EthRequest.Response.Success(TEST_ADDRESS.asEthereumAddressString())
                } andThenThrows UnknownHostException()
        every { normalizerMock.normalize(any()) } returns address

        val actual = runCatching { repository.resolve(address, defaultChain) }

        with(actual) {
            assertTrue(isFailure)
            assertTrue(exceptionOrNull() is UnknownHostException)
        }

        coVerifySequence {
            ethereumRepository.rpcUrl = any()
            normalizerMock.normalize(address)
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae",
                        chainId = defaultChain.chainId
                    ),
                    block = Block.LATEST
                )
            )
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction.Legacy(
                        to = TEST_ADDRESS,
                        data = GET_ADDRESS + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae",
                        chainId = defaultChain.chainId
                    ),
                    block = Block.LATEST
                )
            )
        }
    }

    @Test
    fun `resolve (valid url and conditions) should return address`() = runBlocking {
        val address = "eth"
        every { ethereumRepository.rpcUrl = any() } just Runs
        coEvery { ethereumRepository.request(any<EthRequest<*>>()) } returns
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae",
                        chainId = defaultChain.chainId
                    )
                ).apply {
                    response = EthRequest.Response.Success(TEST_ADDRESS.asEthereumAddressString())
                } andThen
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae",
                        chainId = defaultChain.chainId
                    )
                ).apply {
                    response = EthRequest.Response.Success(TEST_SAFE.asEthereumAddressString())
                }

        every { normalizerMock.normalize(any()) } returns address

        val actual = runCatching { repository.resolve(address, defaultChain) }

        with(actual) {
            assertTrue(isSuccess)
            assertTrue(getOrNull() == TEST_SAFE)
        }

        coVerifySequence {
            ethereumRepository.rpcUrl = any()
            normalizerMock.normalize(address)
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_RESOLVER + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae",
                        chainId = defaultChain.chainId
                    ),
                    block = Block.LATEST
                )
            )
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction.Legacy(
                        to = TEST_ADDRESS,
                        data = GET_ADDRESS + "93cdeb708b7545dc668eb9280176169d1c33cfd8ed6f04690a0bcc88a93fc4ae",
                        chainId = defaultChain.chainId
                    ),
                    block = Block.LATEST
                )
            )
        }
    }

    @Test
    fun `resolveReverse (valid resolver and address) should return name`() = runBlocking {
        every { ethereumRepository.rpcUrl = any() } just Runs
        coEvery { ethereumRepository.request(any<EthRequest<*>>()) } returns
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_RESOLVER + "f0071e6238d539afe51473967bba6b71de272de1bd4010584554dd682d65e5d6",
                        chainId = defaultChain.chainId
                    )
                ).apply {
                    response = EthRequest.Response.Success(ENS_ADDRESS.asEthereumAddressString())
                } andThen
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_NAME + "f0071e6238d539afe51473967bba6b71de272de1bd4010584554dd682d65e5d6",
                        chainId = defaultChain.chainId
                    )
                ).apply {
                    response =
                        EthRequest.Response.Success("0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000f6c696c74657374736166652e6574680000000000000000000000000000000000")
                }

        val actual = runCatching { repository.reverseResolve(TEST_SAFE, defaultChain) }

        with(actual) {
            assertTrue(isSuccess)
            assertTrue(getOrNull() == "liltestsafe.eth")
        }

        coVerifySequence {
            ethereumRepository.rpcUrl = any()
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_RESOLVER + "f0071e6238d539afe51473967bba6b71de272de1bd4010584554dd682d65e5d6",
                        chainId = defaultChain.chainId
                    ),
                    block = Block.LATEST
                )
            )
            ethereumRepository.request(
                EthCall(
                    transaction = Transaction.Legacy(
                        to = ENS_ADDRESS,
                        data = GET_NAME + "f0071e6238d539afe51473967bba6b71de272de1bd4010584554dd682d65e5d6",
                        chainId = defaultChain.chainId
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
        private const val GET_NAME = "0x691f3431" // name(bytes32)
    }
}
