package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger


interface TransactionRepository {
    fun calculateHash(safeAddress: BigInteger, transaction: Transaction): Single<ByteArray>
    fun loadStatus(safeAddress: BigInteger, transaction: Transaction): Single<TransactionStatus>
    fun estimateFees(safeAddress: BigInteger, transaction: Transaction, type: SubmitType): Single<GasEstimate>
    fun submit(safeAddress: BigInteger, transaction: Transaction, type: SubmitType, overrideGasPrice: Wei? = null): Completable

    data class TransactionStatus(val isOwner: Boolean, val requiredConfirmation: Int, val confirmations: Int, val isExecuted: Boolean, val hasConfirmed: Boolean)

    enum class SubmitType {
        CONFIRM,
        CONFIRM_AND_EXECUTE,
        EXECUTE
    }
}