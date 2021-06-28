package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.ChainDao
import io.gnosis.data.models.Chain
import io.gnosis.data.models.ChainInfo
import io.gnosis.data.models.Safe

class ChainInfoRepository(
    private val chainDao: ChainDao,
    private val gatewayApi: GatewayApi
) {
    suspend fun getChainInfo(): List<ChainInfo> = gatewayApi.loadChainInfo().results

    suspend fun updateChainInfo(chains: List<ChainInfo>, safes: List<Safe>) {
        safes.forEach { safe ->
            chains.forEach { chainInfo ->
                if (safe.chainId == chainInfo.chainId) {
                    val chain = Chain(chainInfo.chainId, chainInfo.chainName, chainInfo.theme.textColor, chainInfo.theme.backgroundColor)
                    chainDao.save(chain)
                }
            }
        }
    }

    suspend fun getChains(): List<Chain> = chainDao.loadAll()
}

