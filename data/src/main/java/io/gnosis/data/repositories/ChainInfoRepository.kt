package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.ChainDao
import io.gnosis.data.models.Chain
import io.gnosis.data.models.ChainInfo

class ChainInfoRepository(
    private val chainDao: ChainDao,
    private val gatewayApi: GatewayApi,
    private val safeRepository: SafeRepository
) {
    suspend fun getChainInfo(): List<ChainInfo> {
        val result = gatewayApi.loadChainInfo().results

        val safes = safeRepository.getSafes()
        safes.forEach { safe ->
            println("---> safe: $safe")
        }
        result.forEach {
            val chain = Chain(it.chainId, it.chainName, it.theme.textColor, it.theme.backgroundColor)
            chainDao.save(chain)
        }

        return result
    }
}
