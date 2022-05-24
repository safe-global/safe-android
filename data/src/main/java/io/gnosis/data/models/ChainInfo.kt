package io.gnosis.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.common.adapters.moshi.DecimalNumber
import java.math.BigInteger


@JsonClass(generateAdapter = true)
data class ChainInfo(
    @Json(name = "chainId") @field:DecimalNumber val chainId: BigInteger,
    @Json(name = "chainName") val chainName: String,
    @Json(name = "shortName") val shortName: String,
    @Json(name = "ensRegistryAddress") val ensRegistryAddress: String?,
    @Json(name = "rpcUri") val rpcUri: RpcUri,
    @Json(name = "blockExplorerUriTemplate") val blockExplorerTemplate: BlockExplorerTemplate,
    @Json(name = "nativeCurrency") val nativeCurrency: NativeCurrency,
    @Json(name = "transactionService") val transactionService: String,
    @Json(name = "theme") val theme: ChainTheme
) {

    fun toChain(): Chain {
        return Chain(chainId, chainName, shortName, theme.textColor, theme.backgroundColor, rpcUri.value, rpcUri.authentication, blockExplorerTemplate.address, blockExplorerTemplate.txHash, ensRegistryAddress).apply {
            currency = nativeCurrency.toCurrency(chainId)
        }
    }
}

@JsonClass(generateAdapter = true)
data class BlockExplorerTemplate(
    @Json(name = "address") val address: String,
    @Json(name = "txHash") val txHash: String
)

@JsonClass(generateAdapter = true)
data class RpcUri(
    @Json(name = "authentication") val authentication: RpcAuthentication,
    @Json(name = "value") val value: String
)

enum class RpcAuthentication(val value: Int) {
    @Json(name = "API_KEY_PATH")
    API_KEY_PATH(1),

    @Json(name = "NO_AUTHENTICATION")
    NO_AUTHENTICATION(2);

    companion object {
        fun from(value: Int): RpcAuthentication = when (value) {
            1 -> API_KEY_PATH
            else -> NO_AUTHENTICATION
        }
    }
}

@JsonClass(generateAdapter = true)
data class NativeCurrency(
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "logoUri") val logoUri: String
) {

    fun toCurrency(chainId: BigInteger): Chain.Currency {
        return Chain.Currency(chainId, name, symbol, decimals, logoUri)
    }
}

@JsonClass(generateAdapter = true)
data class ChainTheme(
    @Json(name = "textColor") val textColor: String,
    @Json(name = "backgroundColor") val backgroundColor: String
)
