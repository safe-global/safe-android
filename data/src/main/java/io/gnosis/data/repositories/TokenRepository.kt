package io.gnosis.data.repositories

import io.gnosis.data.db.daos.Erc20TokenDao
import io.gnosis.data.models.Erc20Token
import pm.gnosis.model.Solidity
import java.math.BigInteger

class TokenRepository(
    private val erc20TokenDao: Erc20TokenDao
) {


    companion object {
        val ETH_ADDRESS = Solidity.Address(BigInteger.ZERO)
        val ETH_TOKEN_INFO = Erc20Token(ETH_ADDRESS, "ETH", "Ether", 18, "local::ethereum")
    }
}
