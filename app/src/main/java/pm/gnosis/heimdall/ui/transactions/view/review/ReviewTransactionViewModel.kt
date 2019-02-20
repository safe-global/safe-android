package pm.gnosis.heimdall.ui.transactions.view.review

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.Events
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.ViewUpdate
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.utils.nullOnThrow
import javax.inject.Inject

class ReviewTransactionViewModel @Inject constructor(
    private val submitTransactionHelper: SubmitTransactionHelper,
    private val executionRepository: TransactionExecutionRepository,
    private val tokenRepository: TokenRepository
) : ReviewTransactionContract() {

    private var referenceId: Long? = null
    private lateinit var safe: Solidity.Address

    private val cachedState = mutableMapOf<SafeTransaction, TransactionExecutionRepository.ExecuteInformation>()

    override fun setup(safe: Solidity.Address, referenceId: Long?) {
        if (nullOnThrow { this.safe } != safe) {
            cachedState.clear()
        }
        this.safe = safe
        this.referenceId = referenceId
        submitTransactionHelper.setup(safe, ::txParams, referenceId)
    }

    private fun txParams(transaction: SafeTransaction): Single<TransactionExecutionRepository.ExecuteInformation> {
        return cachedState[transaction]?.let { Single.just(it) }
            ?: tokenRepository.loadPaymentToken()
                .flatMap { executionRepository.loadExecuteInformation(safe, it.address, transaction) }
                .doOnSuccess { cachedState[transaction] = it }
    }

    override fun observe(events: Events, transactionData: TransactionData): Observable<Result<ViewUpdate>> =
        submitTransactionHelper.observe(events, transactionData)

    override fun cancelReview() {
        referenceId?.let { executionRepository.reject(it) }
    }
}
