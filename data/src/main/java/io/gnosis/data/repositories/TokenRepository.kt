package io.gnosis.data.repositories

import io.gnosis.data.BuildConfig
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.assets.CoinBalances
import io.gnosis.data.models.assets.Collectible
import io.gnosis.data.models.assets.TokenInfo
import io.gnosis.data.models.assets.TokenType
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import java.math.BigInteger

class TokenRepository(
    private val gatewayApi: GatewayApi,
    private val settingsRepository: SettingsRepository
) {

    suspend fun loadBalanceOf(safe: Solidity.Address): CoinBalances {
        val response = gatewayApi.loadBalances(safe.asEthereumAddressChecksumString(), settingsRepository.getUserDefaultFiat())
        return CoinBalances(response.fiatTotal, response.items.map {
            if (it.tokenInfo.address == ZERO_ADDRESS)
                it.copy(tokenInfo = it.tokenInfo.copy(logoUri = "local::native_currency"))
            else
                it
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
        val NATIVE_CURRENCY_INFO = TokenInfo(
            TokenType.NATIVE_CURRENCY,
            ZERO_ADDRESS,
            18,
            BuildConfig.NATIVE_CURRENCY_SYMBOL,
            BuildConfig.NATIVE_CURRENCY_NAME,
            "local::native_currency"
        )
    }
}
