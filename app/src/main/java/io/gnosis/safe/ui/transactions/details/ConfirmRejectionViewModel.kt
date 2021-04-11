package io.gnosis.safe.ui.transactions.details

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionInfo
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.calculateSafeTxHash
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.model.Solidity
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject

class ConfirmRejectionViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ConfirmationRejectedViewState>(appDispatchers) {

    var txDetails: TransactionDetails? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    override fun initialState(): ConfirmationRejectedViewState = ConfirmationRejectedViewState(ViewAction.Loading(true))

    fun resumeFlow(owner: Solidity.Address) {
        Timber.e("---> resumeFlow(): owner: $owner")

        if (!settingsHandler.usePasscode || (settingsHandler.usePasscode && credentialsRepository.credentialsUnlocked())) {
            submitRejection(owner)
        }
    }

    fun startRejectionFlow(owner: Solidity.Address) {
        safeLaunch {

//            updateState { ConfirmationRejectedViewState(ViewAction.NavigateTo(ConfirmRejectionFragmentDirections.actionConfirmRejectionFragmentToSigningOwnerSelectionFragment())) }

            if (settingsHandler.usePasscode) {
                updateState {
                    ConfirmationRejectedViewState(
                        ViewAction.NavigateTo(
                            ConfirmRejectionFragmentDirections.actionConfirmRejectionFragmentToEnterPasscodeFragment(selectedOwner = owner.asEthereumAddressString())
                        )
                    )
                }
                updateState { ConfirmationRejectedViewState(ViewAction.None) }
            } else {
                updateState { ConfirmationRejectedViewState(ConfirmRejection(owner = owner)) }
            }
        }
    }

    fun submitRejection(owner: Solidity.Address) {
        val executionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
            ?: throw MissingCorrectExecutionDetailsException

        safeLaunch {
            val activeSafeAddress = safeRepository.getActiveSafe()!!.address
            val rejectionExecutionInfo = DetailedExecutionInfo.MultisigExecutionDetails(nonce = executionInfo.nonce)
            val rejectionTxDetails = TransactionDetails(
                txInfo = TransactionInfo.Custom(to = activeSafeAddress),
                detailedExecutionInfo = rejectionExecutionInfo,
                safeAppInfo = null
            )
            val safeTxHash =
                calculateSafeTxHash(
                    safeAddress = activeSafeAddress,
                    transaction = rejectionTxDetails,
                    executionInfo = rejectionExecutionInfo
                )!!.toHexString()
            validateSafeTxHash(txDetails!!, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }

            val selectedOwner = credentialsRepository.owner(owner) ?: throw MissingOwnerCredential
            kotlin.runCatching {
                transactionRepository.proposeTransaction(
                    safeAddress = activeSafeAddress,
                    nonce = rejectionExecutionInfo.nonce,
                    signature = credentialsRepository.signWithOwner(selectedOwner, safeTxHash.hexToByteArray()),
                    safeTxGas = rejectionExecutionInfo.safeTxGas.toLong(),
                    safeTxHash = safeTxHash,
                    sender = selectedOwner.address
                )
            }.onSuccess {
                tracker.logTransactionRejected()
                updateState { ConfirmationRejectedViewState(RejectionSubmitted) }
            }.onFailure {
                throw TxRejectionFailed(it.cause ?: it)
            }
        }
    }

    fun loadDetails(txId: String) {
        safeLaunch {
            updateState { ConfirmationRejectedViewState(ViewAction.Loading(true)) }
            txDetails = transactionRepository.getTransactionDetails(txId)
            updateState { ConfirmationRejectedViewState(ViewAction.Loading(false)) }
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

    fun selectSigningOwner() {
        val executionInfo = txDetails?.detailedExecutionInfo
        if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
            val allPossibleSigners = executionInfo.signers
            val rejectors = executionInfo.rejectors
            val missingSigners = allPossibleSigners.filter { possibleSigner ->
                rejectors.all { address ->
                    address != possibleSigner
                }
            }
            safeLaunch {
                updateState {
                    ConfirmationRejectedViewState(
                        ViewAction.NavigateTo(
                            ConfirmRejectionFragmentDirections.actionConfirmRejectionFragmentToSigningOwnerSelectionFragment(
                                missingSigners = missingSigners.map {
                                    it.asEthereumAddressString()
                                }.toTypedArray(), isConfirmation = false
                            )
                        )
                    )
                }
                updateState { ConfirmationRejectedViewState(ViewAction.None) }
            }
        }
    }
}

open class ConfirmationRejectedViewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object RejectionSubmitted : BaseStateViewModel.ViewAction

data class ConfirmRejection(
    val owner: Solidity.Address
) : BaseStateViewModel.ViewAction
