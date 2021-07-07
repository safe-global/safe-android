package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.ChainDao
import io.gnosis.data.models.*
import io.mockk.*
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress

class ChainInfoRepositoryTest {

    private val chainDao = mockk<ChainDao>()
    private val gatewayApi = mockk<GatewayApi>()

    private val chainInfoRepository = ChainInfoRepository(chainDao, gatewayApi)

    private val rinkebyChainInfo = ChainInfo(
        4, "Rinkeby", null, "", "",
        NativeCurrency("", "", 18), "",
        ChainTheme("", "")
    )
    private val pagedResult: List<ChainInfo> = listOf(
        ChainInfo(
            1, "Mainnet", "", "", "",
            NativeCurrency(
                "", "", 18
            ), "",
            ChainTheme("", "")
        ),
        rinkebyChainInfo,
        ChainInfo(
            137, "Matic", "", "", "",
            NativeCurrency("", "", 18), "",
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
        val safes = listOf(Safe("0x00".asEthereumAddress()!!, "", 4))

        chainInfoRepository.updateChainInfo(pagedResult, safes)

        coVerify(exactly = 1) { chainDao.save(Chain(4, "Rinkeby", "", "", null)) }
    }
}
