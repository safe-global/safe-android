package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.TransactionStatus
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.transactions.view.status.TransactionStatusActivity
import pm.gnosis.heimdall.utils.DateTimeUtils
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

class SafeTransactionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository,
    private val safeTransactionsRepository: TransactionExecutionRepository,
    private val tokenRepository: TokenRepository,
    private val transactionInfoRepository: TransactionInfoRepository
) : SafeTransactionsContract() {

    private var address: Solidity.Address? = null

    override fun setup(address: Solidity.Address) {
        this.address = address
    }

    override fun observeTransactions(): Flowable<out Result<Adapter.Data<AdapterEntry>>> {
        return this.address?.let {
            Flowable.combineLatest<List<TransactionStatus>, List<TransactionStatus>, List<AdapterEntry>>(
                safeRepository.observePendingTransactions(it),
                safeRepository.observeSubmittedTransactions(it),
                BiFunction { pending, submitted ->
                    val currentTime = System.currentTimeMillis()
                    val combined = ArrayList<AdapterEntry>((pending.size + submitted.size) * 2)
                    if (pending.isNotEmpty()) {
                        combined += AdapterEntry.Header(R.string.header_pending)
                        pending.forEach { combined += AdapterEntry.Transaction(it.id) }
                    }
                    var lastHeaderRes = 0
                    submitted.forEach {
                        val timeDiff = currentTime - it.timestamp
                        val headerRes = when {
                            timeDiff < DateTimeUtils.DAY_IN_MS -> R.string.header_today
                            timeDiff < 2 * DateTimeUtils.DAY_IN_MS -> R.string.header_yesterday
                            lastHeaderRes == 0 -> R.string.header_submitted // First header should never be "older"
                            else -> R.string.header_older
                        }
                        if (lastHeaderRes != headerRes && headerRes != 0) {
                            combined += AdapterEntry.Header(headerRes)
                            lastHeaderRes = headerRes
                        }
                        combined += AdapterEntry.Transaction(it.id)
                    }
                    combined
                }
            )
                .scanToAdapterData()
                .mapToResult()
        } ?: Flowable.empty<Result<Adapter.Data<AdapterEntry>>>()
    }

    override fun loadTransactionInfo(id: String): Single<TransactionInfo> =
        transactionInfoRepository.loadTransactionInfo(id)

    override fun loadTokenInfo(token: Solidity.Address): Single<ERC20Token> =
        if (token == ERC20Token.ETHER_TOKEN.address)
            Single.just(ERC20Token.ETHER_TOKEN)
        else
            tokenRepository.loadToken(token)
                .onErrorResumeNext {
                    tokenRepository.loadTokenInfo(token).firstOrError()
                }

    override fun observeTransactionStatus(id: String): Observable<TransactionExecutionRepository.PublishStatus> =
        safeTransactionsRepository.observePublishStatus(id)

    override fun transactionSelected(id: String): Single<Intent> =
        Single.just(TransactionStatusActivity.createIntent(context, id))
}
