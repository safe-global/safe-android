package io.gnosis.data.models.assets

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport
import pm.gnosis.common.adapters.moshi.BigDecimalNumber
import pm.gnosis.common.adapters.moshi.DecimalNumber
import pm.gnosis.model.Solidity
import java.math.BigDecimal
import java.math.BigInteger

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class CoinBalances(
    @field:BigDecimalNumber val fiatTotal: BigDecimal,
    val items: List<Balance>
)

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class Balance(
    @Json(name = "tokenInfo") val tokenInfo: TokenInfo,
    @Json(name = "balance") @field:DecimalNumber val balance: BigInteger,
    @Json(name = "fiatBalance") @field:BigDecimalNumber val fiatBalance: BigDecimal
)

@ExcludeClassFromJacocoGeneratedReport
@JsonClass(generateAdapter = true)
data class TokenInfo(
    @Json(name = "type") val tokenType: TokenType,
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "decimals") val decimals: Int = 0,
    @Json(name = "symbol") val symbol: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "logoUri") val logoUri: String?
)

enum class TokenType { NATIVE_CURRENCY, ERC20, ERC721 }
