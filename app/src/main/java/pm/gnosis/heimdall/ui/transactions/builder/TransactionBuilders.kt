package pm.gnosis.heimdall.ui.transactions.builder

import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.toInt
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexStringToByteArray
import java.math.BigInteger


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


object MultiSendTransactionBuilder {

    private val MULTI_SEND_LIB = BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!

    fun build(data: TransactionData.MultiSend): SafeTransaction =
        SafeTransaction(
            Transaction(
                MULTI_SEND_LIB, data = MultiSend.MultiSend.encode(
                    Solidity.Bytes(
                        data.transactions.joinToString(separator = "") {
                            SolidityBase.encodeFunctionArguments(
                                Solidity.UInt8(it.operation.toInt().toBigInteger()), // Operation
                                it.wrapped.address, // To
                                Solidity.UInt256(it.wrapped.value?.value ?: BigInteger.ZERO), // Value
                                Solidity.Bytes(it.wrapped.data?.hexStringToByteArray() ?: byteArrayOf()) // Data
                            )
                        }.hexStringToByteArray()
                    )
                )
            ), TransactionExecutionRepository.Operation.DELEGATE_CALL
        )
}
