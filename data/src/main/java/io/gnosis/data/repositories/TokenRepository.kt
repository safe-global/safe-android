package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Page
import io.gnosis.data.models.Safe
import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.models.assets.Collectible
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString

class TokenRepository(private val gatewayApi: GatewayApi) {

    suspend fun loadBalanceOf(safe: Safe, fiatCode: String): CoinBalances {
        val response = gatewayApi.loadBalances(address = safe.address.asEthereumAddressChecksumString(), fiat = fiatCode, chainId = safe.chainId)
        return CoinBalances(response.fiatTotal, response.items)
    }

    suspend fun getCollectibles(safe: Safe): Page<Collectible> =
        gatewayApi.loadCollectibles(chainId = safe.chainId, safeAddress = safe.address.asEthereumAddressChecksumString())


    suspend fun loadCollectiblesPage(pageLink: String): Page<Collectible> =
        gatewayApi.loadCollectiblesPage(pageLink)
}
