package pm.gnosis.heimdall.ui.transactions.view.review

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.Events
import pm.gnosis.heimdall.ui.transactions.view.helpers.SubmitTransactionHelper.ViewUpdate
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.nullOnThrow
import javax.inject.Inject

class ReviewTransactionViewModel @Inject constructor(
    private val submitTransactionHelper: SubmitTransactionHelper,
    private val bridgeRepository: BridgeRepository,
    private val executionRepository: TransactionExecutionRepository,
    private val tokenRepository: TokenRepository
) : ReviewTransactionContract() {

    private var referenceId: Long? = null
    private var sessionId: String? = null
    private lateinit var safe: Solidity.Address

    private val cachedState = mutableMapOf<SafeTransaction, TransactionExecutionRepository.ExecuteInformation>()

    override fun setup(safe: Solidity.Address, referenceId: Long?, sessionId: String?) {
        if (nullOnThrow { this.safe } != safe) {
            cachedState.clear()
        }
        this.safe = safe
        this.referenceId = referenceId
        this.sessionId = sessionId
        submitTransactionHelper.setup(safe, ::txParams, referenceId)
    }

    private fun txParams(transaction: SafeTransaction): Single<TransactionExecutionRepository.ExecuteInformation> {
        return cachedState[transaction]?.let { Single.just(it) }
            ?: tokenRepository.loadPaymentToken(safe)
                .flatMap { executionRepository.loadExecuteInformation(safe, it.address, transaction) }
                .doOnSuccess { cachedState[transaction] = it }
    }

    override fun observe(events: Events, transactionData: TransactionData): Observable<Result<ViewUpdate>> =
        submitTransactionHelper.observe(events, transactionData)

    override fun cancelReview() {
        referenceId?.let { executionRepository.reject(it) }
    }

    override fun observeSessionInfo() =
        sessionId?.let { id ->
            bridgeRepository.observeSession(id)
                .filter { it is BridgeRepository.SessionEvent.MetaUpdate }
                .map {
                    (it as BridgeRepository.SessionEvent.MetaUpdate).meta.let { meta ->
                        SessionInfo(meta.dappName, meta.dappUrl, meta.dappIcons?.firstOrNull())
                    }
                }
                .mapToResult()
        } ?: Observable.empty()
}
