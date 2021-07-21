package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Safe
import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.models.assets.Collectible
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString

class TokenRepository(private val gatewayApi: GatewayApi) {

    suspend fun loadBalanceOf(safe: Safe, fiatCode: String): CoinBalances {
        val response = gatewayApi.loadBalances(address = safe.address.asEthereumAddressChecksumString(), fiat = fiatCode, chainId = safe.chainId)
        return CoinBalances(response.fiatTotal, response.items)
    }

    suspend fun loadCollectiblesOf(safe: Safe): List<Collectible> =
        gatewayApi.loadCollectibles(chainId = safe.chainId, safeAddress = safe.address.asEthereumAddressChecksumString())
            .asSequence()
            .groupBy {
                it.address
            }
            .toList()
            .map {
                it.second.sortedWith(Comparator { c1, c2 ->
                    if (c1.name.isNullOrBlank()) {
                        if (c2.name.isNullOrBlank())
                            0
                        else
                            1
                    } else if (c2.name.isNullOrBlank()) {
                        -1
                    } else {
                        c1.name.compareTo(c2.name)
                    }
                })
            }
            .sortedWith(Comparator { l1, l2 ->
                if (l1.first().tokenName.isBlank()) {
                    if (l2.first().tokenName.isBlank())
                        0
                    else
                        1
                } else if (l2.first().tokenName.isBlank()) {
                    -1
                } else {
                    l1.first().tokenName.compareTo(l2.first().tokenName)
                }
            })
            .flatten()
            .toList()
}
