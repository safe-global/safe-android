package pm.gnosis.heimdall.ui.transactions.view.status

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.transactions.view.helpers.TransactionViewHolderBuilder
import pm.gnosis.heimdall.utils.emitAndNext
import javax.inject.Inject

class TransactionStatusViewModel @Inject constructor(
    private val infoRepository: TransactionInfoRepository,
    private val executionRepository: TransactionExecutionRepository,
    private val tokenRepository: TokenRepository,
    private val transactionViewHolderBuilder: TransactionViewHolderBuilder
) : TransactionStatusContract() {
    override fun observeUpdates(id: String): Observable<ViewUpdate> =
        infoRepository.loadTransactionInfo(id)
            .flatMap { info ->
                if (info.gasToken == ERC20Token.ETHER_TOKEN.address) {
                    Single.just(ERC20Token.ETHER_TOKEN).map { info to it }
                } else {
                    tokenRepository.loadToken(info.gasToken).map { info to it }
                        .onErrorReturnItem(info to ERC20Token(info.gasToken, decimals = 0))
                }
            }
            .emitAndNext(
                emit = { (info, token) ->
                    val gasCosts = info.gasLimit * info.gasPrice
                    ViewUpdate.Params(
                        info.chainHash,
                        info.timestamp,
                        gasCosts,
                        token,
                        info.data.toType()
                    )
                },
                next = { (info) -> transactionViewHolderBuilder.build(info.safe, info.data, false).map<ViewUpdate>(
                    ViewUpdate::Details
                ).toObservable() }
            )

    override fun observeStatus(id: String): Observable<TransactionExecutionRepository.PublishStatus> =
        executionRepository.observePublishStatus(id)

    private fun TransactionData.toType() =
            when (this) {
                is TransactionData.Generic -> R.string.transaction_type_generic
                is TransactionData.AssetTransfer -> R.string.transaction_type_asset_transfer
            }
}
