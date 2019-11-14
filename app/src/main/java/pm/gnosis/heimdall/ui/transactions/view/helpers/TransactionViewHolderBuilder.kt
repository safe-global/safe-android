package pm.gnosis.heimdall.ui.transactions.view.helpers

import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.viewholders.*
import pm.gnosis.model.Solidity
import javax.inject.Inject


interface TransactionViewHolderBuilder {
    fun build(safe: Solidity.Address, transactionData: TransactionData, extraInfo: Boolean = true): Single<TransactionInfoViewHolder>
}

class DefaultTransactionViewHolderBuilder @Inject constructor(
    private val addressHelper: AddressHelper,
    private val safeRepository: GnosisSafeRepository,
    private val tokenRepository: TokenRepository
) : TransactionViewHolderBuilder {
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
                is TransactionData.ReplaceRecoveryPhrase ->
                    ReplaceRecoveryPhraseViewHolder(addressHelper, safe, transactionData.safeTransaction)
                is TransactionData.ConnectAuthenticator ->
                    ConnectAuthenticatorViewHolder(
                        addressHelper = addressHelper,
                        extension = transactionData.extension,
                        safe = safe,
                        safeRepository = safeRepository
                    )
                is TransactionData.UpdateMasterCopy ->
                    UpdateMasterCopyViewHolder(
                        addressHelper = addressHelper,
                        data = transactionData,
                        safe = safe
                    )
                is TransactionData.MultiSend ->
                    MultiSendViewHolder(
                        addressHelper = addressHelper,
                        data = transactionData,
                        safe = safe
                    )
            }
        }
}
