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
        return transactionRepository.loadInformation(safeAddress, transaction)
                .flatMapObservable { info ->
                    info.toSubmitType()
                            .flatMap { transactionRepository.estimateFees(safeAddress, transaction, it) }
                            .map { Info(info, it) }
                            .toObservable()
                            // First we emit the info without estimate, so the user will see
                            // independently from what happens to the estimation request
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
                    isExecuted -> throw SimpleLocalizedException(context.getString(R.string.error_transaction_already_executed))
                    confirmations >= requiredConfirmation -> TransactionRepository.SubmitType.EXECUTE
                    confirmations + 1 >= requiredConfirmation && isOwner && !hasConfirmed -> TransactionRepository.SubmitType.CONFIRM_AND_EXECUTE
                    !hasConfirmed && isOwner -> TransactionRepository.SubmitType.CONFIRM
                    !isOwner -> throw SimpleLocalizedException(context.getString(R.string.error_confirm_not_owner))
                    else -> throw SimpleLocalizedException(context.getString(R.string.error_transaction_already_confirmed)) // Last branch left is `hasConfirmed == true`
                }
            }
}