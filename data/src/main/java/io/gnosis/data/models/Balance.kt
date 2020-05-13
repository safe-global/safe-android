package io.gnosis.data.models

import java.math.BigDecimal
import java.math.BigInteger

data class Balance(
    val token: Erc20Token,
    val balance: BigInteger,
    val balanceUsd: BigDecimal
)
