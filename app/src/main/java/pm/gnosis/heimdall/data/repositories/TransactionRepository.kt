package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger


interface TransactionRepository {
    fun calculateHash(safeAddress: BigInteger, transaction: Transaction): Single<ByteArray>
    fun loadStatus(safeAddress: BigInteger): Single<TransactionStatus>
    fun estimateFees(safeAddress: BigInteger, transaction: Transaction): Single<GasEstimate>
    fun submit(safeAddress: BigInteger, transaction: Transaction, overrideGasPrice: Wei? = null): Completable

    data class TransactionStatus(val isOwner: Boolean, val requiredConfirmation: Int)

    enum class PublishStatus {
        UNKNOWN,
        PENDING,
        FAILED,
        SUCCESS
    }

    fun observePublishStatus(id: String): Observable<PublishStatus>
}