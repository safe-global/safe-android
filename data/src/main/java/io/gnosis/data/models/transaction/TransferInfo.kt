package io.gnosis.data.models.transaction

import io.gnosis.data.models.assets.TokenInfo
import io.gnosis.data.repositories.TokenRepository
import pm.gnosis.model.Solidity
import java.math.BigInteger

enum class TransferType {
    ERC20, ERC721, ETHER
}

sealed class TransferInfo(val type: TransferType) {

    data class Erc20Transfer(
        val tokenAddress: Solidity.Address,
        val tokenName: String?,
        val tokenSymbol: String?,
        val logoUri: String?,
        val decimals: Int?,
        val value: BigInteger
    ) : TransferInfo(TransferType.ERC20)

    data class Erc721Transfer(
        val tokenAddress: Solidity.Address,
        val tokenId: String,
        val tokenName: String?,
        val tokenSymbol: String?,
        val logoUri: String?
    ) : TransferInfo(TransferType.ERC721)

    data class EtherTransfer(
        val value: BigInteger
    ) : TransferInfo(TransferType.ETHER)
}


fun TransferInfo.value(): BigInteger =
    when (this) {
        is TransferInfo.EtherTransfer -> value
        is TransferInfo.Erc20Transfer -> value
        is TransferInfo.Erc721Transfer -> BigInteger.ONE
    }

fun TransferInfo.symbol(): String? =
    when (this) {
        is TransferInfo.EtherTransfer -> TokenRepository.ETH_TOKEN_INFO.symbol
        is TransferInfo.Erc20Transfer -> tokenSymbol
        is TransferInfo.Erc721Transfer -> tokenSymbol
    }

fun TransferInfo.decimals(): Int? =
    when (this) {
        is TransferInfo.Erc20Transfer -> decimals
        is TransferInfo.Erc721Transfer -> 0
        is TransferInfo.EtherTransfer -> TokenRepository.ETH_TOKEN_INFO.decimals
    }
