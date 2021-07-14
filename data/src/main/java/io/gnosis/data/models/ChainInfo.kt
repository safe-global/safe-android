package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.common.adapters.moshi.DecimalNumber
import java.math.BigInteger


@JsonClass(generateAdapter = true)
data class ChainInfo(
    @Json(name = "chainId") @field:DecimalNumber val chainId: BigInteger,
    @Json(name = "chainName") val chainName: String,
    @Json(name = "ensRegistryAddress") val ensRegistryAddress: String?,
    @Json(name = "rpcUrl") val rpcUrl: String,
    @Json(name = "blockExplorerUrl") val blockExplorerUrl: String,
    @Json(name = "nativeCurrency") val nativeCurrency: NativeCurrency,
    @Json(name = "transactionService") val transactionService: String,
    @Json(name = "theme") val theme: ChainTheme
) {

    fun toChain(): Chain {
        return Chain(chainId, chainName, theme.textColor, theme.backgroundColor, rpcUrl, blockExplorerUrl, ensRegistryAddress).apply {
            currency = nativeCurrency.toCurrency(chainId)
        }
    }
}

@JsonClass(generateAdapter = true)
data class NativeCurrency(
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "logoUrl") val logoUrl: String
) {

    fun toCurrency(chainId: BigInteger): Chain.Currency {
        return Chain.Currency(chainId, name, symbol, decimals, logoUrl)
    }
}

@JsonClass(generateAdapter = true)
data class ChainTheme(
    @Json(name = "textColor") val textColor: String,
    @Json(name = "backgroundColor") val backgroundColor: String
)
