package pm.gnosis.heimdall.data.remote.models.tokens

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
data class VerifiedTokenJson(
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "logoUrl") val logoUrl: String
)

fun VerifiedTokenJson.fromNetwork() = ERC20Token(address, name, symbol, decimals, logoUrl)
