package io.gnosis.data.models

import pm.gnosis.model.Solidity

data class Collectible(
    val id: String,
    val address: Solidity.Address,
    val tokenName: String,
    val tokenSymbol: String,
    val uri: String?,
    val name: String?,
    val description: String?,
    val imageUri: String?,
    val logoUri: String?
)
