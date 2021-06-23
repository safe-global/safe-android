package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.ChainDao
import io.gnosis.data.models.Chain
import io.gnosis.data.models.ChainInfo

class ChainInfoRepository(
    private val chainDao: ChainDao,
    private val gatewayApi: GatewayApi
) {
    suspend fun getChainInfo(): List<ChainInfo> {

        val result = gatewayApi.loadChainInfo().results
        result.forEach {
            val chain = Chain(it.chainId, it.chainName)
            chainDao.insert(chain)
        }

        return result
    }

}
