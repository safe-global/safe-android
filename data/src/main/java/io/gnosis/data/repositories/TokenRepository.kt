package io.gnosis.data.repositories

import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.backend.dto.ServiceTokenInfo
import io.gnosis.data.backend.dto.tokenAsErc20Token
import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.models.Balance
import io.gnosis.data.models.Erc20Token
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
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
        private val defaultErc20Address = "0xc778417e063141139fce010982780140aa0cd5ab".asEthereumAddress()!!
        private val defaultErc721Address = "0xB3775fB83F7D12A36E0475aBdD1FCA35c091efBe".asEthereumAddress()!!
        private val ETH_ADDRESS = Solidity.Address(BigInteger.ZERO)
        val ETH_TOKEN_INFO = Erc20Token(ETH_ADDRESS, "Ether", "ETH", 18, "local::ethereum")
        val ETH_SERVICE_TOKEN_INFO = ServiceTokenInfo(ETH_ADDRESS, 18, "ETH", "Ether", "local::ethereum")
        val FAKE_ERC20_TOKEN_INFO = ServiceTokenInfo(defaultErc20Address, 18, "WETH", "Wrapped Ether", "local::ethereum")
        val FAKE_ERC721_TOKEN_INFO = ServiceTokenInfo(defaultErc721Address, 18, "DRK", "Dirk", "local::ethereum")
    }
}
