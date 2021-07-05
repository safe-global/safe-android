package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.ChainDao
import io.gnosis.data.models.Chain
import io.gnosis.data.models.ChainInfo
import io.gnosis.data.models.Page
import io.gnosis.data.models.Safe

class ChainInfoRepository(
    private val chainDao: ChainDao,
    private val gatewayApi: GatewayApi
) {

    suspend fun loadChainInfoPage(pageLink: String): Page<ChainInfo> =
        gatewayApi.loadChainInfoPage(pageLink)

    suspend fun getChainInfo(): Page<ChainInfo> =
        gatewayApi.loadChainInfo()

    suspend fun updateChainInfo(chains: List<ChainInfo>, safes: List<Safe>) {
        safes.map { it.chainId }.toSet().forEach { chainId ->
            val chainInfo = chains.find { it.chainId == chainId }
            chainInfo?.let {
                val chain = Chain(it.chainId, it.chainName, it.theme.textColor, it.theme.backgroundColor)
                chainDao.save(chain)
            }
        }
    }

    suspend fun save(chain: Chain) = chainDao.save(chain)

    suspend fun getChains(): List<Chain> = chainDao.loadAll()
}

