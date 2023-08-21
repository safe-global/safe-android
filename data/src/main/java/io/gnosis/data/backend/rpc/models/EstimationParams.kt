package io.gnosis.data.backend.rpc.models

import java.math.BigInteger


data class EstimationParams(
    val gasPrice: BigInteger,
    val balance: BigInteger,
    val nonce: BigInteger,
    val callResult: Boolean,
    val estimate: BigInteger
)
