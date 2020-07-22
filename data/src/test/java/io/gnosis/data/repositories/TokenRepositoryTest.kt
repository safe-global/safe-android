package io.gnosis.data.repositories

import com.squareup.moshi.Types
import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.ServiceBalance
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.backend.dto.tokenAsErc20Token
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.models.Balance
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_TOKEN_INFO
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.parseToBigInteger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.BigInteger
import java.util.stream.Collectors

class TokenRepositoryTest {

    private val erc20TokenDao = mockk<Erc20TokenDao>()
    private val transactionServiceApi = mockk<TransactionServiceApi>()
    private val tokenRepository = TokenRepository(erc20TokenDao, transactionServiceApi)

    private val moshi = dataMoshi
    private val adapter = moshi.adapter<List<ServiceBalance>>(Types.newParameterizedType(List::class.java, ServiceBalance::class.java))

    @Test
    fun `loadBalancesOf (transactionApi failure) should throw`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val throwable = Throwable()
        coEvery { transactionServiceApi.loadBalances(any()) } throws throwable

        val actual = runCatching { tokenRepository.loadBalanceOf(address) }

        with(actual) {
            assert(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString()) }
        coVerify { erc20TokenDao wasNot Called }
    }

    @Test
    fun `loadBalancesOf (null token address) should use ETH_TOKEN_INFO`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val serviceBalance = buildServiceBalance(1)
        coEvery { transactionServiceApi.loadBalances(any()) } returns listOf(serviceBalance.copy(tokenAddress = null))
        coEvery { erc20TokenDao.insertToken(any()) } just Runs

        val actual = runCatching { tokenRepository.loadBalanceOf(address) }

        with(actual) {
            assert(isSuccess)
            assertEquals(
                getOrNull(),
                listOf(Balance(ETH_TOKEN_INFO, serviceBalance.balance, serviceBalance.balanceUsd))
            )
        }
        coVerifySequence {
            transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString())
            erc20TokenDao.insertToken(ETH_TOKEN_INFO)
        }
    }

    @Test
    fun `loadBalancesOf (address) should succeed`() = runBlocking {
        val address = Solidity.Address(BigInteger.ONE)
        val serviceBalance = buildServiceBalance(1)
        val expectedToken = serviceBalance.tokenAsErc20Token()
        coEvery { transactionServiceApi.loadBalances(any()) } returns listOf(serviceBalance)
        coEvery { erc20TokenDao.insertToken(any()) } just Runs

        val actual = runCatching { tokenRepository.loadBalanceOf(address) }

        with(actual) {
            assert(isSuccess)
            assertEquals(
                getOrNull(),
                listOf(Balance(expectedToken, serviceBalance.balance, serviceBalance.balanceUsd))
            )
        }
        coVerifySequence {
            transactionServiceApi.loadBalances(address.asEthereumAddressChecksumString())
            erc20TokenDao.insertToken(expectedToken)
        }
    }

    @Test
    fun `loadBalancesOf (address) should parse correct amounts`() = runBlocking {
        val jsonString: String = readResource("load_balances_usd.json")

        val balances = adapter.fromJson(jsonString)

        assertEquals(3, balances?.size)

        assertEquals(balances!![0].balance, "1331553306076676".parseToBigInteger())
        assertEquals(balances!![0].balanceUsd, "0.3248".toBigDecimal())

        assertEquals(balances!![1].balance, "234500000000000000".parseToBigInteger())
        assertEquals(balances!![1].balanceUsd, "0.2371".toBigDecimal())

        assertEquals(balances!![2].balance, "100000000000000000".parseToBigInteger())
        assertEquals(balances!![2].balanceUsd, "0.1346".toBigDecimal())
    }

    private fun buildServiceBalance(index: Long) =
        ServiceBalance(
            Solidity.Address(BigInteger.valueOf(index)),
            ServiceBalance.ServiceTokenMeta(
                15,
                "symbol$index",
                "name$index",
                "logo.uri.$index"
            ),
            BigInteger.valueOf(index),
            BigDecimal.valueOf(index)
        )

    private fun buildServiceTokenInfo(index: Long) =
        ServiceTokenInfo(
            Solidity.Address(BigInteger.valueOf(index)),
            15,
            "symbol$index",
            "name$index",
            "logo.url.$index"
        )

    private fun readResource(fileName: String): String {
        return BufferedReader(
            InputStreamReader(
                this::class.java.getClassLoader()?.getResourceAsStream(fileName)!!
            )
        ).lines().parallel().collect(Collectors.joining("\n"))
    }
}
