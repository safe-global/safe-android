package io.gnosis.data.repositories

import io.gnosis.data.backend.RelayServiceApi
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.models.Balance
import io.gnosis.data.models.Erc20Token
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

class TokenRepository(
    private val erc20TokenDao: Erc20TokenDao,
    private val transactionServiceApi: TransactionServiceApi,
    private val relayServiceApi: RelayServiceApi
) {

    suspend fun loadBalancesOf(safe: Solidity.Address, forceRefetch: Boolean = false): List<Balance> =
        transactionServiceApi.loadBalances(safe.asEthereumAddressChecksumString())
            .associateWith { erc20TokenDao.loadToken(it.tokenAddress!!) }
            .map { (balance, tokenFromDao) ->
                val token = when {
                    tokenFromDao != null && !forceRefetch -> tokenFromDao
                    balance.tokenAddress != null -> loadToken(balance.tokenAddress)
                    else -> ETH_TOKEN_INFO
                }
                Balance(token, balance.balance, balance.balanceUsd)
            }

    suspend fun loadToken(address: Solidity.Address): Erc20Token =
        relayServiceApi.tokenInfo(address.asEthereumAddressString()).let {
            it.toErc20Token().apply { erc20TokenDao.insertToken(this) }
        }


    companion object {
        val ETH_ADDRESS = Solidity.Address(BigInteger.ZERO)
        val ETH_TOKEN_INFO = Erc20Token(ETH_ADDRESS, "ETH", "Ether", 18, "local::ethereum")
    }
}
