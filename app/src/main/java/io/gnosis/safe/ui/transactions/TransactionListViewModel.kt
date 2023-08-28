package io.gnosis.safe.ui.transactions

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.TransactionLocal
import io.gnosis.data.models.transaction.ConflictType
import io.gnosis.data.models.transaction.SafeAppInfo
import io.gnosis.data.models.transaction.Transaction
import io.gnosis.data.models.transaction.TransactionDirection
import io.gnosis.data.models.transaction.TransactionInfo
import io.gnosis.data.models.transaction.TransactionStatus
import io.gnosis.data.models.transaction.TransferInfo
import io.gnosis.data.models.transaction.TransferType
import io.gnosis.data.models.transaction.TxListEntry
import io.gnosis.data.models.transaction.decimals
import io.gnosis.data.models.transaction.symbol
import io.gnosis.data.models.transaction.value
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionLocalRepository
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.paging.TransactionPagingProvider
import io.gnosis.safe.ui.transactions.paging.TransactionPagingSource
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.DEFAULT_ERC20_SYMBOL
import io.gnosis.safe.utils.DEFAULT_ERC721_SYMBOL
import io.gnosis.safe.utils.formatBackendDateTime
import io.gnosis.safe.utils.formatBackendTimeOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

class TransactionListViewModel
@Inject constructor(
    private val transactionsPager: TransactionPagingProvider,
    private val transactionLocalRepository: TransactionLocalRepository,
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val balanceFormatter: BalanceFormatter,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionsViewState>(appDispatchers) {

    override fun initialState(): TransactionsViewState = TransactionsViewState(null, true)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState { TransactionsViewState(isLoading = true, viewAction = ActiveSafeChanged(safe)) }
            }
        }
    }

    fun load(type: TransactionPagingSource.Type) {
        safeLaunch {
            val activeSafe = safeRepository.getActiveSafe()
            val safes = safeRepository.getSafes()
            if (activeSafe != null) {
                val owners = credentialsRepository.owners()
                getTransactions(activeSafe, safes, owners, type).collectLatest {
                    updateState {
                        TransactionsViewState(
                            isLoading = false,
                            viewAction = LoadTransactions(it)
                        )
                    }
                }
            } else {
                updateState(forceViewAction = true) { TransactionsViewState(isLoading = false, viewAction = NoSafeSelected) }
            }
        }
    }

    private suspend fun getTransactions(
        safe: Safe,
        safes: List<Safe>,
        owners: List<Owner>,
        type: TransactionPagingSource.Type
    ): Flow<PagingData<TransactionView>> {

        var txLocal: TransactionLocal? = null
        if (type == TransactionPagingSource.Type.QUEUE ) {
            txLocal = transactionLocalRepository.updateLocalTxLatest(safe)
        }

        val safeTxItems: Flow<PagingData<TransactionView>> = transactionsPager.getTransactionsStream(safe, type)
            .map { pagingData ->
                pagingData
                    .map { txListEntry ->
                        when (txListEntry) {
                            is TxListEntry.Transaction -> {
                                // conflict is resolved if there is local tx with same nonce
                                // that was submitted for execution
                                if (txLocal?.safeTxNonce == txListEntry.transaction.executionInfo?.nonce) {
                                    if (txLocal?.safeTxHash == txListEntry.transaction.id.split("_").last() && txListEntry.transaction.txStatus == TransactionStatus.AWAITING_EXECUTION) {
                                        val tx = txListEntry.transaction.copy(txStatus = TransactionStatus.PENDING)
                                        txListEntry.transaction
                                        getTransactionView(
                                            chain = safe.chain,
                                            transaction = tx,
                                            safes = safes,
                                            needsYourConfirmation = false,
                                            isConflict = false,
                                            localOwners = owners
                                        )
                                    } else {
                                        TransactionView.Unknown
                                    }
                                } else {
                                    val isConflict = txListEntry.conflictType != ConflictType.None
                                    val txView =
                                        getTransactionView(
                                            chain = safe.chain,
                                            transaction = txListEntry.transaction,
                                            safes = safes,
                                            needsYourConfirmation = txListEntry.transaction.canBeSignedByAnyOwner(owners),
                                            isConflict = isConflict,
                                            localOwners = owners
                                        )
                                    if (isConflict) {
                                        TransactionView.Conflict(txView, txListEntry.conflictType)
                                    } else txView
                                }
                            }
                            is TxListEntry.DateLabel -> TransactionView.SectionDateHeader(date = txListEntry.timestamp)
                            is TxListEntry.Label -> TransactionView.SectionLabelHeader(label = txListEntry.label)
                            is TxListEntry.ConflictHeader -> {
                                // conflict is resolved if there is local tx with same nonce
                                // that was submitted for execution
                                if (txLocal?.safeTxNonce == txListEntry.nonce.toBigInteger()) {
                                    TransactionView.Unknown
                                } else {
                                    TransactionView.SectionConflictHeader(nonce = txListEntry.nonce)
                                }
                            }
                            else -> TransactionView.Unknown
                        }
                    }
                    .filter { it !is TransactionView.Unknown }
            }
            .cachedIn(viewModelScope)

        return safeTxItems
    }

    fun getTransactionView(
        chain: Chain,
        transaction: Transaction,
        safes: List<Safe>,
        needsYourConfirmation: Boolean = false,
        isConflict: Boolean = false,
        localOwners: List<Owner> = listOf()
    ): TransactionView {
        with(transaction) {
            return when (val txInfo = txInfo) {
                is TransactionInfo.Transfer -> toTransferView(chain, txInfo, needsYourConfirmation, isConflict)
                is TransactionInfo.SettingsChange -> toSettingsChangeView(chain, txInfo, needsYourConfirmation, isConflict)
                is TransactionInfo.Custom -> {
                    if (txInfo.isCancellation) {
                        toRejectionTransactionView(chain, needsYourConfirmation, isConflict)
                    } else {
                        toCustomTransactionView(chain, txInfo, safes, needsYourConfirmation, isConflict)
                    }
                }
                is TransactionInfo.Creation -> {
                    val owner = localOwners.find { it.address == txInfo.creator.value }
                    toHistoryCreation(chain, txInfo, owner)
                }
                TransactionInfo.Unknown -> TransactionView.Unknown
            }
        }
    }

    private fun Transaction.toTransferView(
        chain: Chain,
        txInfo: TransactionInfo.Transfer,
        needsYourConfirmation: Boolean,
        isConflict: Boolean = false
    ): TransactionView =
        if (isCompleted(txStatus)) historicTransfer(chain, txInfo, isConflict)
        else queuedTransfer(chain, txInfo, needsYourConfirmation, isConflict)

    private fun Transaction.historicTransfer(chain: Chain, txInfo: TransactionInfo.Transfer, isConflict: Boolean): TransactionView.Transfer =
        TransactionView.Transfer(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            amountText = formatTransferAmount(chain, txInfo.transferInfo, txInfo.incoming()),
            dateTimeText = timestamp.formatBackendTimeOfDay(),
            txTypeIcon = if (txInfo.incoming()) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            direction = if (txInfo.incoming()) R.string.tx_list_receive else R.string.tx_list_send,
            amountColor = if (txInfo.transferInfo.value() > BigInteger.ZERO && txInfo.incoming()) R.color.success else R.color.label_primary,
            alpha = alpha(txStatus),
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString() ?: ""
        )

    private fun Transaction.queuedTransfer(
        chain: Chain,
        txInfo: TransactionInfo.Transfer,
        needsYourConfirmation: Boolean,
        isConflict: Boolean = false
    ): TransactionView.TransferQueued {
        //FIXME this wouldn't make sense for incoming Ethereum TXs
        val threshold = executionInfo?.confirmationsRequired ?: -1
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)
        val incoming = txInfo.incoming()

        return TransactionView.TransferQueued(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, needsYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            amountText = formatTransferAmount(chain, txInfo.transferInfo, incoming),
            dateTime = timestamp,
            txTypeIcon = if (incoming) R.drawable.ic_arrow_green_10dp else R.drawable.ic_arrow_red_10dp,
            direction = if (txInfo.incoming()) R.string.tx_list_receive else R.string.tx_list_send,
            amountColor = if (txInfo.transferInfo.value() > BigInteger.ZERO && incoming) R.color.success else R.color.label_primary,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.success else R.color.icon,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.toSettingsChangeView(
        chain: Chain,
        txInfo: TransactionInfo.SettingsChange,
        needsYourConfirmation: Boolean,
        isConflict: Boolean = false
    ): TransactionView =
        when {
            isQueuedSettingsChange(txStatus) -> queuedSettingsChange(chain, txInfo, needsYourConfirmation, isConflict)
            isHistoricSettingsChange(txStatus) -> historicSettingsChange(chain, txInfo)
            else -> TransactionView.Unknown
        }

    private fun isHistoricSettingsChange(txStatus: TransactionStatus): Boolean = isCompleted(txStatus)

    private fun isQueuedSettingsChange(txStatus: TransactionStatus): Boolean = !isCompleted(txStatus)

    private fun Transaction.historicSettingsChange(chain: Chain, txInfo: TransactionInfo.SettingsChange): TransactionView.SettingsChange =
        TransactionView.SettingsChange(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendTimeOfDay(),
            method = txInfo.dataDecoded.method,
            alpha = alpha(txStatus),
            nonce = executionInfo?.nonce?.toString().orEmpty()
        )

    private fun Transaction.queuedSettingsChange(
        chain: Chain,
        txInfo: TransactionInfo.SettingsChange,
        needsYourConfirmation: Boolean,
        isConflict: Boolean
    ): TransactionView.SettingsChangeQueued {
        //FIXME this wouldn't make sense for incoming Ethereum TXs
        val threshold = executionInfo?.confirmationsRequired ?: -1
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)

        return TransactionView.SettingsChangeQueued(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, needsYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTime = timestamp,
            method = txInfo.dataDecoded.method,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.success else R.color.icon,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString().orEmpty()
        )
    }

    private fun Transaction.toCustomTransactionView(
        chain: Chain,
        txInfo: TransactionInfo.Custom,
        safes: List<Safe>,
        needsYourConfirmation: Boolean,
        isConflict: Boolean
    ): TransactionView =
        if (!isCompleted(txStatus)) queuedCustomTransaction(chain, txInfo, safes, needsYourConfirmation, isConflict)
        else historicCustomTransaction(chain, txInfo, safes)

    private fun Transaction.historicCustomTransaction(
        chain: Chain,
        txInfo: TransactionInfo.Custom,
        safes: List<Safe>
    ): TransactionView.CustomTransaction {

        val addressInfo = resolveKnownAddress(txInfo.to, safeAppInfo, safes)

        return TransactionView.CustomTransaction(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendTimeOfDay(),
            alpha = alpha(txStatus),
            nonce = executionInfo?.nonce?.toString() ?: "",
            addressInfo = addressInfo,
            methodName = txInfo.methodName,
            actionCount = txInfo.actionCount
        )
    }

    private fun Transaction.queuedCustomTransaction(
        chain: Chain,
        txInfo: TransactionInfo.Custom,
        safes: List<Safe>,
        needsYourConfirmation: Boolean,
        isConflict: Boolean
    ): TransactionView.CustomTransactionQueued {

        //FIXME this wouldn't make sense for incoming Ethereum TXs
        val threshold = executionInfo?.confirmationsRequired ?: -1
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)

        val addressInfo = resolveKnownAddress(txInfo.to, safeAppInfo, safes)

        return TransactionView.CustomTransactionQueued(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, needsYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTime = timestamp,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.success else R.color.icon,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString() ?: "",
            methodName = txInfo.methodName,
            actionCount = txInfo.actionCount,
            addressInfo = addressInfo
        )
    }

    private fun Transaction.toRejectionTransactionView(
        chain: Chain,
        awaitingYourConfirmation: Boolean,
        isConflict: Boolean
    ): TransactionView =
        if (!isCompleted(txStatus)) queuedRejectionTransaction(chain, awaitingYourConfirmation, isConflict)
        else historicRejectionTransaction(chain)

    private fun Transaction.historicRejectionTransaction(chain: Chain): TransactionView.RejectionTransaction {

        return TransactionView.RejectionTransaction(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendTimeOfDay(),
            alpha = alpha(txStatus),
            nonce = executionInfo?.nonce?.toString() ?: ""
        )
    }

    private fun Transaction.queuedRejectionTransaction(
        chain: Chain,
        awaitingYourConfirmation: Boolean,
        isConflict: Boolean
    ): TransactionView.RejectionTransactionQueued {

        //FIXME this wouldn't make sense for incoming Ethereum TXs
        val threshold = executionInfo?.confirmationsRequired ?: -1
        val thresholdMet = checkThreshold(threshold, executionInfo?.confirmationsSubmitted)

        return TransactionView.RejectionTransactionQueued(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus, awaitingYourConfirmation),
            statusColorRes = statusTextColor(txStatus),
            dateTime = timestamp,
            confirmations = executionInfo?.confirmationsSubmitted ?: 0,
            threshold = threshold,
            confirmationsTextColor = if (thresholdMet) R.color.success else R.color.icon,
            confirmationsIcon = if (thresholdMet) R.drawable.ic_confirmations_green_16dp else R.drawable.ic_confirmations_grey_16dp,
            nonce = if (isConflict) "" else executionInfo?.nonce?.toString() ?: ""
        )
    }

    private fun resolveKnownAddress(
        addressData: AddressInfo,
        safeAppInfo: SafeAppInfo?,
        safes: List<Safe>
    ): AddressInfoData {
        val localName = safes.find { it.address == addressData.value }?.localName
        val addressString = addressData.value.asEthereumAddressString()
        return when {
            safeAppInfo != null -> AddressInfoData.Remote(safeAppInfo.name, safeAppInfo.logoUri, addressString, true)
            localName != null -> AddressInfoData.Local(localName, addressString)
            !addressData.name.isNullOrBlank() -> AddressInfoData.Remote(addressData.name, addressData.logoUri, addressString, false)
            else -> AddressInfoData.Default
        }
    }

    private fun Transaction.toHistoryCreation(chain: Chain, txInfo: TransactionInfo.Creation, localCreator: Owner?): TransactionView.Creation =
        TransactionView.Creation(
            chain = chain,
            id = id,
            status = txStatus,
            statusText = displayString(txStatus),
            statusColorRes = statusTextColor(txStatus),
            dateTimeText = timestamp.formatBackendTimeOfDay(),
            label = R.string.tx_list_creation,
            creationDetails = TransactionView.CreationDetails(
                statusText = displayString(txStatus),
                statusColorRes = statusTextColor(txStatus),
                dateTimeText = timestamp.formatBackendDateTime(),
                creator = txInfo.creator.value.asEthereumAddressString(),
                creatorInfo =
                    if (localCreator == null) {
                        AddressInfoData.Remote(txInfo.creator.name, txInfo.creator.logoUri, txInfo.creator.value.asEthereumAddressString())
                    } else {
                        AddressInfoData.Local(localCreator.name, localCreator.address.asEthereumAddressString())
                    },
                factory = txInfo.factory?.value?.asEthereumAddressString(),
                factoryInfo = AddressInfoData.Remote(
                    txInfo.factory?.name,
                    txInfo.factory?.logoUri,
                    txInfo.factory?.value?.asEthereumAddressString()
                ),
                implementation = txInfo.implementation?.value?.asEthereumAddressString(),
                implementationInfo = AddressInfoData.Remote(
                    txInfo.implementation?.name,
                    txInfo.implementation?.logoUri,
                    txInfo.implementation?.value?.asEthereumAddressString()
                ),
                transactionHash = txInfo.transactionHash
            )
        )

    private fun isCompleted(status: TransactionStatus): Boolean =
        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS,
            TransactionStatus.AWAITING_EXECUTION,
            TransactionStatus.PENDING -> false
            TransactionStatus.SUCCESS,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED -> true
        }

    @StringRes
    private fun displayString(status: TransactionStatus, needsYourConfirmation: Boolean = false): Int =
        when (status) {
            TransactionStatus.AWAITING_CONFIRMATIONS -> if (needsYourConfirmation) R.string.tx_status_needs_your_confirmation else R.string.tx_status_needs_confirmations
            TransactionStatus.AWAITING_EXECUTION -> R.string.tx_status_needs_execution
            TransactionStatus.CANCELLED -> R.string.tx_status_cancelled
            TransactionStatus.FAILED -> R.string.tx_status_failed
            TransactionStatus.SUCCESS -> R.string.tx_status_success
            TransactionStatus.PENDING -> R.string.tx_status_pending
        }

    private fun formatTransferAmount(chain: Chain, transferInfo: TransferInfo, incoming: Boolean): String {
        val symbol = transferInfo.symbol(chain).let { symbol ->
            if (symbol.isNullOrEmpty()) {
                getDefaultSymbol(transferInfo.type)
            } else {
                symbol
            }
        }
        return balanceFormatter.formatAmount(transferInfo.value(), incoming, transferInfo.decimals(chain) ?: 0, symbol)
    }

    private fun getDefaultSymbol(type: TransferType?): String = when (type) {
        TransferType.ERC721 -> DEFAULT_ERC721_SYMBOL
        TransferType.ERC20 -> DEFAULT_ERC20_SYMBOL
        else -> ""
    }

    private fun statusTextColor(status: TransactionStatus): Int {
        return when (status) {
            TransactionStatus.SUCCESS -> R.color.success
            TransactionStatus.CANCELLED -> R.color.label_secondary
            TransactionStatus.AWAITING_EXECUTION,
            TransactionStatus.AWAITING_CONFIRMATIONS,
            TransactionStatus.PENDING -> R.color.warning
            else -> R.color.error
        }
    }

    private fun alpha(txStatus: TransactionStatus): Float =
        when (txStatus) {
            TransactionStatus.FAILED, TransactionStatus.CANCELLED -> OPACITY_HALF
            else -> OPACITY_FULL
        }

    private fun checkThreshold(safeThreshold: Int, txThreshold: Int?): Boolean {
        return txThreshold?.let {
            it >= safeThreshold
        } ?: false
    }

    companion object {
        const val OPACITY_FULL = 1.0F
        const val OPACITY_HALF = 0.5F
    }
}


data class TransactionsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?,
    val isLoading: Boolean
) : BaseStateViewModel.State

data class LoadTransactions(
    val newTransactions: PagingData<TransactionView>
) : BaseStateViewModel.ViewAction

data class ActiveSafeChanged(
    val activeSafe: Safe?
) : BaseStateViewModel.ViewAction

object NoSafeSelected : BaseStateViewModel.ViewAction

fun Transaction.canBeSignedByAnyOwner(owners: List<Owner>): Boolean {
    return executionInfo?.missingSigners?.any { address ->
        owners.any { owner ->
            owner.address == address.value
        }
    } ?: false
}

fun TransactionInfo.Transfer.incoming(): Boolean = direction != TransactionDirection.OUTGOING
