package io.gnosis.safe.ui.transactions.details

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.Owner
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
import io.gnosis.safe.ui.settings.app.SettingsHandler
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
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionDetailsViewState>(appDispatchers) {

    private var confirmationInProgress = false

    override fun initialState() = TransactionDetailsViewState(ViewAction.Loading(true))

    fun loadDetails(txId: String) {
        safeLaunch {
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            txDetails = transactionRepository.getTransactionDetails(txId)
            val safes = safeRepository.getSafes()

            val executionInfo = txDetails?.detailedExecutionInfo

            val owners = credentialsRepository.owners()

            var awaitingConfirm = false
            var rejectable = false
            var safeOwner = false
            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                awaitingConfirm = isAwaitingOwnerConfirmation(executionInfo, txDetails!!.txStatus, owners)
                rejectable = canBeRejectedFromDevice(executionInfo, txDetails!!.txStatus, owners)
                safeOwner = isOwner(executionInfo, owners)
            }

            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState {
                TransactionDetailsViewState(
                    UpdateDetails(
                        txDetails?.toTransactionDetailsViewData(safes),
                        awaitingConfirm,
                        rejectable,
                        safeOwner
                    )
                )
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun isAwaitingOwnerConfirmation(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        status: TransactionStatus,
        owners: List<Owner>
    ): Boolean =
        status == TransactionStatus.AWAITING_CONFIRMATIONS &&
                owners.isNotEmpty() &&
                owners[0].let { owner ->
                    executionInfo.signers.contains(owner.address) && !executionInfo.confirmations.map { it.signer }.contains(owner.address)
                }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun canBeRejectedFromDevice(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        status: TransactionStatus,
        owners: List<Owner>
    ): Boolean = !status.isCompleted() &&
            owners.isNotEmpty() &&
            owners.any { credentials ->
                !executionInfo.rejectors.contains(credentials.address)
            }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun isOwner(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        owners: List<Owner>
    ): Boolean {
        return owners.isNotEmpty() &&
                owners[0].let { owner ->
                    executionInfo.signers.contains(owner.address)
                }
    }

    var txDetails: TransactionDetails? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    fun flowInProgress(): Boolean {
        return confirmationInProgress
    }

    fun resumeFlow() {
        safeLaunch {
            if (confirmationInProgress) {

                // update ui without reloading tx details

                val safes = safeRepository.getSafes()

                val executionInfo = txDetails?.detailedExecutionInfo

                val owners = credentialsRepository.owners()

                var awaitingConfirm = false
                var rejectable = false
                var safeOwner = false
                if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                    awaitingConfirm = isAwaitingOwnerConfirmation(executionInfo, txDetails!!.txStatus, owners)
                    rejectable = canBeRejectedFromDevice(executionInfo, txDetails!!.txStatus, owners)
                    safeOwner = isOwner(executionInfo, owners)
                }

                updateState {
                    TransactionDetailsViewState(
                        UpdateDetails(
                            txDetails?.toTransactionDetailsViewData(safes),
                            awaitingConfirm,
                            rejectable,
                            safeOwner
                        )
                    )
                }

                if (credentialsRepository.credentialsUnlocked()) {
                    submitConfirmation(txDetails!!)
                }

                confirmationInProgress = false
            }
        }
    }

    fun startConfirmationFlow() {
        safeLaunch {
            if (settingsHandler.usePasscode) {
                confirmationInProgress = true
                updateState {
                    TransactionDetailsViewState(
                        ViewAction.NavigateTo(
                            TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToEnterPasscodeFragment()
                        )
                    )
                }
                updateState { TransactionDetailsViewState(ViewAction.None) }
            } else {
                updateState { TransactionDetailsViewState(ConfirmConfirmation) }
            }
        }
    }

    fun submitConfirmation(transaction: TransactionDetails) {
        val executionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
            ?: throw MissingCorrectExecutionDetailsException

        safeLaunch {
            validateSafeTxHash(transaction, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            if (credentialsRepository.ownerCount() == 0) {
                throw MissingOwnerCredential
            }
            val owners = credentialsRepository.owners()
            //TODO: unlock with passcode if passcode is enabled
            kotlin.runCatching {
                transactionRepository.submitConfirmation(
                    executionInfo.safeTxHash,
                    credentialsRepository.signWithOwner(owners[0], executionInfo.safeTxHash.hexToByteArray())
                )
            }.onSuccess {
                txDetails = it
                tracker.logTransactionConfirmed()
                val safes = safeRepository.getSafes()

                var awaitingConfirm = false
                var rejectable = false
                var safeOwner = false
                if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                    awaitingConfirm = isAwaitingOwnerConfirmation(executionInfo, txDetails!!.txStatus, owners)
                    rejectable = canBeRejectedFromDevice(executionInfo, txDetails!!.txStatus, owners)
                    safeOwner = isOwner(executionInfo, owners)
                }

                updateState {
                    TransactionDetailsViewState(
                        ConfirmationSubmitted(
                            it.toTransactionDetailsViewData(safes),
                            awaitingConfirm,
                            rejectable,
                            safeOwner
                        )
                    )
                }
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

object ConfirmConfirmation : BaseStateViewModel.ViewAction

class TxConfirmationFailed(override val cause: Throwable) : Throwable(cause)
class TxRejectionFailed(override val cause: Throwable) : Throwable(cause)
object MismatchingSafeTxHash : Throwable()
object MissingOwnerCredential : Throwable()
object MissingCorrectExecutionDetailsException : Throwable()
object WrongPasscode : Throwable()

