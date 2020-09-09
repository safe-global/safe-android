package io.gnosis.data.backend.dto

import io.gnosis.data.models.Collectible
import pm.gnosis.model.Solidity
import java.util.*

data class CollectibleDto(

    // collectible id
    val id: String,

    // NFT token contract address
    val address: Solidity.Address,

    // NFT token name (label of the section in the list)
    val tokenName: String,

    val tokenSymbol: String,

    val uri: String?,

    // name of the collectible
    val name: String?,

    // description of the collectible
    val description: String?,

    // image of the collectible
    val imageUri: String?,

    // image of the contract
    val logoUri: String?
) {
    fun toCollectible(): Collectible {
        return Collectible(
            id,
            address,
            tokenName,
            tokenSymbol,
            uri,
            name,
            description,
            imageUri,
            logoUri
        )
    }
}
