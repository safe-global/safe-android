package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.model.SolidityBase.PADDED_HEX_LENGTH
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
                transaction.operation == TransactionExecutionRepository.Operation.DELEGATE_CALL ->
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
            val tx = transaction.wrapped
            val data = tx.data?.removeHexPrefix()
            when {
                data.isNullOrBlank() -> // If we have no data we default to ether transfer
                    TransactionData.AssetTransfer(ERC20Token.ETHER_TOKEN.address, tx.value?.value ?: BigInteger.ZERO, tx.address)
                tx.value?.value ?: BigInteger.ZERO == BigInteger.ZERO && data.isSolidityMethod(ERC20Contract.Transfer.METHOD_ID) -> // There should be no ether transfer with the token transfer
                    parseTokenTransfer(tx)
                isMultiSend(transaction) && isReplaceRecoveryPhrase(transaction) -> TransactionData.ReplaceRecoveryPhrase(transaction)
                isMultiSend(transaction) -> parseMultiSend(transaction)
                data.isSolidityMethod(GnosisSafe.AddOwnerWithThreshold.METHOD_ID) -> parseAddOwnerWithThreshold(tx)
                data.isSolidityMethod(GnosisSafe.ChangeMasterCopy.METHOD_ID) -> parseChangeMasterCopy(tx)
                else ->
                    TransactionData.Generic(tx.address, tx.value?.value ?: BigInteger.ZERO, tx.data)
            }
        }
            .subscribeOn(Schedulers.io())

    // TODO: This need to be adjusted for the new MultiSend
    private fun isMultiSend(safeTransaction: SafeTransaction) =
        safeTransaction.operation == TransactionExecutionRepository.Operation.DELEGATE_CALL &&
                safeTransaction.wrapped.address == MULTI_SEND_LIB &&
                safeTransaction.wrapped.data != null &&
                safeTransaction.wrapped.data!!.isSolidityMethod(MultiSend.MultiSend.METHOD_ID)

    // TODO: This need to be adjusted for the new MultiSend
    private fun parseMultiSend(transaction: SafeTransaction): TransactionData.MultiSend {
        val payload =
            transaction.wrapped.data?.removeSolidityMethodPrefix(MultiSend.MultiSend.METHOD_ID) ?: return TransactionData.MultiSend(emptyList())

        val transactions = mutableListOf<SafeTransaction>()
        val partitions = SolidityBase.PartitionData.of(payload)
        nullOnThrow { partitions.consume() } ?: throw IllegalArgumentException("Missing multisend data position")
        nullOnThrow { partitions.consume() } ?: throw IllegalArgumentException("Missing multisend data length")
        var current: String? = nullOnThrow { partitions.consume() }
        while (current != null) {
            val operation = TransactionExecutionRepository.Operation.fromInt(current.hexAsBigInteger().toInt())
            val to = nullOnThrow { partitions.consume().asEthereumAddress() } ?: throw IllegalArgumentException("Illegal to")
            val value = nullOnThrow { Wei(partitions.consume().hexAsBigInteger()) } ?: throw IllegalArgumentException("Illegal value")
            nullOnThrow { partitions.consume().hexAsBigInteger() } ?: throw IllegalArgumentException("Missing data position")
            val data = nullOnThrow { SolidityBase.decodeBytes(partitions) }
            transactions.add(SafeTransaction(Transaction(to, value = value, data = data?.toHex()?.addHexPrefix()), operation))
            current = nullOnThrow { partitions.consume() }
        }

        return TransactionData.MultiSend(transactions)
    }

    private fun isReplaceRecoveryPhrase(transaction: SafeTransaction): Boolean {
        val payload = transaction.wrapped.data?.removeSolidityMethodPrefix(MultiSend.MultiSend.METHOD_ID) ?: return false

        val noPrefix = payload.removeHexPrefix()
        if (noPrefix.length.rem(PADDED_HEX_LENGTH) != 0) throw IllegalArgumentException("Data is not a multiple of $PADDED_HEX_LENGTH")
        val partitions = noPrefix.chunked(PADDED_HEX_LENGTH)
        if (partitions.size != 20) return false
        if (partitions[3] != partitions[12]) return false

        if (!partitions[7].startsWith(GnosisSafe.SwapOwner.METHOD_ID)) return false
        if (!partitions[16].startsWith(GnosisSafe.SwapOwner.METHOD_ID)) return false
        return true
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
            TransactionExecutionRepository.Operation.values()[operation.toInt()]
        )

    companion object {
        private val MULTI_SEND_LIB = BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!
        // These additional costs are hardcoded in the smart contract
        private val SAFE_TX_BASE_COSTS = BigInteger.valueOf(32000)
    }
}
