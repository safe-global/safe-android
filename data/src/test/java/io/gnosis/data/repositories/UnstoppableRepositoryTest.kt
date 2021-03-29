package io.gnosis.data.repositories

import com.unstoppabledomains.config.network.model.Network
import com.unstoppabledomains.exceptions.ns.NSExceptionCode
import com.unstoppabledomains.exceptions.ns.NSExceptionParams
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import com.unstoppabledomains.resolution.DomainResolution
import com.unstoppabledomains.resolution.Resolution
import com.unstoppabledomains.resolution.naming.service.NamingServiceType
import io.gnosis.data.models.Safe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class UnstoppableRepositoryTest {

    private lateinit var repository: UnstoppableDomainsRepository;
    private lateinit var resolutionLib: DomainResolution;

    @Before
    fun setup() {
        resolutionLib = mockk();
        repository = UnstoppableDomainsRepository(resolutionLib);
    }

    @Test
    fun `resolve - (domain) should succeed`() = run {
        coEvery {
            resolutionLib.getAddress(SUCCESS_DOMAIN, "eth")
        } returns "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A";


        val addr = runBlocking {
            repository.resolve(SUCCESS_DOMAIN)
        }

        coVerify { resolutionLib.getAddress(SUCCESS_DOMAIN, "eth") }
        assert(addr == SUCCESS_ADDR);
    }


    @Test
    fun `resolve - (domain) should fail`() = runBlocking {
        coEvery {
            resolutionLib.getAddress(FAIL_DOMAIN, "eth")
        } throws NamingServiceException(NSExceptionCode.UnregisteredDomain, NSExceptionParams("d", FAIL_DOMAIN))

        try { repository.resolve(FAIL_DOMAIN); }
        catch(err: NamingServiceException) {
            assert(err.code == NSExceptionCode.UnregisteredDomain)
        }
        coVerify { resolutionLib.getAddress(FAIL_DOMAIN, "eth") }
    }

    companion object {
        private val SUCCESS_ADDR = "0x1C8b9B78e3085866521FE206fa4c1a67F49f153A".asEthereumAddress()!!
        private val SUCCESS_DOMAIN = "udtestdev-creek.crypto"
        private val FAIL_DOMAIN = "brad.crypto"

    }
}