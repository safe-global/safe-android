package pm.gnosis.heimdall.data.repositories.model

import pm.gnosis.heimdall.data.db.model.ERC20TokenDb
import java.math.BigInteger

data class ERC20Token(val address: String,
                      val name: String? = null,
                      val symbol: String? = null,
                      val decimals: BigInteger? = null,
                      val verified: Boolean = false)

fun ERC20Token.toDb() = ERC20TokenDb(address, name, verified)
fun ERC20TokenDb.fromDb() = ERC20Token(address = address, name = name, verified = verified)

