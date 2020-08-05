package io.gnosis.safe.notifications.models

import pm.gnosis.model.Solidity
import java.math.BigInteger

sealed class Notification {

    data class TxProcessed(
        val msg: String,
        val failed: Boolean,
        val safe: Solidity.Address,
        val safeTxHash: String
    )

    data class IncomingTransfer(
        val msg: String,
        val safe: Solidity.Address,
        val amount: BigInteger
    )

    data class IncomingERC20Transfer(
        val msg: String,
        val safe: Solidity.Address
    )

    data class IncomingERC721Transfer(
        val msg: String,
        val safe: Solidity.Address
    )
}
