package pm.gnosis.heimdall.ui.transactions.builder

import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei

interface TransactionBuilder<in T : TransactionData> {
    fun build(data: T): SafeTransaction
}

object GenericTransactionBuilder : TransactionBuilder<TransactionData.Generic> {
    override fun build(data: TransactionData.Generic) =
        SafeTransaction(
            Transaction(
                data.to,
                value = Wei(data.value),
                data = data.data
            ), TransactionExecutionRepository.Operation.CALL
        )
}

object AssetTransferTransactionBuilder : TransactionBuilder<TransactionData.AssetTransfer> {
    override fun build(data: TransactionData.AssetTransfer) =
        if (data.token == ERC20Token.ETHER_TOKEN.address)
            SafeTransaction(
                Transaction(
                    data.receiver,
                    value = Wei(data.amount)
                ), TransactionExecutionRepository.Operation.CALL
            )
        else
            SafeTransaction(
                Transaction(
                    data.token,
                    data = StandardToken.Transfer.encode(data.receiver, Solidity.UInt256(data.amount))
                ), TransactionExecutionRepository.Operation.CALL
            )
}
