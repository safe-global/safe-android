package io.gnosis.safe.ui.transactions.details

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Owner
import io.gnosis.data.models.Safe
import io.gnosis.data.models.TransactionLocal
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionStatus
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionLocalRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.SemVer
import io.gnosis.data.utils.calculateSafeTxHash
import io.gnosis.data.utils.toSignatureString
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.HeimdallApplication.Companion.LEDGER_EXECUTION
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
    private val transactionLocalRepository: TransactionLocalRepository,
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

            val activeSafe = safeRepository.getActiveSafe()!!
            val safeInfo = activeSafe?.let { safeRepository.getSafeInfo(it) }

            txDetails = transactionRepository.getTransactionDetails(
                activeSafe.chainId,
                txId
            )
            var txLocal: TransactionLocal? = null

            val safes = safeRepository.getSafes()

            val executionInfo = txDetails?.detailedExecutionInfo
            val owners = credentialsRepository.owners()

            var canSign = false
            var canExecute = false
            var safeOwner = false
            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                canSign = canBeSignedFromDevice(executionInfo, owners)
                canExecute = canBeExecutedFromDevice(executionInfo, owners)
                nextInLine = safeInfo.nonce == executionInfo.nonce
                safeOwner = isOwner(executionInfo, owners)
                txLocal = transactionLocalRepository.updateLocalTx(activeSafe, executionInfo.safeTxHash)
            }

            var txDetailsViewData = txDetails?.toTransactionDetailsViewData(
                safes = safes,
                canSign = canSign,
                canExecute = canExecute,
                owners = owners,
                nextInLine = nextInLine,
                hasOwnerKey = safeOwner
            )

            txLocal?.let {
                when {
                    txDetailsViewData?.txStatus == TransactionStatus.AWAITING_EXECUTION -> {
                        txDetailsViewData = txDetailsViewData?.copy(txStatus = TransactionStatus.PENDING)
                    }
                    txDetailsViewData?.txStatus == TransactionStatus.SUCCESS ||
                            txDetailsViewData?.txStatus == TransactionStatus.FAILED -> {
                        // tx was indexed by the transaction service
                        // local transaction can be deleted
                        transactionLocalRepository.delete(it)
                    }
                }
            }

            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState {
                TransactionDetailsViewState(
                    UpdateDetails(
                        txDetailsViewData
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
                    executionInfo.signers.map { it.value }
                        .contains(owner.address) && !executionInfo.confirmations.map { it.signer.value }
                        .contains(owner.address)
                }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun canBeSignedFromDevice(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        localOwners: List<Owner>
    ): Boolean {
        val signedBy = executionInfo.confirmations.map { it.signer.value }
        val possibleSigners = executionInfo.signers.map { it.value }
        return localOwners.isNotEmpty() &&
                localOwners.any { owner ->
                    possibleSigners.contains(owner.address) && !signedBy.contains(owner.address)
                }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun canBeExecutedFromDevice(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        localOwners: List<Owner>
    ): Boolean {
        val ownersThatCanExecute = localOwners.filter {
            //TODO: [Ledger execution] remove after successfully tested
            if (LEDGER_EXECUTION) {
                executionInfo.signers.contains(AddressInfo(it.address))
            } else {
                executionInfo.signers.contains(AddressInfo(it.address)) && it.type != Owner.Type.LEDGER_NANO_X
            }
        }
        return ownersThatCanExecute.isNotEmpty() && executionInfo.confirmations.size >= executionInfo.confirmationsRequired
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun isOwner(
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails,
        owners: List<Owner>
    ): Boolean {
        return owners.isNotEmpty() &&
                owners.any { owner ->
                    executionInfo.signers.map { it.value }.contains(owner.address)
                }
    }

    var txDetails: TransactionDetails? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    private var nextInLine = false

    fun resumeFlow(selectedOwnerAddress: Solidity.Address, signedSafeTxHash: String? = null) {
        safeLaunch {
            // update ui without reloading tx details
            val safes = safeRepository.getSafes()

            val executionInfo = txDetails?.detailedExecutionInfo

            val owners = credentialsRepository.owners()
            val addressInfoIndex = txDetails?.txData?.addressInfoIndex ?: emptyMap()

            var canSign = false
            var canExecute = false
            var safeOwner = false
            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                canSign = canBeSignedFromDevice(executionInfo, owners)
                canExecute = canBeExecutedFromDevice(executionInfo, owners)
                safeOwner = isOwner(executionInfo, owners)
            }

            updateState {
                TransactionDetailsViewState(
                    UpdateDetails(
                        txDetails?.toTransactionDetailsViewData(
                            safes = safes,
                            canSign = canSign,
                            canExecute = canExecute,
                            nextInLine = nextInLine,
                            owners = owners,
                            hasOwnerKey = safeOwner
                        )
                    )
                )
            }

            if (signedSafeTxHash != null) {
                // no need to additionally protect device key with a passcode
                submitConfirmation(txDetails!!, selectedOwnerAddress, signedSafeTxHash)
            } else if (!settingsHandler.usePasscode || !settingsHandler.requirePasscodeForConfirmations || (settingsHandler.requirePasscodeForConfirmations && credentialsRepository.credentialsUnlocked())) {
                submitConfirmation(txDetails!!, selectedOwnerAddress)
            }
        }
    }

    fun startConfirmationFlow() {
        safeLaunch {
            val safe = safeRepository.getActiveSafe()!!
            val executionInfo = txDetails?.detailedExecutionInfo
            if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
                val allPossibleSigners = executionInfo.signers
                val confirmations = executionInfo.confirmations
                val missingSigners = allPossibleSigners.filter { possibleSigner ->
                    confirmations.all { confirmation ->
                        confirmation.signer.value != possibleSigner.value
                    }
                }
                //FIXME: validate safeTxHash here and abort flow if validation fails
                updateState {
                    TransactionDetailsViewState(
                        ViewAction.NavigateTo(
                            TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToSigningOwnerSelectionFragment(
                                missingSigners = missingSigners.map {
                                    it.value.asEthereumAddressString()
                                }.toTypedArray(),
                                signingMode = SigningMode.CONFIRMATION,
                                chain = safe.chain,
                                safeTxHash = executionInfo.safeTxHash
                            )
                        )
                    )
                }
                updateState { TransactionDetailsViewState(ViewAction.None) }
            }
        }
    }

    fun submitConfirmation(
        transaction: TransactionDetails,
        selectedOwnerKey: Solidity.Address,
        signedSafeTxHash: String? = null
    ) {

        val executionInfo =
            txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
                ?: throw MissingCorrectExecutionDetailsException

        safeLaunch {

            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }

            val safe = safeRepository.getActiveSafe()!!

            //FIXME: validate safeTxHash in startConfirmationFlow
            validateSafeTxHash(safe, transaction, executionInfo).takeUnless { it }
                ?.let { throw MismatchingSafeTxHash }

            if (credentialsRepository.ownerCount() == 0) {
                throw MissingOwnerCredential
            }

            val selectedOwner = credentialsRepository.owner(selectedOwnerKey)
            val owners = credentialsRepository.owners()
            kotlin.runCatching {
                transactionRepository.submitConfirmation(
                    chainId = safe.chainId,
                    safeTxHash = executionInfo.safeTxHash,
                    signedSafeTxHash = signedSafeTxHash ?: credentialsRepository.signWithOwner(
                        selectedOwner!!,
                        executionInfo.safeTxHash.hexToByteArray()
                    ).toSignatureString()
                )
            }.onSuccess {
                txDetails = it
                val newExecutionInfo =
                    txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
                        ?: throw MissingCorrectExecutionDetailsException
                tracker.logTransactionConfirmed(safe.chainId)
                val safes = safeRepository.getSafes()

                val canSign = canBeSignedFromDevice(newExecutionInfo, owners)
                val canExecute = canBeExecutedFromDevice(newExecutionInfo, owners)
                val safeOwner = isOwner(newExecutionInfo, owners)

                updateState {
                    TransactionDetailsViewState(
                        ConfirmationSubmitted(
                            it.toTransactionDetailsViewData(
                                safes = safes,
                                canSign = canSign,
                                canExecute = canExecute,
                                nextInLine = nextInLine,
                                owners = owners,
                                hasOwnerKey = safeOwner
                            )
                        )
                    )
                }
            }.onFailure {
                throw TxConfirmationFailed(it.cause ?: it)
            }
        }
    }

    private suspend fun validateSafeTxHash(
        safe: Safe,
        transaction: TransactionDetails,
        executionInfo: DetailedExecutionInfo.MultisigExecutionDetails
    ): Boolean {
        return kotlin.runCatching {
            val contractVersion = safe.version?.let {
                SemVer.parse(it)
            } ?: SemVer(0, 0, 0)
            val safeTxHash = executionInfo.safeTxHash
            val calculatedSafeTxHash =
                calculateSafeTxHash(
                    contractVersion,
                    safe.chainId,
                    safe.address,
                    transaction,
                    executionInfo
                ).toHexString().addHexPrefix()
            safeTxHash == calculatedSafeTxHash
        }.getOrDefault(false)
    }

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled() = settingsHandler.chainPrefixCopy
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
