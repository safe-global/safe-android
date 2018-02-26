package pm.gnosis.heimdall.data.repositories.impls

import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.models.Transaction
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.removeSolidityMethodPrefix
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleTransactionDetailsRepository @Inject constructor(
    appDb: ApplicationDb
) : TransactionDetailsRepository {

    private val descriptionsDao = appDb.descriptionsDao()

    override fun loadTransactionData(transaction: Transaction): Single<Optional<TransactionTypeData>> {
        return Single.fromCallable {
            val type = parseTransactionType(transaction.value?.value, transaction.data)
            decodeTransactionData(type, transaction).toOptional()
        }.subscribeOn(Schedulers.computation())
    }

    override fun loadTransactionType(transaction: Transaction): Single<TransactionType> =
        Single.fromCallable {
            parseTransactionType(transaction.value?.value, transaction.data)
        }.subscribeOn(Schedulers.computation())

    override fun loadTransactionDetails(id: String): Single<TransactionDetails> {
        return descriptionsDao.loadDescription(id)
            .subscribeOn(Schedulers.io())
            .map { decodeDescription(id, it.fromDb()) }
    }

    private fun parseTransactionType(value: BigInteger?, data: String?): TransactionType =
        when {
            data.isNullOrBlank() -> TransactionType.ETHER_TRANSFER // If we have no data we default to ether transfer
            data?.isSolidityMethod(StandardToken.Transfer.METHOD_ID) == true -> TransactionType.TOKEN_TRANSFER
            data?.isSolidityMethod(GnosisSafe.AddOwner.METHOD_ID) == true -> TransactionType.ADD_SAFE_OWNER
            data?.isSolidityMethod(GnosisSafe.RemoveOwner.METHOD_ID) == true -> TransactionType.REMOVE_SAFE_OWNER
            data?.isSolidityMethod(GnosisSafe.ReplaceOwner.METHOD_ID) == true -> TransactionType.REPLACE_SAFE_OWNER
            else -> TransactionType.GENERIC
        }

    private fun decodeDescription(transactionId: String, description: GnosisSafeTransactionDescription) =
        decodeTransactionResult(transactionId, description.safeAddress, description.toTransaction(), description.submittedAt, description.subject)

    private fun decodeTransactionResult(
        transactionId: String,
        safe: BigInteger,
        transaction: Transaction,
        submittedAt: Long,
        subject: String? = null
    ): TransactionDetails {
        val type = parseTransactionType(transaction.value?.value, transaction.data)
        return TransactionDetails(transactionId, type, decodeTransactionData(type, transaction), transaction, safe, submittedAt, subject)
    }

    private fun decodeTransactionData(type: TransactionType, transaction: Transaction): TransactionTypeData? =
        when (type) {
            TransactionType.TOKEN_TRANSFER -> {
                val arguments = transaction.data!!.removeSolidityMethodPrefix(StandardToken.Transfer.METHOD_ID)
                StandardToken.Transfer.decodeArguments(arguments).let { TokenTransferData(it.to.value, it.value.value) }
            }
            TransactionType.ADD_SAFE_OWNER -> {
                val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.AddOwner.METHOD_ID)
                GnosisSafe.AddOwner.decodeArguments(arguments).let { AddSafeOwnerData(it.owner.value, it._threshold.value.toInt()) }
            }
            TransactionType.REMOVE_SAFE_OWNER -> {
                val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.RemoveOwner.METHOD_ID)
                GnosisSafe.RemoveOwner.decodeArguments(arguments)
                    .let { RemoveSafeOwnerData(it.ownerindex.value, it.owner.value, it._threshold.value.toInt()) }
            }
            TransactionType.REPLACE_SAFE_OWNER -> {
                val arguments = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.ReplaceOwner.METHOD_ID)
                GnosisSafe.ReplaceOwner.decodeArguments(arguments)
                    .let { ReplaceSafeOwnerData(it.oldownerindex.value, it.oldowner.value, it.newowner.value) }
            }
            else -> null
        }

    private fun GnosisSafeTransactionDescription.toTransaction(): Transaction =
        Transaction(to, value = value, data = data, nonce = nonce)
}
