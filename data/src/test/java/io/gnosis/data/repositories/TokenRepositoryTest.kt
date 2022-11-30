package io.gnosis.data.repositories

import com.squareup.moshi.Types
import io.gnosis.data.BuildConfig
import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Safe
import io.gnosis.data.models.assets.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val gatewayApi = mockk<GatewayApi>()
    private val tokenRepository = TokenRepository(gatewayApi)

    private val moshi = dataMoshi
    private val balancesAdapter = moshi.adapter(CoinBalances::class.java)

    @Test
    fun `loadBalancesOf (transactionApi failure) should throw`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "Name", CHAIN_ID)
        val throwable = Throwable()
        coEvery { gatewayApi.loadBalances(address = any(), fiat = any(), chainId = CHAIN_ID) } throws throwable

        val actual = runCatching { tokenRepository.loadBalanceOf(safe, "USD") }

        with(actual) {
            assertTrue(isFailure)
            assertEquals(throwable, exceptionOrNull())
        }
        coVerify(exactly = 1) { gatewayApi.loadBalances(address = safe.address.asEthereumAddressChecksumString(), fiat = "USD", chainId = CHAIN_ID) }
    }

    @Test
    fun `loadBalancesOf (zero token address) should use ETH_TOKEN_INFO`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "Name", CHAIN_ID)
        val balance = buildBalance(1)
        val balanceExpected = buildBalance(1).let { it.copy(tokenInfo = NATIVE_CURRENCY_INFO) }
        coEvery { gatewayApi.loadBalances(address = any(), fiat = any(), chainId = CHAIN_ID) } returns
                CoinBalances(
                    BigDecimal.ZERO,
                    listOf(
                        balance.copy(
                            balance.tokenInfo.copy(
                                tokenType = TokenType.NATIVE_CURRENCY,
                                address = Solidity.Address(BigInteger.ZERO),
                                decimals = 18,
                                logoUri = "local::native_currency",
                                name = BuildConfig.NATIVE_CURRENCY_NAME,
                                symbol = BuildConfig.NATIVE_CURRENCY_SYMBOL
                            )
                        )

                    )
                )

        val actual = runCatching { tokenRepository.loadBalanceOf(safe, "USD") }

        with(actual) {
            assertTrue(isSuccess)
            assertEquals(
                CoinBalances(BigDecimal.ZERO, listOf(balanceExpected)),
                getOrNull()
            )
        }
        coVerifySequence {
            gatewayApi.loadBalances(address = safe.address.asEthereumAddressChecksumString(), fiat = "USD", chainId = CHAIN_ID)
        }
    }

    @Test
    fun `loadBalancesOf (address) should succeed`() = runBlocking {
        val safe = Safe(Solidity.Address(BigInteger.ONE), "Name", CHAIN_ID)
        val balance = buildBalance(1)
        val balanceExpected = buildBalance(1)
        coEvery { gatewayApi.loadBalances(address = any(), fiat = any(), chainId = CHAIN_ID) } returns CoinBalances(BigDecimal.ZERO, listOf(balance))

        val actual = runCatching { tokenRepository.loadBalanceOf(safe, "USD") }

        with(actual) {
            assertTrue(isSuccess)
            assertEquals(
                CoinBalances(BigDecimal.ZERO, listOf(balanceExpected)),
                getOrNull()
            )
        }
        coVerifySequence {
            gatewayApi.loadBalances(address = safe.address.asEthereumAddressChecksumString(), fiat = "USD", chainId = CHAIN_ID)
        }
    }

    @Test
    fun `loadBalancesOf (address) should parse correct amounts`() = runBlocking {
        val jsonString: String = readResource("load_balances_usd.json")

        val balances = balancesAdapter.fromJson(jsonString)!!

        assertEquals(BigDecimal("4467.6637"), balances.fiatTotal)
        assertEquals(3, balances.items.size)

        assertEquals("9083012300000128000".parseToBigInteger(), balances.items[0].balance)
        assertEquals("3650.9168".toBigDecimal(), balances.items[0].fiatBalance)

        assertEquals("9000000000000000001".parseToBigInteger(), balances.items[1].balance)
        assertEquals("37.2906".toBigDecimal(), balances.items[1].fiatBalance)

        assertEquals("474804615".parseToBigInteger(), balances.items[2].balance)
        assertEquals("4.021".toBigDecimal(), balances.items[2].fiatBalance)
    }

    private fun buildBalance(index: Long) =
        Balance(
            buildTokenInfo(index),
            BigInteger.valueOf(index),
            BigDecimal.valueOf(index)
        )

    private fun buildTokenInfo(index: Long) =
        TokenInfo(
            TokenType.ERC20,
            Solidity.Address(BigInteger.valueOf(index)),
            15,
            "symbol$index",
            "name$index",
            "logo.uri.$index"
        )

    private fun readResource(fileName: String): String {
        return BufferedReader(
            InputStreamReader(
                this::class.java.getClassLoader()?.getResourceAsStream(fileName)!!
            )
        ).lines().parallel().collect(Collectors.joining("\n"))
    }

    companion object {
        private val CHAIN_ID = BuildConfig.CHAIN_ID.toBigInteger()
        private val NATIVE_CURRENCY_INFO = TokenInfo(
            TokenType.NATIVE_CURRENCY,
            Solidity.Address(BigInteger.ZERO),
            18,
            BuildConfig.NATIVE_CURRENCY_SYMBOL,
            BuildConfig.NATIVE_CURRENCY_NAME,
            "local::native_currency"
        )
    }
}
