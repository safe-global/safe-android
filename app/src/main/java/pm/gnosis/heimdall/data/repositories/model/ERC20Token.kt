package pm.gnosis.heimdall.data.repositories.model

import pm.gnosis.heimdall.data.db.model.ERC20TokenDb
import java.math.BigInteger

data class ERC20Token(val address: BigInteger,
                      val name: String? = null,
                      val symbol: String? = null,
                      val decimals: Int,
                      val verified: Boolean = false)

fun ERC20Token.toDb() = ERC20TokenDb(address, name, symbol, decimals, verified)
fun ERC20TokenDb.fromDb() = ERC20Token(address, name, symbol, decimals, verified)
