package pm.gnosis.heimdall.data.remote.models.tokens

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
data class VerifiedToken(
    @Json(name = "token") val info: TokenInfoDeprecated,
    @Json(name = "default") val default: Boolean
)


@Deprecated("Use new TokenInfo format")
@JsonClass(generateAdapter = true)
data class TokenInfoDeprecated(
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "logoUrl") val logoUrl: String
)

fun VerifiedToken.fromNetwork() = info.run { ERC20Token(address, name, symbol, decimals, logoUrl) }

@JsonClass(generateAdapter = true)
data class TokenInfo(
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "logoUri") val logoUri: String
)
fun TokenInfo.fromNetwork() = ERC20Token(address, name, symbol, decimals, logoUri)
