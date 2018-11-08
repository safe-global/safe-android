package pm.gnosis.heimdall.ui.transactions.view.review

import io.reactivex.Observable
import io.reactivex.Single
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
    private val executionRepository: TransactionExecutionRepository
) : ReviewTransactionContract() {

    private lateinit var safe: Solidity.Address

    private val cachedState = mutableMapOf<SafeTransaction, TransactionExecutionRepository.ExecuteInformation>()

    override fun setup(safe: Solidity.Address) {
        if (nullOnThrow { this.safe } != safe) {
            cachedState.clear()
        }
        this.safe = safe
        submitTransactionHelper.setup(safe, ::txParams)
    }

    private fun txParams(transaction: SafeTransaction): Single<TransactionExecutionRepository.ExecuteInformation> {
        return cachedState[transaction]?.let { Single.just(it) }
                ?: executionRepository.loadExecuteInformation(safe, ERC20Token.ETHER_TOKEN.address, transaction)
                    .doOnSuccess { cachedState[transaction] = it }
    }

    override fun observe(events: Events, transactionData: TransactionData): Observable<Result<ViewUpdate>> =
        submitTransactionHelper.observe(events, transactionData)
}
