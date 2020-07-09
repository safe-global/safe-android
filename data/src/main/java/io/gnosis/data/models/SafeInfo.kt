package io.gnosis.data.models

import pm.gnosis.model.Solidity
import java.math.BigInteger

data class SafeInfo(
    val address: Solidity.Address,
    val nonce: BigInteger,
    val threshold: Int,
    val owners: List<Solidity.Address>,
    val masterCopy: Solidity.Address
)
