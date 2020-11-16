package io.gnosis.data.repositories

import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.assets.*
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import java.math.BigInteger

class TokenRepository(
    private val gatewayApi: GatewayApi
) {

    suspend fun loadBalanceOf(safe: Solidity.Address): CoinBalances {
        val response = gatewayApi.loadBalances(safe.asEthereumAddressChecksumString())
        return CoinBalances(response.fiatTotal, response.items.map {

            if (it.tokenInfo.address == null || it.tokenInfo.address == ZERO_ADDRESS)
                Balance(
                    ETH_TOKEN_INFO,
                    it.balance,
                    it.fiatBalance
                )
            else
                Balance(
                    TokenInfo(
                        TokenType.valueOf(it.tokenInfo.tokenType),
                        it.tokenInfo.address,
                        it.tokenInfo.decimals,
                        it.tokenInfo.symbol,
                        it.tokenInfo.name,
                        it.tokenInfo.logoUri
                            ?: "https://gnosis-safe-token-logos.s3.amazonaws.com/${it.tokenInfo.address.asEthereumAddressChecksumString()}.png"
                    ),
                    it.balance,
                    it.fiatBalance
                )
        })
    }

    suspend fun loadCollectiblesOf(safe: Solidity.Address): List<Collectible> =
        gatewayApi.loadCollectibles(safe.asEthereumAddressChecksumString())
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

    companion object {
        val ZERO_ADDRESS = Solidity.Address(BigInteger.ZERO)
        val ETH_TOKEN_INFO = TokenInfo(
            TokenType.ETHER,
            ZERO_ADDRESS,
            18,
            "ETH",
            "Ether",
            "local::ethereum"
        )
    }
}
