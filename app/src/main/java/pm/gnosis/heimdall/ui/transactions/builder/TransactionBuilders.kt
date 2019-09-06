package pm.gnosis.heimdall.ui.transactions.builder

import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei


object GenericTransactionBuilder {
    fun build(data: TransactionData.Generic) =
        SafeTransaction(
            Transaction(
                data.to,
                value = Wei(data.value),
                data = data.data
            ), TransactionExecutionRepository.Operation.CALL
        )
}

object AssetTransferTransactionBuilder {
    fun build(data: TransactionData.AssetTransfer) =
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
                    data = ERC20Contract.Transfer.encode(data.receiver, Solidity.UInt256(data.amount))
                ), TransactionExecutionRepository.Operation.CALL
            )
}


object UpdateMasterCopyTransactionBuilder {
    fun build(safe: Solidity.Address, data: TransactionData.UpdateMasterCopy): SafeTransaction =
        SafeTransaction(
            Transaction(
                safe,
                data = GnosisSafe.ChangeMasterCopy.encode(data.masterCopy)
            ), TransactionExecutionRepository.Operation.CALL
        )
}
