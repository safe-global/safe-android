package io.gnosis.data.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import io.gnosis.data.models.assets.TokenType

class TokenTypeAdapter {

    @ToJson
    fun toJson(tokenType: TokenType): String = tokenType.name

    @FromJson
    fun fromJson(tokenType: String): TokenType =
        when (tokenType) {
            "NATIVE_TOKEN" -> TokenType.NATIVE_CURRENCY
            "ERC20" -> TokenType.ERC20
            "ERC721" -> TokenType.ERC721
            else -> throw JsonDataException("Unsupported operation value: \"$tokenType\"")
        }
}
