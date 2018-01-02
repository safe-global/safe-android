package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import java.math.BigInteger
import javax.inject.Inject


class ViewTransactionViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val transactionRepository: TransactionRepository,
        private val transactionDetailsRepository: TransactionDetailsRepository
) : ViewTransactionContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
            .build()

    override fun checkTransactionType(transaction: Transaction): Single<TransactionType> {
        return transactionDetailsRepository.loadTransactionType(transaction)
    }

    override fun loadTransactionInfo(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Info>> {
        return transactionRepository.loadStatus(safeAddress)
                .flatMapObservable { info ->
                    Single.fromCallable { info.check() }
                            .flatMap { transactionRepository.estimateFees(safeAddress, transaction) }
                            .map { Info(safeAddress, transaction, info, it) }
                            .toObservable()
                            // First we emit the info without estimate, so the user will see
                            // independently from what happens to the estimation request
                            .startWith(Info(safeAddress, transaction, info))
                }
                .onErrorResumeNext(Function { errorHandler.observable(it) })
                .mapToResult()
    }

    override fun submitTransaction(safeAddress: BigInteger, transaction: Transaction, overrideGasPrice: Wei?): Single<Result<BigInteger>> {
        return transactionRepository.loadStatus(safeAddress)
                .flatMapCompletable {
                    it.check()
                    transactionRepository.submit(safeAddress, transaction, overrideGasPrice)
                }
                .andThen(Single.just(safeAddress))
                .onErrorResumeNext({ errorHandler.single(it) })
                .mapToResult()
    }

    private fun TransactionRepository.TransactionStatus.check() {
        when {
            requiredConfirmation - (if (isOwner) 1 else 0) > 0 ->
                throw SimpleLocalizedException(context.getString(R.string.error_not_enough_confirmations))
        }
    }
}
