package io.gnosis.safe.ui.transactions.details

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionInfo
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.calculateSafeTxHash
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.details.viewdata.TransactionDetailsViewData
import io.gnosis.safe.ui.transactions.details.viewdata.toTransactionDetailsViewData
import io.gnosis.safe.utils.OwnerCredentialsRepository
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.toHexString
import javax.inject.Inject

class ConfirmRejectionViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionDetailsViewState>(appDispatchers) {

    var txDetails: TransactionDetails? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    override fun initialState(): TransactionDetailsViewState = TransactionDetailsViewState(ViewAction.Loading(true))

    fun submitRejection() {
        val executionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
            ?: throw MissingCorrectExecutionDetailsException

        safeLaunch {
            val activeSafeAddress = safeRepository.getActiveSafe()!!.address
            val rejectionExecutionInfo = DetailedExecutionInfo.MultisigExecutionDetails(nonce = executionInfo.nonce)
            val rejectionTxDetails = TransactionDetails(
                txInfo = TransactionInfo.Custom(activeSafeAddress),
                detailedExecutionInfo = rejectionExecutionInfo
            )
            val safeTxHash =
                calculateSafeTxHash(
                    safeAddress = activeSafeAddress,
                    transaction = rejectionTxDetails,
                    executionInfo = rejectionExecutionInfo
                )!!.toHexString()
            validateSafeTxHash(txDetails!!, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }
            updateState { TransactionDetailsViewState(BaseStateViewModel.ViewAction.Loading(true)) }
            val ownerCredentials = ownerCredentialsRepository.retrieveCredentials() ?: run { throw MissingOwnerCredential }
            kotlin.runCatching {
                transactionRepository.proposeTransaction(
                    safeAddress = activeSafeAddress,
                    nonce = rejectionExecutionInfo.nonce,
                    signature = transactionRepository.sign(ownerCredentials.key, safeTxHash),
                    safeTxGas = rejectionExecutionInfo.safeTxGas.toLong(),
                    safeTxHash = safeTxHash,
                    sender = ownerCredentials.address
                )
            }.onSuccess {
                tracker.logTransactionRejected()
                val safes = safeRepository.getSafes()
                //TODO ? Modify txDetails to look like it got rejected ?
                updateState { TransactionDetailsViewState(RejectionSubmitted(txDetails!!.toTransactionDetailsViewData(safes))) }
            }.onFailure {
                throw TxRejectionFailed(it)
            }
        }
    }

    fun loadDetails(txId: String) {
        safeLaunch {
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            txDetails = transactionRepository.getTransactionDetails(txId)
            val safes = safeRepository.getSafes()
            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState { TransactionDetailsViewState(UpdateDetails(txDetails?.toTransactionDetailsViewData(safes))) }
        }
    }

    private suspend fun validateSafeTxHash(
        transaction: TransactionDetails,
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails
    ): Boolean {
        return kotlin.runCatching {
            val safe = safeRepository.getActiveSafe()
            val safeTxHash = executionInfo.safeTxHash
            val calculatedSafeTxHash = calculateSafeTxHash(safe!!.address, transaction, executionInfo)?.toHexString()?.addHexPrefix()
            safeTxHash == calculatedSafeTxHash
        }.getOrDefault(false)
    }
}

data class RejectionSubmitted(
    val txDetails: TransactionDetailsViewData?
) : BaseStateViewModel.ViewAction
