package io.gnosis.safe.ui.transactions.details

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionStatus
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.calculateSafeTxHash
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.transactions.details.viewdata.TransactionDetailsViewData
import io.gnosis.safe.ui.transactions.details.viewdata.toTransactionDetailsViewData
import io.gnosis.safe.utils.isCompleted
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import javax.inject.Inject

class TransactionDetailsViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionDetailsViewState>(appDispatchers) {

    override fun initialState() = TransactionDetailsViewState(ViewAction.Loading(true))

    fun loadDetails(txId: String) {
        safeLaunch {
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            txDetails = transactionRepository.getTransactionDetails(txId)
            val safes = safeRepository.getSafes()

            val executionInfo = txDetails?.detailedExecutionInfo

            var awaitingConfirm = false
            var rejectable = false
            var safeOwner = false
            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                awaitingConfirm = isAwaitingOwnerConfirmation(executionInfo, txDetails!!.txStatus)
                rejectable = canBeRejectedFromDevice(executionInfo, txDetails!!.txStatus)
                safeOwner = isOwner(executionInfo)
            }

            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState { TransactionDetailsViewState(UpdateDetails(txDetails?.toTransactionDetailsViewData(safes), awaitingConfirm, rejectable, safeOwner)) }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun isAwaitingOwnerConfirmation(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        status: TransactionStatus
    ): Boolean =
        status == TransactionStatus.AWAITING_CONFIRMATIONS &&
                credentialsRepository.ownerCount() > 0 &&
                credentialsRepository.owners()[0].let { owner ->
                    executionInfo.signers.contains(owner.address) && !executionInfo.confirmations.map { it.signer }.contains(owner.address)
                }


    //TODO: remove when backend provides info about rejections
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun canBeRejectedFromDevice(executionInfo: DetailedExecutionInfo.MultisigExecutionDetails, status: TransactionStatus): Boolean =
        !status.isCompleted() &&
                credentialsRepository.ownerCount() > 0 &&
                credentialsRepository.owners()[0].let { credentials ->
                    executionInfo.signers.contains(credentials.address)
                }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun isOwner(executionInfo: DetailedExecutionInfo.MultisigExecutionDetails): Boolean {
        return credentialsRepository.ownerCount() > 0 &&
                credentialsRepository.owners()[0].let { owner ->
                    executionInfo.signers.contains(owner.address)
                }
    }

    var txDetails: TransactionDetails? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    fun submitConfirmation(transaction: TransactionDetails) {
        val executionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
            ?: throw MissingCorrectExecutionDetailsException

        safeLaunch {
            validateSafeTxHash(transaction, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            if (credentialsRepository.ownerCount() == 0) {
                throw MissingOwnerCredential
            }
            val owner = credentialsRepository.owners()[0]
            //TODO: unlock with passcode if passcode is enabled
            kotlin.runCatching {
                transactionRepository.submitConfirmation(
                    executionInfo.safeTxHash,
                    credentialsRepository.signWithOwner(owner, executionInfo.safeTxHash.hexToByteArray())
                )
            }.onSuccess {
                tracker.logTransactionConfirmed()
                val safes = safeRepository.getSafes()

                var awaitingConfirm = false
                var rejectable = false
                var safeOwner = false
                if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                    awaitingConfirm = isAwaitingOwnerConfirmation(executionInfo, txDetails!!.txStatus)
                    rejectable = canBeRejectedFromDevice(executionInfo, txDetails!!.txStatus)
                    safeOwner = isOwner(executionInfo)
                }

                updateState { TransactionDetailsViewState(ConfirmationSubmitted(it.toTransactionDetailsViewData(safes), awaitingConfirm, rejectable, safeOwner)) }
            }.onFailure {
                throw TxConfirmationFailed(it.cause ?: it)
            }
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
    val txDetails: TransactionDetailsViewData?,
    var awaitingConfirm: Boolean,
    var rejectable: Boolean,
    var safeOwner: Boolean
) : BaseStateViewModel.ViewAction

data class ConfirmationSubmitted(
    val txDetails: TransactionDetailsViewData?,
    var awaitingConfirm: Boolean,
    var rejectable: Boolean,
    var safeOwner: Boolean
) : BaseStateViewModel.ViewAction

class TxConfirmationFailed(override val cause: Throwable) : Throwable(cause)
class TxRejectionFailed(override val cause: Throwable) : Throwable(cause)
object MismatchingSafeTxHash : Throwable()
object MissingOwnerCredential : Throwable()
object MissingCorrectExecutionDetailsException : Throwable()

