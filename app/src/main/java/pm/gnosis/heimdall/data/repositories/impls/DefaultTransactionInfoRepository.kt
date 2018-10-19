package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.removeHexPrefix
import pm.gnosis.utils.removeSolidityMethodPrefix
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DefaultTransactionInfoRepository @Inject constructor(
    appDb: ApplicationDb
) : TransactionInfoRepository {

    private val descriptionsDao = appDb.descriptionsDao()

    override fun checkRestrictedTransaction(transaction: SafeTransaction): Single<SafeTransaction> =
        Single.fromCallable {
            when {
                transaction.operation == TransactionExecutionRepository.Operation.DELEGATE_CALL ->
                    throw RestrictedTransactionException.DelegateCall
                transaction.wrapped.data?.isSolidityMethod(GnosisSafe.AddOwnerWithThreshold.METHOD_ID) == true ->
                    throw RestrictedTransactionException.ModifyOwners
                transaction.wrapped.data?.isSolidityMethod(GnosisSafe.RemoveOwner.METHOD_ID) == true ->
                    throw RestrictedTransactionException.ModifyOwners
                transaction.wrapped.data?.isSolidityMethod(GnosisSafe.SwapOwner.METHOD_ID) == true ->
                    throw RestrictedTransactionException.ModifyOwners
                transaction.wrapped.data?.isSolidityMethod(GnosisSafe.EnableModule.METHOD_ID) == true ->
                    throw RestrictedTransactionException.ModifyModules
                transaction.wrapped.data?.isSolidityMethod(GnosisSafe.DisableModule.METHOD_ID) == true ->
                    throw RestrictedTransactionException.ModifyModules
                transaction.wrapped.data?.isSolidityMethod(GnosisSafe.ChangeThreshold.METHOD_ID) == true ->
                    throw RestrictedTransactionException.ChangeThreshold
                transaction.wrapped.data?.isSolidityMethod(GnosisSafe.ChangeMasterCopy.METHOD_ID) == true ->
                    throw RestrictedTransactionException.ChangeMasterCopy
            }
            transaction
        }
            .subscribeOn(Schedulers.io())

    override fun parseTransactionData(transaction: SafeTransaction): Single<TransactionData> =
        Single.fromCallable {
            val tx = transaction.wrapped
            val data = tx.data?.removeHexPrefix()
            when {
                data.isNullOrBlank() -> // If we have no data we default to ether transfer
                    TransactionData.AssetTransfer(ERC20Token.ETHER_TOKEN.address, tx.value?.value ?: BigInteger.ZERO, tx.address)
                tx.value?.value ?: BigInteger.ZERO == BigInteger.ZERO && data?.isSolidityMethod(ERC20Contract.Transfer.METHOD_ID) == true -> // There should be no ether transfer with the token transfer
                    parseTokenTransfer(tx)
                isMultiSend(transaction) && isReplaceRecoveryPhrase(transaction) -> TransactionData.ReplaceRecoveryPhrase(transaction)
                data!!.isSolidityMethod(GnosisSafe.AddOwnerWithThreshold.METHOD_ID) -> parseAddOwnerWithThreshold(tx)
                else ->
                    TransactionData.Generic(tx.address, tx.value?.value ?: BigInteger.ZERO, tx.data)
            }
        }
            .subscribeOn(Schedulers.io())

    private fun isMultiSend(safeTransaction: SafeTransaction) =
        safeTransaction.operation == TransactionExecutionRepository.Operation.DELEGATE_CALL &&
                safeTransaction.wrapped.data != null &&
                safeTransaction.wrapped.data!!.isSolidityMethod(MultiSend.MultiSend.METHOD_ID)

    private fun isReplaceRecoveryPhrase(transaction: SafeTransaction): Boolean {
        val payload = transaction.wrapped.data?.removeSolidityMethodPrefix(MultiSend.MultiSend.METHOD_ID) ?: return false
        val partitions = SolidityBase.partitionData(payload)
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

    private fun parseAddOwnerWithThreshold(transaction: Transaction): TransactionData.ConnectExtension {
        val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.AddOwnerWithThreshold.METHOD_ID)
        return GnosisSafe.AddOwnerWithThreshold.decodeArguments(arguments).let { TransactionData.ConnectExtension(it.owner) }
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
        // These additional costs are hardcoded in the smart contract
        private val SAFE_TX_BASE_COSTS = BigInteger.valueOf(32000)
    }
}
