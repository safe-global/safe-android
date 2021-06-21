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
import pm.gnosis.model.Solidity
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.asEthereumAddressString
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

    override fun initialState() = TransactionDetailsViewState(ViewAction.Loading(true))

    fun loadDetails(txId: String) {
        safeLaunch {
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            txDetails = transactionRepository.getTransactionDetails(txId)
            val safes = safeRepository.getSafes()

            val executionInfo = txDetails?.detailedExecutionInfo
            val owners = credentialsRepository.owners()

            var canSign = false
            var safeOwner = false
            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                canSign = canBeSignedFromDevice(executionInfo, owners)
                safeOwner = isOwner(executionInfo, owners)
            }

            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState {
                TransactionDetailsViewState(
                    UpdateDetails(
                        txDetails?.toTransactionDetailsViewData(safes = safes, canSign = canSign, hasOwnerKey = safeOwner, owners = owners)
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
                owners.any { owner ->
                    executionInfo.signers.contains(owner.address) && !executionInfo.confirmations.map { it.signer }.contains(owner.address)
                }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun canBeSignedFromDevice(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        localOwners: List<Owner>
    ): Boolean {
        val signedBy = executionInfo.confirmations.map { it.signer }
        val possibleSigners = executionInfo.signers
        return localOwners.isNotEmpty() &&
                localOwners.any { owner ->
                    possibleSigners.contains(owner.address) && !signedBy.contains(owner.address)
                }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun isOwner(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        owners: List<Owner>
    ): Boolean {
        return owners.isNotEmpty() &&
                owners.any { owner ->
                    executionInfo.signers.contains(owner.address)
                }
    }

    var txDetails: TransactionDetails? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    fun resumeFlow(selectedOwnerAddress: Solidity.Address) {
        safeLaunch {
            // update ui without reloading tx details
            val safes = safeRepository.getSafes()

            val executionInfo = txDetails?.detailedExecutionInfo

            val owners = credentialsRepository.owners()

            var canSign = false
            var safeOwner = false
            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                canSign = canBeSignedFromDevice(executionInfo, owners)
                safeOwner = isOwner(executionInfo, owners)
            }

            updateState {
                TransactionDetailsViewState(
                    UpdateDetails(
                        txDetails?.toTransactionDetailsViewData(safes = safes, canSign = canSign, hasOwnerKey = safeOwner, owners = owners)
                    )
                )
            }

            if (!settingsHandler.requirePasscodeForConfirmations || (settingsHandler.requirePasscodeForConfirmations && credentialsRepository.credentialsUnlocked())) {
                submitConfirmation(txDetails!!, selectedOwnerAddress)
            }
        }
    }

    fun startConfirmationFlow() {
        safeLaunch {
            val executionInfo = txDetails?.detailedExecutionInfo
            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                val allPossibleSigners = executionInfo.signers
                val confirmations = executionInfo.confirmations
                val missingSigners = allPossibleSigners.filter { possibleSigner ->
                    confirmations.all { confirmation ->
                        confirmation.signer != possibleSigner
                    }
                }
                updateState {
                    TransactionDetailsViewState(
                        ViewAction.NavigateTo(
                            TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToSigningOwnerSelectionFragment(
                                missingSigners = missingSigners.map {
                                    it.asEthereumAddressString()
                                }.toTypedArray()
                            )
                        )
                    )
                }
                updateState { TransactionDetailsViewState(ViewAction.None) }
            }
        }
    }

    fun submitConfirmation(transaction: TransactionDetails, selectedOwnerKey: Solidity.Address) {
        val executionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
            ?: throw MissingCorrectExecutionDetailsException

        safeLaunch {
            validateSafeTxHash(transaction, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            if (credentialsRepository.ownerCount() == 0) {
                throw MissingOwnerCredential
            }
            val selectedOwner = credentialsRepository.owner(selectedOwnerKey)
            val owners = credentialsRepository.owners()
            kotlin.runCatching {
                transactionRepository.submitConfirmation(
                    executionInfo.safeTxHash,
                    credentialsRepository.signWithOwner(selectedOwner!!, executionInfo.safeTxHash.hexToByteArray())
                )
            }.onSuccess {
                txDetails = it
                val newExecutionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
                    ?: throw MissingCorrectExecutionDetailsException
                tracker.logTransactionConfirmed()
                val safes = safeRepository.getSafes()

                val canSign = canBeSignedFromDevice(newExecutionInfo, owners)
                val safeOwner = isOwner(newExecutionInfo, owners)

                updateState {
                    TransactionDetailsViewState(
                        ConfirmationSubmitted(
                            it.toTransactionDetailsViewData(safes = safes, canSign = canSign, hasOwnerKey = safeOwner, owners = owners)
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
    val txDetails: TransactionDetailsViewData?
) : BaseStateViewModel.ViewAction

data class ConfirmationSubmitted(
    val txDetails: TransactionDetailsViewData?
) : BaseStateViewModel.ViewAction

data class ConfirmConfirmation(
    val owner: Solidity.Address
) : BaseStateViewModel.ViewAction

class TxConfirmationFailed(override val cause: Throwable) : Throwable(cause)
class TxRejectionFailed(override val cause: Throwable) : Throwable(cause)
object MismatchingSafeTxHash : Throwable()
object MissingOwnerCredential : Throwable()
object MissingCorrectExecutionDetailsException : Throwable()
object WrongPasscode : Throwable()

