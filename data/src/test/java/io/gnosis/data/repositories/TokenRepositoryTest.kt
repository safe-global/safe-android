package io.gnosis.data.repositories

import io.gnosis.data.backend.RelayServiceApi
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.ServiceBalance
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.models.Balance
import io.gnosis.data.models.Erc20Token
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_TOKEN_INFO
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
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
        val token = buildServiceTokenInfo(1)
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
    fun `loadBalancesOf (transactionApi failure) should throw`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val throwable = Throwable()
        coEvery { transactionServiceApi.loadBalances(any()) } throws throwable

        val actual = runCatching { tokenRepository.loadBalancesOf(address) }

        with(actual) {
            assert(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString()) }
    }

    @Test
    fun `loadBalancesOf (empty DAO and relayService failure) should throw`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val throwable = Throwable()
        coEvery { transactionServiceApi.loadBalances(any()) } returns listOf(buildServiceBalance(1))
        coEvery { erc20TokenDao.loadToken(any()) } returns null
        coEvery { relayServiceApi.tokenInfo(any()) } throws throwable

        val actual = runCatching { tokenRepository.loadBalancesOf(address) }

        with(actual) {
            assert(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerifySequence {
            transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString())
            erc20TokenDao.loadToken(address)
            relayServiceApi.tokenInfo(address.asEthereumAddressString())
        }
    }

    @Test
    fun `loadBalancesOf (empty DAO) should call relayService`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val serviceTokenInfo = buildServiceTokenInfo(1)
        val serviceBalance = buildServiceBalance(1)
        coEvery { transactionServiceApi.loadBalances(any()) } returns listOf(serviceBalance)
        coEvery { erc20TokenDao.loadToken(any()) } returns null
        coEvery { erc20TokenDao.insertToken(any()) } just Runs
        coEvery { relayServiceApi.tokenInfo(any()) } returns serviceTokenInfo

        val actual = runCatching { tokenRepository.loadBalancesOf(address) }

        with(actual) {
            assert(isSuccess)
            assertEquals(
                getOrNull(),
                listOf(Balance(serviceTokenInfo.toErc20Token(), serviceBalance.balance, serviceBalance.balanceUsd))
            )
        }
        coVerifySequence {
            transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString())
            erc20TokenDao.loadToken(address)
            relayServiceApi.tokenInfo(address.asEthereumAddressString())
            erc20TokenDao.insertToken(serviceTokenInfo.toErc20Token())
        }
    }

    @Test
    fun `loadBalancesOf (contained in DAO) should not call relayService `() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val serviceTokenInfo = buildServiceTokenInfo(1)
        val serviceBalance = buildServiceBalance(1)
        coEvery { transactionServiceApi.loadBalances(any()) } returns listOf(serviceBalance)
        coEvery { erc20TokenDao.loadToken(any()) } returns buildErc20Token(1)

        val actual = runCatching { tokenRepository.loadBalancesOf(address) }

        with(actual) {
            assert(isSuccess)
            assertEquals(
                getOrNull(),
                listOf(Balance(serviceTokenInfo.toErc20Token(), serviceBalance.balance, serviceBalance.balanceUsd))
            )
        }
        coVerifySequence {
            transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString())
            erc20TokenDao.loadToken(address)
        }
        coVerify { relayServiceApi wasNot Called }
    }

    @Test
    fun `loadBalancesOf (contained in DAO and force refetch) should call relayService`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val serviceTokenInfo = buildServiceTokenInfo(1)
        val serviceBalance = buildServiceBalance(1)
        coEvery { transactionServiceApi.loadBalances(any()) } returns listOf(serviceBalance)
        coEvery { erc20TokenDao.loadToken(any()) } returns buildErc20Token(1)
        coEvery { erc20TokenDao.insertToken(any()) } just Runs
        coEvery { relayServiceApi.tokenInfo(any()) } returns serviceTokenInfo

        val actual = runCatching { tokenRepository.loadBalancesOf(address, true) }

        with(actual) {
            assert(isSuccess)
            assertEquals(
                getOrNull(),
                listOf(Balance(serviceTokenInfo.toErc20Token(), serviceBalance.balance, serviceBalance.balanceUsd))
            )
        }
        coVerifySequence {
            transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString())
            erc20TokenDao.loadToken(address)
            relayServiceApi.tokenInfo(address.asEthereumAddressString())
            erc20TokenDao.insertToken(serviceTokenInfo.toErc20Token())
        }
    }

    @Test
    fun `loadBalancesOf (null token address) should use ETH_TOKEN_INFO`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val serviceBalance = buildServiceBalance(1)
        coEvery { transactionServiceApi.loadBalances(any()) } returns listOf(serviceBalance.copy(tokenAddress = null))

        val actual = runCatching { tokenRepository.loadBalancesOf(address) }

        with(actual) {
            assert(isSuccess)
            assertEquals(
                getOrNull(),
                listOf(Balance(ETH_TOKEN_INFO, serviceBalance.balance, serviceBalance.balanceUsd))
            )
        }
        coVerifySequence {
            transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString())
        }
        coVerify { relayServiceApi wasNot Called }
        coVerify { erc20TokenDao wasNot Called }
    }

    private fun buildErc20Token(index: Long) =
        Erc20Token(
            Solidity.Address(BigInteger.valueOf(index)),
            "name$index",
            "symbol$index",
            15,
            "logo.url.$index"
        )

    private fun buildBalance(index: Long) =
        Balance(
            buildServiceTokenInfo(index).toErc20Token(),
            BigInteger.valueOf(index),
            "$index.0"
        )

    private fun buildServiceBalance(index: Long) =
        ServiceBalance(
            Solidity.Address(BigInteger.valueOf(index)),
            ServiceBalance.ServiceTokenMeta(
                15,
                "symbol$index",
                "name$index"
            ),
            BigInteger.valueOf(index),
            "$index.00"
        )

    private fun buildServiceTokenInfo(index: Long) =
        ServiceTokenInfo(
            Solidity.Address(BigInteger.valueOf(index)),
            15,
            "symbol$index",
            "name$index",
            "logo.url.$index"
        )
}
