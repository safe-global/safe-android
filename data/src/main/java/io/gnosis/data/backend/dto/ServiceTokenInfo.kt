package io.gnosis.data.backend.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.models.Erc20Token
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
data class ServiceTokenInfo(
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "decimals") val decimals: Int = 0,
    @Json(name = "symbol") val symbol: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "logoUri") val logoUri: String?,
    @Json(name = "type") val type: TokenType? = null
) {
    fun toErc20Token(): Erc20Token = Erc20Token(
        address,
        name,
        symbol,
        decimals,
        logoUri ?: "https://gnosis-safe-token-logos.s3.amazonaws.com/${address.asEthereumAddressChecksumString()}.png"
    )

    enum class TokenType { ERC20, ERC721 }
}
