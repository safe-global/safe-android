package io.gnosis.data.models.transaction

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.adapters.SolidityAddressParceler
import io.gnosis.data.models.Chain
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pm.gnosis.model.Solidity
import java.math.BigInteger

enum class TransferType {
    @Json(name = "ERC20") ERC20,
    @Json(name = "ERC721") ERC721,
    @Json(name = "NATIVE_COIN") NATIVE_COIN
}

sealed class TransferInfo(
    @Json(name = "type") val type: TransferType
) : Parcelable{
    @JsonClass(generateAdapter = true)
    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class Erc20Transfer(
        @Json(name = "tokenAddress")
        val tokenAddress: Solidity.Address,
        @Json(name = "tokenName")
        val tokenName: String?,
        @Json(name = "tokenSymbol")
        val tokenSymbol: String?,
        @Json(name = "logoUri")
        val logoUri: String?,
        @Json(name = "decimals")
        val decimals: Int?,
        @Json(name = "value")
        val value: BigInteger
    ) : TransferInfo(TransferType.ERC20)

    @JsonClass(generateAdapter = true)
    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class Erc721Transfer(
        @Json(name = "tokenAddress")
        val tokenAddress: Solidity.Address,
        @Json(name = "tokenId")
        val tokenId: String,
        @Json(name = "tokenName")
        val tokenName: String?,
        @Json(name = "tokenSymbol")
        val tokenSymbol: String?,
        @Json(name = "logoUri")
        val logoUri: String?
    ) : TransferInfo(TransferType.ERC721)

    @JsonClass(generateAdapter = true)
    @Parcelize
    data class NativeTransfer(
        @Json(name = "value")
        val value: BigInteger
    ) : TransferInfo(TransferType.NATIVE_COIN)
}

fun TransferInfo.value(): BigInteger =
    when (this) {
        is TransferInfo.NativeTransfer -> value
        is TransferInfo.Erc20Transfer -> value
        is TransferInfo.Erc721Transfer -> BigInteger.ONE
    }

fun TransferInfo.symbol(chain: Chain): String? =
    when (this) {
        is TransferInfo.NativeTransfer -> chain.currency.symbol
        is TransferInfo.Erc20Transfer -> tokenSymbol
        is TransferInfo.Erc721Transfer -> tokenSymbol
    }

fun TransferInfo.decimals(chain: Chain): Int? =
    when (this) {
        is TransferInfo.Erc20Transfer -> decimals
        is TransferInfo.Erc721Transfer -> 0
        is TransferInfo.NativeTransfer -> chain.currency.decimals
    }
