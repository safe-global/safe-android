package pm.gnosis.heimdall.data.remote.models.tokens

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
data class VerifiedTokenResult(
    @Json(name = "results") val results: List<VerifiedToken>
)

@JsonClass(generateAdapter = true)
data class VerifiedToken(
    @Json(name = "token") val info: TokenInfo,
    @Json(name = "default") val default: Boolean
)


@JsonClass(generateAdapter = true)
data class TokenInfo(
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "logoUrl") val logoUrl: String
)

fun VerifiedToken.fromNetwork() = info?.run { ERC20Token(address, name, symbol, decimals, logoUrl) }
