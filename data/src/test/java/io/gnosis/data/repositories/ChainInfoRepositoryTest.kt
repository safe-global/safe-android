package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.ChainDao
import io.gnosis.data.models.*
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

class ChainInfoRepositoryTest {

    private val chainDao = mockk<ChainDao>()
    private val gatewayApi = mockk<GatewayApi>()

    private val chainInfoRepository = ChainInfoRepository(chainDao, gatewayApi)

    private val rinkebyChainInfo = ChainInfo(
        Chain.ID_RINKEBY,
        "Rinkeby",
        "rin",
        null,
        RpcUri(RpcAuthentication.API_KEY_PATH, ""),
        BlockExplorerTemplate("", ""),
        NativeCurrency("", "", 18, ""),
        "",
        ChainTheme("", "")
    )
    private val pagedResult: List<ChainInfo> = listOf(
        ChainInfo(
            Chain.ID_MAINNET,
            "Mainnet",
            "eth",
            null,
            RpcUri(RpcAuthentication.API_KEY_PATH, ""),
            BlockExplorerTemplate("", ""),
            NativeCurrency("", "", 18, ""),
            "",
            ChainTheme("", "")
        ),
        rinkebyChainInfo,
        ChainInfo(
            BigInteger.valueOf(137),
            "Matic",
            "matic",
            null,
            RpcUri(RpcAuthentication.API_KEY_PATH, ""),
            BlockExplorerTemplate("", ""),
            NativeCurrency("", "", 18, ""),
            "",
            ChainTheme("", "")
        )
    )

    @Before
    fun setUp() {

        coEvery { gatewayApi.loadChainInfo() } returns Page(0, null, null, pagedResult)
    }

    @Test
    fun getChainInfo() = runBlocking {

        val result = chainInfoRepository.getChainInfo().results

        assertEquals(result, pagedResult)
    }

    @Test
    fun updateChainInfo() = runBlocking {
        coEvery { chainDao.save(any()) } just Runs
        coEvery { chainDao.saveCurrency(any()) } just Runs
        val safes = listOf(Safe("0x00".asEthereumAddress()!!, "", Chain.ID_RINKEBY))

        chainInfoRepository.updateChainInfo(pagedResult, safes)

        coVerify(exactly = 1) {
            chainDao.save(
                Chain(
                    Chain.ID_RINKEBY,
                    "Rinkeby",
                    "rin",
                    "",
                    "",
                    "",
                    RpcAuthentication.API_KEY_PATH,
                    "",
                    "",
                    null
                )
            )
        }
    }
}
