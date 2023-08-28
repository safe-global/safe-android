package io.gnosis.data.repositories

import com.unstoppabledomains.exceptions.ns.NSExceptionCode
import com.unstoppabledomains.exceptions.ns.NSExceptionParams
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import com.unstoppabledomains.resolution.DomainResolution
import io.gnosis.data.models.Chain
import io.gnosis.data.models.RpcAuthentication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class UnstoppableRepositoryTest {

    private lateinit var repository: UnstoppableDomainsRepository
    private lateinit var resolutionLib: DomainResolution

    @Before
    fun setup() {
        resolutionLib = mockk()
        repository = UnstoppableDomainsRepository(resolutionLib)
    }

    @Test
    fun `resolve - (domain) should succeed`() = run {
        coEvery {
            resolutionLib.getAddress(SUCCESS_DOMAIN, "eth")
        } returns "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A"


        val addr = runBlocking {
            repository.resolve(SUCCESS_DOMAIN, Chain.ID_GOERLI)
        }

        coVerify { resolutionLib.getAddress(SUCCESS_DOMAIN, "eth") }
        assertTrue(addr == SUCCESS_ADDR)
    }


    @Test
    fun `resolve - (domain) should fail`() = runBlocking {
        coEvery {
            resolutionLib.getAddress(FAIL_DOMAIN, "eth")
        } throws NamingServiceException(
            NSExceptionCode.UnregisteredDomain,
            NSExceptionParams("d", FAIL_DOMAIN)
        )

        try {
            repository.resolve(FAIL_DOMAIN, Chain.ID_GOERLI)
        } catch (err: NamingServiceException) {
            assertTrue(err.code == NSExceptionCode.UnregisteredDomain)
        }
        coVerify { resolutionLib.getAddress(FAIL_DOMAIN, "eth") }
    }

    @Test
    fun `canResolve - (1) should succeed for Mainnet`() {

        val result = repository.canResolve(
            Chain(
                Chain.ID_MAINNET,
                false,
                "Mainnet",
                "eth",
                "",
                "",
                "",
                RpcAuthentication.API_KEY_PATH,
                "",
                "",
                null,
                listOf()
            )
        )

        assertTrue(result)
    }

    @Test
    fun `canResolve - (4) should succeed for Rinkeby`() {
        repository = UnstoppableDomainsRepository()

        val result = repository.canResolve(
            Chain(
                Chain.ID_GOERLI,
                true,
                "Goerli",
                "gor",
                "",
                "",
                "",
                RpcAuthentication.API_KEY_PATH,
                "",
                "",
                null,
                listOf()
            )
        )

        assertTrue(result)
    }

    @Test
    fun `canResolve - (17) should fail for unsupported_chain`() {

        val result = repository.canResolve(
            Chain(
                BigInteger.valueOf(17),
                true,
                "Unknown",
                "",
                "",
                "",
                "",
                RpcAuthentication.API_KEY_PATH,
                "",
                "",
                null,
                listOf()
            )
        )

        assertFalse(result)
    }

    companion object {
        private val SUCCESS_ADDR =
            "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!
        private const val SUCCESS_DOMAIN = "udtestdev-creek.crypto"
        private const val FAIL_DOMAIN = "brad.crypto"

    }
}
