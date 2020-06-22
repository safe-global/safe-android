package io.gnosis.data.models

import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ServiceTokenInfo
import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class Transaction {
    data class Custom(
        val nonce: BigInteger?,
        val address: Solidity.Address,
        val dataSize: Long,
        val date: String?,
        val value: BigInteger
    ) : Transaction()

    data class SettingsChange(
        val dataDecoded: DataDecodedDto,
        val date: String?,
        val nonce: BigInteger
    ) : Transaction()

    data class Transfer(
        val recipient: Solidity.Address,
        val sender: Solidity.Address,
        val value: BigInteger,
        val date: String?,
        val tokenInfo: ServiceTokenInfo
    ) : Transaction()
}

