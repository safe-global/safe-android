package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.models.DetailedExecutionInfo
import io.gnosis.data.models.TransactionDetails
import io.gnosis.data.models.TransactionStatus
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.calculateSafeTxHash
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.OwnerCredentialsRepository
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.toHexString
import javax.inject.Inject

class TransactionDetailsViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionDetailsViewState>(appDispatchers) {

    override fun initialState() = TransactionDetailsViewState(ViewAction.Loading(true))

    fun loadDetails(txId: String) {
        safeLaunch {
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            val txDetails = transactionRepository.getTransactionDetails(txId)
            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState { TransactionDetailsViewState(UpdateDetails(txDetails)) }
        }
    }

    fun isAwaitingOwnerConfirmation(executionInfo: DetailedExecutionInfo.MultisigExecutionDetails, status: TransactionStatus): Boolean =
        status == TransactionStatus.AWAITING_CONFIRMATIONS &&
                ownerCredentialsRepository.hasCredentials() &&
                true == ownerCredentialsRepository.retrieveCredentials()?.let { credentials ->
            executionInfo.signers.contains(credentials.address) && !executionInfo.confirmations.map { it.signer }.contains(credentials.address)
        }

    fun submitConfirmation(transaction: TransactionDetails, executionInfo: DetailedExecutionInfo.MultisigExecutionDetails) {
        safeLaunch {
            validateSafeTxHash(transaction, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            val ownerCredentials = ownerCredentialsRepository.retrieveCredentials() ?: run { throw MissingOwnerCredential }
            val txDetails = transactionRepository.submitConfirmation(
                executionInfo.safeTxHash,
                transactionRepository.sign(ownerCredentials.key, executionInfo.safeTxHash)
            )
            updateState { TransactionDetailsViewState(ConfirmationSubmitted(txDetails)) }
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

open class TransactionDetailsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateDetails(
    val txDetails: TransactionDetails?
) : BaseStateViewModel.ViewAction

data class ConfirmationSubmitted(
    val txDetails: TransactionDetails?
) : BaseStateViewModel.ViewAction

object MismatchingSafeTxHash : Throwable()
object MissingOwnerCredential : Throwable()
