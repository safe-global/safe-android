package io.gnosis.data.backend.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.common.adapters.moshi.BigDecimalNumber
import pm.gnosis.common.adapters.moshi.DecimalNumber
import pm.gnosis.model.Solidity
import java.math.BigDecimal
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class CoinBalancesDto(
    @field:BigDecimalNumber val fiatTotal: BigDecimal,
    val items: List<BalanceDto>
)

@JsonClass(generateAdapter = true)
data class BalanceDto(
    @Json(name = "tokenInfo") val tokenInfo: TokenInfoDto,
    @Json(name = "balance") @field:DecimalNumber val balance: BigInteger,
    @Json(name = "fiatBalance") @field:BigDecimalNumber val fiatBalance: BigDecimal
)

@JsonClass(generateAdapter = true)
data class TokenInfoDto(
    @Json(name = "type") val tokenType: String,
    @Json(name = "address") val address: Solidity.Address?,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String,
    @Json(name = "logoUri") val logoUri: String?
)
