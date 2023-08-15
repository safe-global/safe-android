package io.gnosis.safe.ui.transactions.details

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionInfo
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.SemVer
import io.gnosis.data.utils.calculateSafeTxHash
import io.gnosis.data.utils.toSignatureString
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.model.Solidity
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
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

    fun resumeFlow(owner: Solidity.Address, signedSafeTxHash: String? = null) {
        if (signedSafeTxHash != null) {
            // no need to additionally protect device key with a passcode
            submitRejection(owner, signedSafeTxHash)
        } else if (!settingsHandler.usePasscode || !settingsHandler.requirePasscodeForConfirmations || (settingsHandler.requirePasscodeForConfirmations && credentialsRepository.credentialsUnlocked())) {
            submitRejection(owner)
        }
    }

    fun startRejectionFlow(owner: Solidity.Address) {
        safeLaunch {
            if (settingsHandler.usePasscode) {
                updateState {
                    ConfirmationRejectedViewState(
                        ViewAction.NavigateTo(
                            ConfirmRejectionFragmentDirections.actionConfirmRejectionFragmentToEnterPasscodeFragment(selectedOwner = owner.asEthereumAddressString())
                        )
                    )
                }
            } else {
                updateState { ConfirmationRejectedViewState(ConfirmRejection(owner = owner)) }
            }
        }
    }

    fun submitRejection(owner: Solidity.Address, signedSafeTxHash: String? = null) {

        val executionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
            ?: throw MissingCorrectExecutionDetailsException

        safeLaunch {

            val safe = safeRepository.getActiveSafe()!!
            val contractVersion = safe.version?.let {
                SemVer.parse(it)
            } ?: SemVer(0, 0, 0)

            //FIXME: validate earlier
            validateSafeTxHash(safe, txDetails!!, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }

            val rejectionExecutionInfo = DetailedExecutionInfo.MultisigExecutionDetails(nonce = executionInfo.nonce)
            val rejectionTxDetails = TransactionDetails(
                txInfo = TransactionInfo.Custom(to = AddressInfo(safe.address)),
                detailedExecutionInfo = rejectionExecutionInfo,
                safeAppInfo = null
            )
            val safeTxHash =
                calculateSafeTxHash(
                    implementationVersion = contractVersion,
                    chainId = safe.chainId,
                    safeAddress = safe.address,
                    transaction = rejectionTxDetails,
                    executionInfo = rejectionExecutionInfo
                ).toHexString()

            val selectedOwner = credentialsRepository.owner(owner) ?: throw MissingOwnerCredential
            kotlin.runCatching {
                transactionRepository.proposeTransaction(
                    chainId = safe.chainId,
                    safeAddress = safe.address,
                    toAddress = safe.address,
                    nonce = rejectionExecutionInfo.nonce,
                    signature = signedSafeTxHash ?: credentialsRepository.signWithOwner(selectedOwner, safeTxHash.hexToByteArray()).toSignatureString(),
                    safeTxGas = rejectionExecutionInfo.safeTxGas.toLong(),
                    safeTxHash = safeTxHash,
                    sender = selectedOwner.address
                )
            }.onSuccess {
                tracker.logTransactionRejected(safe.chainId)
                updateState { ConfirmationRejectedViewState(RejectionSubmitted) }
            }.onFailure {
                throw TxRejectionFailed(it.cause ?: it)
            }
        }
    }

    fun loadDetails(txId: String) {
        if (txDetails == null) {
            safeLaunch {
                updateState { ConfirmationRejectedViewState(ViewAction.Loading(true)) }
                txDetails = transactionRepository.getTransactionDetails(safeRepository.getActiveSafe()!!.chainId, txId)
                updateState { ConfirmationRejectedViewState(ViewAction.Loading(false)) }
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
            val calculatedSafeTxHash = calculateSafeTxHash(contractVersion, safe.chainId, safe.address, transaction, executionInfo).toHexString().addHexPrefix()
            safeTxHash == calculatedSafeTxHash
        }.getOrDefault(false)
    }

    fun selectSigningOwner() {
        val executionInfo = txDetails?.detailedExecutionInfo
        if (executionInfo is DetailedExecutionInfo.MultisigExecutionDetails) {
            val allPossibleSigners = executionInfo.signers
            val rejectors = executionInfo.rejectors?.map { it.value } ?: emptyList()
            val missingSigners = allPossibleSigners.filter { possibleSigner ->
                rejectors.all { address ->
                    address != possibleSigner.value
                }
            }

            safeLaunch {
                //FIXME: refactor this section. tx hash of the rejection transaction needs to be propagated for signing on ledger device
                val safe = safeRepository.getActiveSafe()!!
                val contractVersion = safe.version?.let {
                    SemVer.parse(it)
                } ?: SemVer(0, 0, 0)

                val rejectionExecutionInfo = DetailedExecutionInfo.MultisigExecutionDetails(nonce = executionInfo.nonce)
                val rejectionTxDetails = TransactionDetails(
                    txInfo = TransactionInfo.Custom(to = AddressInfo(safe.address)),
                    detailedExecutionInfo = rejectionExecutionInfo,
                    safeAppInfo = null
                )
                val rejectionTxHash =
                    calculateSafeTxHash(
                        implementationVersion = contractVersion,
                        chainId = safe.chainId,
                        safeAddress = safe.address,
                        transaction = rejectionTxDetails,
                        executionInfo = rejectionExecutionInfo
                    ).toHexString()
                // ----------------

                updateState {
                    ConfirmationRejectedViewState(
                        ViewAction.NavigateTo(
                            ConfirmRejectionFragmentDirections.actionConfirmRejectionFragmentToSigningOwnerSelectionFragment(
                                missingSigners = missingSigners.map {
                                    it.value.asEthereumAddressString()
                                }.toTypedArray(),
                                signingMode = SigningMode.REJECTION,
                                chain = safe.chain,
                                safeTxHash = rejectionTxHash
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
