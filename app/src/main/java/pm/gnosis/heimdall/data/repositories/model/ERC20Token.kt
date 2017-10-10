package pm.gnosis.heimdall.data.repositories.model

import pm.gnosis.heimdall.data.db.ERC20TokenDb
import java.math.BigInteger

data class ERC20Token(val address: String,
                      val name: String? = null,
                      val symbol: String? = null,
                      val decimals: BigInteger? = null,
                      val verified: Boolean? = null)

fun ERC20Token.toDb(): ERC20TokenDb {
    val dbToken = ERC20TokenDb()
    dbToken.address = address
    dbToken.name = name
    dbToken.verified = verified
    return dbToken
}

fun ERC20TokenDb.fromDb() = address?.let { ERC20Token(address = it, name = name, verified = verified) }
