package io.gnosis.data.repositories

import io.gnosis.data.backend.RelayServiceApi
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.models.Balance
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class TokenRepositoryTest {

    private val erc20TokenDao = mockk<Erc20TokenDao>()
    private val relayServiceApi = mockk<RelayServiceApi>()
    private val transactionServiceApi = mockk<TransactionServiceApi>()

    private val tokenRepository = TokenRepository(erc20TokenDao, transactionServiceApi, relayServiceApi)

    @Test
    fun `loadToken (token address) should return token`() = runBlocking {
        val token = buildFakeToken(1)
        coEvery { relayServiceApi.tokenInfo(any()) } returns token
        coEvery { erc20TokenDao.insertToken(any()) } just Runs

        val actual = tokenRepository.loadToken(Solidity.Address(BigInteger.ONE))

        assertEquals(token.toErc20Token(), actual)
        coVerifySequence {
            relayServiceApi.tokenInfo(token.address.asEthereumAddressString())
            erc20TokenDao.insertToken(token.toErc20Token())
        }
    }

    @Test
    fun `loadToken (relay service failure) should throw`() = runBlocking {
        val throwable = IllegalStateException()
        val tokenAddress = Solidity.Address(BigInteger.ONE)
        coEvery { relayServiceApi.tokenInfo(any()) } throws throwable

        val actual = runCatching { tokenRepository.loadToken(tokenAddress) }

        with(actual) {
            assert(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify {
            relayServiceApi.tokenInfo(tokenAddress.asEthereumAddressString())
            erc20TokenDao wasNot Called
        }
    }

    @Test
    fun `loadBalancesOf ()`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
//        coEvery { transactionServiceApi.loadBalances(any()) }

//        val actual = tokenRepository.loadBalancesOf(address)
    }

    private fun buildFakeToken(index: Long) =
        ServiceTokenInfo(
            Solidity.Address(BigInteger.valueOf(index)),
            15,
            "symbol$index",
            "name$index",
            "logo.url.$index"
        )

    private fun buildBalance(index: Long) =
        Balance(
            buildFakeToken(index).toErc20Token(),
            BigInteger.valueOf(index),
            "$index.0"
        )
}
