package pm.gnosis.heimdall.ui.transactions

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.models.Transaction
import java.math.BigInteger
import javax.inject.Inject


class BaseTransactionViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val transactionRepository: TransactionRepository
) : BaseTransactionContract() {

    private val errorHandler = LocalizedException.networkErrorHandlerBuilder(context)
            .build()

    override fun loadTransactionInfo(safeAddress: BigInteger, transaction: Transaction): Observable<Result<Info>> {
        return transactionRepository.loadInformation(safeAddress, transaction)
                .flatMapObservable { info ->
                    info.toSubmitType()
                            .flatMap { transactionRepository.estimateFees(safeAddress, transaction, it)  }
                            .map { Info(info, it) }
                            .toObservable()
                            .startWith(Info(info))
                }
                .onErrorResumeNext(Function { errorHandler.observable(it) })
                .mapToResult()
    }

    override fun submitTransaction(safeAddress: BigInteger, transaction: Transaction): Single<Result<Unit>> {
        return transactionRepository.loadInformation(safeAddress, transaction)
                .flatMap { it.toSubmitType() }
                .flatMapCompletable { transactionRepository.submit(safeAddress, transaction, it) }
                .andThen(Single.just(Unit))
                .onErrorResumeNext({ errorHandler.single(it) })
                .mapToResult()
    }

    private fun TransactionRepository.TransactionInfo.toSubmitType(): Single<TransactionRepository.SubmitType> =
            Single.fromCallable {
                when {
                    isExecuted -> throw LocalizedException(context.getString(R.string.error_transaction_already_executed))
                    confirmations >= requiredConfirmation -> TransactionRepository.SubmitType.EXECUTE
                    confirmations + 1 >= requiredConfirmation && isOwner && !hasConfirmed -> TransactionRepository.SubmitType.CONFIRM_AND_EXECUTE
                    !hasConfirmed && isOwner -> TransactionRepository.SubmitType.CONFIRM
                    !isOwner -> throw LocalizedException(context.getString(R.string.error_confirm_not_owner))
                    // hasConfirmed == true
                    else -> throw LocalizedException(context.getString(R.string.error_transaction_already_confirmed))
                }
            }
}