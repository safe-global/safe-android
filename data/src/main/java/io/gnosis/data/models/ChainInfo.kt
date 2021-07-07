package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.contracts.BuildConfig.ENS_REGISTRY


@JsonClass(generateAdapter = true)
data class ChainInfo(
    @Json(name = "chainId") val chainId: Int,
    @Json(name = "chainName") val chainName: String,
    @Json(name = "ensRegistryAddress") val ensRegistryAddress: String? = ENS_REGISTRY,
    @Json(name = "rpcUrl") val rpcUrl: String,
    @Json(name = "blockExplorerUrl") val blockExplorerUrl: String,
    @Json(name = "nativeCurrency") val nativeCurrency: NativeCurrency,
    @Json(name = "transactionService") val transactionService: String,
    @Json(name = "theme") val theme: ChainTheme

)

@JsonClass(generateAdapter = true)
data class NativeCurrency(
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int
)

@JsonClass(generateAdapter = true)
data class ChainTheme(
    @Json(name = "textColor") val textColor: String,
    @Json(name = "backgroundColor") val backgroundColor: String
)
