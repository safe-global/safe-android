package pm.gnosis.heimdall.ui.transactions.view.helpers

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.viewholders.AssetTransferViewHolder
import pm.gnosis.heimdall.ui.transactions.view.viewholders.GenericTransactionViewHolder
import pm.gnosis.model.Solidity
import javax.inject.Inject


interface TransactionViewHolderBuilder {
    fun build(safe: Solidity.Address, transactionData: TransactionData, extraInfo: Boolean = true): Single<TransactionInfoViewHolder>
}

class DefaultTransactionViewHolderBuilder @Inject constructor(
    private val addressHelper: AddressHelper,
    private val tokenRepository: TokenRepository
    ): TransactionViewHolderBuilder {
    override fun build(safe: Solidity.Address, transactionData: TransactionData, extraInfo: Boolean): Single<TransactionInfoViewHolder> =
        Single.fromCallable {
            when (transactionData) {
                is TransactionData.Generic ->
                    GenericTransactionViewHolder(safe, transactionData, addressHelper)
                is TransactionData.AssetTransfer ->
                    AssetTransferViewHolder(
                        safe,
                        transactionData,
                        addressHelper,
                        tokenRepository,
                        extraInfo
                    )
            }
        }
}
