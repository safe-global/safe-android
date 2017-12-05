package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger


interface TransactionRepository {
    fun calculateHash(safeAddress: BigInteger, transaction: Transaction): Single<ByteArray>
    fun loadInformation(safeAddress: BigInteger, transaction: Transaction): Single<TransactionInfo>
    fun estimateFees(safeAddress: BigInteger, transaction: Transaction, type: SubmitType): Single<Wei>
    fun submit(safeAddress: BigInteger, transaction: Transaction, type: SubmitType): Completable

    data class TransactionInfo(val isOwner: Boolean, val requiredConfirmation: Int, val confirmations: Int, val isExecuted: Boolean, val hasConfirmed: Boolean)

    enum class SubmitType {
        CONFIRM,
        CONFIRM_AND_EXECUTE,
        EXECUTE
    }
}