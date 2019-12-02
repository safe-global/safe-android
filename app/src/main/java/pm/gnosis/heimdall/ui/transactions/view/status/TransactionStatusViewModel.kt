package pm.gnosis.heimdall.ui.transactions.view.status

import io.reactivex.Observable
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
                tokenRepository.loadToken(info.gasToken).map { info to it }
                    .onErrorReturnItem(info to ERC20Token(info.gasToken, "", "", 0, ""))
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
                next = { (info) ->
                    transactionViewHolderBuilder.build(info.safe, info.data, false).map<ViewUpdate>(
                        ViewUpdate::Details
                    ).toObservable()
                }
            )

    override fun observeStatus(id: String): Observable<TransactionExecutionRepository.PublishStatus> =
        executionRepository.observePublishStatus(id)

    private fun TransactionData.toType() =
        when (this) {
            is TransactionData.Generic -> R.string.transaction_type_generic
            is TransactionData.AssetTransfer -> R.string.transaction_type_asset_transfer
            is TransactionData.ReplaceRecoveryPhrase -> R.string.settings_change
            is TransactionData.ConnectAuthenticator -> R.string.settings_change
            is TransactionData.UpdateMasterCopy -> R.string.contract_upgrade
            is TransactionData.MultiSend -> R.string.multi_send
        }
}
