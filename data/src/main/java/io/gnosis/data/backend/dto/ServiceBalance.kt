package io.gnosis.data.backend.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.models.Erc20Token
import io.gnosis.data.repositories.TokenRepository.Companion.ETH_TOKEN_INFO
import pm.gnosis.common.adapters.moshi.BigDecimalNumber
import pm.gnosis.common.adapters.moshi.DecimalNumber
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import java.math.BigDecimal
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class ServiceBalance(
    @Json(name = "tokenAddress") val tokenAddress: Solidity.Address?,
    @Json(name = "token") val token: ServiceTokenMeta?,
    @Json(name = "balance") @field:DecimalNumber val balance: BigInteger,
    @Json(name = "balanceUsd") @field:BigDecimalNumber val balanceUsd: BigDecimal
) {
    @JsonClass(generateAdapter = true)
    data class ServiceTokenMeta(
        @Json(name = "decimals") val decimals: Int,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "name") val name: String,
        @Json(name = "logoUri") val logoUri: String?
    )
}

fun ServiceBalance.tokenAsErc20Token(): Erc20Token =
    if (tokenAddress == null) ETH_TOKEN_INFO
    else Erc20Token(
        tokenAddress,
        token?.name.orEmpty(),
        token?.symbol.orEmpty(),
        token?.decimals ?: 0,
        token?.logoUri ?: "https://gnosis-safe-token-logos.s3.amazonaws.com/${tokenAddress.asEthereumAddressChecksumString()}.png"
    )
