package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import pm.gnosis.heimdall.data.repositories.RestrictedTransactionException
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.Operation
import pm.gnosis.heimdall.data.repositories.TransactionInfo
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultTransactionInfoRepository @Inject constructor(
    appDb: ApplicationDb
) : TransactionInfoRepository {

    private val descriptionsDao = appDb.descriptionsDao()

    override fun checkRestrictedTransaction(safe: Solidity.Address, transaction: SafeTransaction): Single<SafeTransaction> =
        Single.fromCallable {
            @Suppress("ConstantConditionIf")
            if (BuildConfig.ALLOW_RESTRICTED_TX) return@fromCallable transaction
            when {
                transaction.operation == Operation.DELEGATE_CALL ->
                    throw RestrictedTransactionException.DelegateCall
                checkIsMethodCallOnSafe(safe, transaction, GnosisSafe.AddOwnerWithThreshold.METHOD_ID) ->
                    throw RestrictedTransactionException.ModifyOwners
                checkIsMethodCallOnSafe(safe, transaction, GnosisSafe.RemoveOwner.METHOD_ID) ->
                    throw RestrictedTransactionException.ModifyOwners
                checkIsMethodCallOnSafe(safe, transaction, GnosisSafe.SwapOwner.METHOD_ID) ->
                    throw RestrictedTransactionException.ModifyOwners
                checkIsMethodCallOnSafe(safe, transaction, GnosisSafe.EnableModule.METHOD_ID) ->
                    throw RestrictedTransactionException.ModifyModules
                checkIsMethodCallOnSafe(safe, transaction, GnosisSafe.DisableModule.METHOD_ID) ->
                    throw RestrictedTransactionException.ModifyModules
                checkIsMethodCallOnSafe(safe, transaction, GnosisSafe.ChangeThreshold.METHOD_ID) ->
                    throw RestrictedTransactionException.ChangeThreshold
                checkIsMethodCallOnSafe(safe, transaction, GnosisSafe.ChangeMasterCopy.METHOD_ID) ->
                    throw RestrictedTransactionException.ChangeMasterCopy
                checkIsMethodCallOnSafe(safe, transaction, GnosisSafe.SetFallbackHandler.METHOD_ID) ->
                    throw RestrictedTransactionException.SetFallbackHandler
                transaction.wrapped.address == safe && !transaction.wrapped.data?.removeHexPrefix().isNullOrEmpty() ->
                    throw RestrictedTransactionException.DataCallToSafe
            }
            transaction
        }
            .subscribeOn(Schedulers.io())

    private fun checkIsMethodCallOnSafe(safe: Solidity.Address, transaction: SafeTransaction, methodId: String) =
        transaction.wrapped.address == safe && transaction.wrapped.data?.isSolidityMethod(methodId) == true

    override fun parseTransactionData(transaction: SafeTransaction): Single<TransactionData> =
        Single.fromCallable {
            when {
                transaction.isCall() -> transaction.wrapped.parseCall()
                isMultiSend(transaction) -> parseMultiSend(transaction)
                else -> transaction.toGenericTransactionData()
            }
        }
            .subscribeOn(Schedulers.io())

    private fun SafeTransaction.toGenericTransactionData() =
        wrapped.let { tx ->
            TransactionData.Generic(tx.address, tx.value?.value ?: BigInteger.ZERO, tx.data, operation)
        }

    private fun SafeTransaction.isCall() = this.operation == Operation.CALL

    private fun Transaction.parseCall() =
        when {
            data?.removeHexPrefix().isNullOrBlank() -> // If we have no data we default to ether transfer
                TransactionData.AssetTransfer(ERC20Token.ETHER_TOKEN.address, value?.value ?: BigInteger.ZERO, address)
            value?.value ?: BigInteger.ZERO == BigInteger.ZERO && data!!.isSolidityMethod(ERC20Contract.Transfer.METHOD_ID) -> // There should be no ether transfer with the token transfer
                parseTokenTransfer(this)
            data!!.isSolidityMethod(GnosisSafe.AddOwnerWithThreshold.METHOD_ID) -> parseAddOwnerWithThreshold(this)
            data!!.isSolidityMethod(GnosisSafe.ChangeMasterCopy.METHOD_ID) -> parseChangeMasterCopy(this)
            else ->
                TransactionData.Generic(address, value?.value ?: BigInteger.ZERO, data, Operation.CALL)
        }

    private fun isMultiSend(safeTransaction: SafeTransaction) =
        safeTransaction.operation == Operation.DELEGATE_CALL &&
                safeTransaction.wrapped.data != null &&
                safeTransaction.wrapped.data!!.isSolidityMethod(MultiSend.MultiSend.METHOD_ID)

    private fun parseMultiSend(transaction: SafeTransaction): TransactionData {
        val payload =
            transaction.wrapped.data?.removeSolidityMethodPrefix(MultiSend.MultiSend.METHOD_ID)
                ?: return TransactionData.MultiSend(emptyList(), transaction.wrapped.address)

        val multiSend = when (transaction.wrapped.address) {
            MULTI_SEND_LIB -> nullOnThrow { parseMultiSendNew(payload) }
            MULTI_SEND_OLD_LIB -> nullOnThrow { parseMultiSendOld(payload) }
            else -> null
        } ?: return transaction.toGenericTransactionData()
        return processMultiSend(transaction, multiSend)
    }

    private fun parseMultiSendNew(payload: String): TransactionData.MultiSend {
        val transactions = mutableListOf<SafeTransaction>()
        val data = MultiSend.MultiSend.decodeArguments(payload).transactions.items.toHexString()
        val reader = PayloadReader(data)
        while (reader.hasAdditional(85)) {
            val operation = Operation.fromInt(reader.readAsHexInt(1))
            val to = nullOnThrow { reader.read(20).asEthereumAddress() } ?: throw IllegalArgumentException("Illegal to")
            val value = nullOnThrow { Wei(reader.readAsHexBigInteger(32)) } ?: throw IllegalArgumentException("Illegal value")
            val dataSize = nullOnThrow { reader.readAsHexBigInteger(32) } ?: throw IllegalArgumentException("Missing data size")
            val data = nullOnThrow { reader.read(dataSize.toInt()).hexToByteArray() }
            transactions.add(SafeTransaction(Transaction(to, value = value, data = data?.toHex()?.addHexPrefix()), operation))
        }

        return TransactionData.MultiSend(transactions, MULTI_SEND_LIB)
    }

    private fun parseMultiSendOld(payload: String): TransactionData.MultiSend {
        val transactions = mutableListOf<SafeTransaction>()
        val partitions = SolidityBase.PartitionData.of(payload)
        nullOnThrow { partitions.consume() } ?: throw IllegalArgumentException("Missing multisend data position")
        nullOnThrow { partitions.consume() } ?: throw IllegalArgumentException("Missing multisend data length")
        var current: String? = nullOnThrow { partitions.consume() }
        while (current != null) {
            val operation = Operation.fromInt(current.hexAsBigInteger().toInt())
            val to = nullOnThrow { partitions.consume().asEthereumAddress() } ?: throw IllegalArgumentException("Illegal to")
            val value = nullOnThrow { Wei(partitions.consume().hexAsBigInteger()) } ?: throw IllegalArgumentException("Illegal value")
            nullOnThrow { partitions.consume().hexAsBigInteger() } ?: throw IllegalArgumentException("Missing data position")
            val data = nullOnThrow { SolidityBase.decodeBytes(partitions) }
            transactions.add(SafeTransaction(Transaction(to, value = value, data = data?.toHex()?.addHexPrefix()), operation))
            current = nullOnThrow { partitions.consume() }
        }

        return TransactionData.MultiSend(transactions, MULTI_SEND_OLD_LIB)
    }

    private fun processMultiSend(transaction: SafeTransaction, multiSend: TransactionData.MultiSend) =
        parseUpgradeV111(multiSend)
            ?: parseReplaceRecoveryPhrase(transaction, multiSend)
            ?: multiSend

    private fun parseUpgradeV111(multiSend: TransactionData.MultiSend): TransactionData? {
        if (multiSend.transactions.size != 2) return null

        // Needs to be a valid owner swap tx
        val changeMasterCopy = multiSend.transactions[0]
        if (changeMasterCopy.operation != Operation.CALL || changeMasterCopy.wrapped.data?.isSolidityMethod(GnosisSafe.ChangeMasterCopy.METHOD_ID) != true)
            return null

        val masterCopy =
            GnosisSafe.ChangeMasterCopy.decodeArguments(changeMasterCopy.wrapped.data!!.removeSolidityMethodPrefix(GnosisSafe.ChangeMasterCopy.METHOD_ID))

        // Check correct master copy
        if (masterCopy._mastercopy != SafeContractUtils.safeMasterCopy_1_1_1) return null

        // Needs to be a valid owner swap tx
        val setFallbackHandler = multiSend.transactions[1]
        if (setFallbackHandler.operation != Operation.CALL || setFallbackHandler.wrapped.data?.isSolidityMethod(GnosisSafe.SetFallbackHandler.METHOD_ID) != true)
            return null

        val fallbackHandler =
            GnosisSafe.SetFallbackHandler.decodeArguments(setFallbackHandler.wrapped.data!!.removeSolidityMethodPrefix(GnosisSafe.SetFallbackHandler.METHOD_ID))

        // Check correct master copy
        if (fallbackHandler.handler != DEFAULT_FALLBACK_HANDLER) return null

        // We need to swap owners at the same Safe
        if (changeMasterCopy.wrapped.address != setFallbackHandler.wrapped.address) return null
        return TransactionData.UpdateMasterCopy(SafeContractUtils.safeMasterCopy_1_1_1)
    }

    private fun parseReplaceRecoveryPhrase(transaction: SafeTransaction, multiSend: TransactionData.MultiSend): TransactionData? {
        if (multiSend.transactions.size != 2) return null

        // Needs to be a valid owner swap tx
        val firstOwnerSwap = multiSend.transactions[0]
        if (firstOwnerSwap.operation != Operation.CALL || firstOwnerSwap.wrapped.data?.isSolidityMethod(GnosisSafe.SwapOwner.METHOD_ID) != true)
            return null

        // Needs to be a valid owner swap tx
        val secondOwnerSwap = multiSend.transactions[1]
        if (secondOwnerSwap.operation != Operation.CALL || secondOwnerSwap.wrapped.data?.isSolidityMethod(GnosisSafe.SwapOwner.METHOD_ID) != true)
            return null

        // We need to swap owners at the same Safe
        if (firstOwnerSwap.wrapped.address != secondOwnerSwap.wrapped.address) return null
        return TransactionData.ReplaceRecoveryPhrase(transaction)
    }

    private fun parseTokenTransfer(transaction: Transaction): TransactionData.AssetTransfer {
        val arguments = transaction.data!!.removeSolidityMethodPrefix(ERC20Contract.Transfer.METHOD_ID)
        return ERC20Contract.Transfer.decodeArguments(arguments).let { TransactionData.AssetTransfer(transaction.address, it._value.value, it._to) }
    }

    private fun parseChangeMasterCopy(transaction: Transaction): TransactionData.UpdateMasterCopy {
        val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.ChangeMasterCopy.METHOD_ID)
        return GnosisSafe.ChangeMasterCopy.decodeArguments(arguments).let { TransactionData.UpdateMasterCopy(it._mastercopy) }
    }

    private fun parseAddOwnerWithThreshold(transaction: Transaction): TransactionData.ConnectAuthenticator {
        val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.AddOwnerWithThreshold.METHOD_ID)
        return GnosisSafe.AddOwnerWithThreshold.decodeArguments(arguments).let { TransactionData.ConnectAuthenticator(it.owner) }
    }

    override fun loadTransactionInfo(id: String): Single<TransactionInfo> =
        descriptionsDao.loadDescription(id)
            .subscribeOn(Schedulers.io())
            .flatMap { info ->
                descriptionsDao.loadStatus(id).map { info to it }
            }
            .flatMap { (info, status) ->
                val transaction = info.toTransaction()
                parseTransactionData(transaction)
                    .map {
                        val gasLimit = info.dataGas + info.txGas + SAFE_TX_BASE_COSTS
                        TransactionInfo(id, status.transactionId, info.safeAddress, it, info.submittedAt, gasLimit, info.gasPrice, info.gasToken)
                    }
            }

    private fun TransactionDescriptionDb.toTransaction(): SafeTransaction =
        SafeTransaction(
            Transaction(to, value = Wei(value), data = data, nonce = nonce),
            Operation.values()[operation.toInt()]
        )

    private class PayloadReader(private val payload: String) {
        private var index = 0

        fun read(bytes: Int) = payload.substring(index, index + bytes * 2).apply {
            index += bytes * 2
        }

        fun readAsHexBigInteger(bytes: Int) = read(bytes).hexAsBigInteger()

        fun readAsHexInt(bytes: Int) = read(bytes).toInt(16)

        fun hasAdditional(bytes: Int) = (index + bytes * 2) <= payload.length
    }

    companion object {
        private val MULTI_SEND_LIB = BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!
        private val MULTI_SEND_OLD_LIB = BuildConfig.MULTI_SEND_OLD_ADDRESS.asEthereumAddress()!!
        private val DEFAULT_FALLBACK_HANDLER = BuildConfig.DEFAULT_FALLBACK_HANDLER.asEthereumAddress()!!
        // These additional costs are hardcoded in the smart contract
        private val SAFE_TX_BASE_COSTS = BigInteger.valueOf(32000)
    }
}
