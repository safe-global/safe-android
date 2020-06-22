package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.backend.dto.tokenAsErc20Token
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.models.Balance
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

    companion object {
        private val ETH_ADDRESS = Solidity.Address(BigInteger.ZERO)
        val ETH_TOKEN_INFO = Erc20Token(ETH_ADDRESS, "Ether", "ETH", 18, "local::ethereum")
        val ETH_SERVICE_TOKEN_INFO = ServiceTokenInfo(ETH_ADDRESS, 18, "ETH", "Ether", "local::ethereum")
    }
}
