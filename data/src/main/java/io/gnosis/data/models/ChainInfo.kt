package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.common.adapters.moshi.DecimalNumber
import java.math.BigInteger
import io.gnosis.contracts.BuildConfig.ENS_REGISTRY


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
