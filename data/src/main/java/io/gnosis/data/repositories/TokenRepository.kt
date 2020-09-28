package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.backend.dto.tokenAsErc20Token
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.models.Balance
import io.gnosis.data.models.Collectible
import io.gnosis.data.models.Erc20Token
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import java.math.BigInteger

class TokenRepository(
    private val erc20TokenDao: Erc20TokenDao,
    private val transactionServiceApi: TransactionServiceApi
) {

    suspend fun loadBalanceOf(safe: Solidity.Address): List<Balance> =
        transactionServiceApi.loadBalances(safe.asEthereumAddressChecksumString())
            .map {
                val token = it.tokenAsErc20Token()
                erc20TokenDao.insertToken(token)
                Balance(token, it.balance, it.balanceUsd)
            }

    //FIXME: use client gateway (grouping and sorting will be done on the backend side)
    suspend fun loadCollectiblesOf(safe: Solidity.Address): List<Collectible> =
        transactionServiceApi.loadCollectibles(safe.asEthereumAddressChecksumString())
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
            .map {
                it.toCollectible()
            }

    companion object {
        val ZERO_ADDRESS = Solidity.Address(BigInteger.ZERO)
        val ETH_TOKEN_INFO = Erc20Token(ZERO_ADDRESS, "Ether", "ETH", 18, "local::ethereum")
        val ETH_SERVICE_TOKEN_INFO = ServiceTokenInfo(ZERO_ADDRESS, 18, "ETH", "Ether", "local::ethereum")
    }
}
