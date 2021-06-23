package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.db.daos.SafeDao
import io.gnosis.data.models.ChainInfo

class ChainInfoRepository(
    private val safeDao: SafeDao,
    private val gatewayApi: GatewayApi
) {
    suspend fun getChainInfo(): List<ChainInfo> = gatewayApi.loadChainInfo().results
}
