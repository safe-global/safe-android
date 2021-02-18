package io.gnosis.safe.ui.transactions.details

import androidx.annotation.VisibleForTesting
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TransactionDetails
import io.gnosis.data.models.transaction.TransactionInfo
import io.gnosis.data.models.transaction.TransactionStatus
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
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.math.BigInteger
import java.util.*
import javax.inject.Inject

class TransactionDetailsViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionDetailsViewState>(appDispatchers) {

    override fun initialState() = TransactionDetailsViewState(ViewAction.Loading(true))

    fun loadDetails(txId: String) {
        safeLaunch {
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            txDetails = transactionRepository.getTransactionDetails(txId)
            val safes = safeRepository.getSafes()
            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState { TransactionDetailsViewState(UpdateDetails(txDetails?.toTransactionDetailsViewData(safes))) }
        }
    }

    fun isAwaitingOwnerConfirmation(executionInfo: DetailedExecutionInfo.MultisigExecutionDetails, status: TransactionStatus): Boolean =
        status == TransactionStatus.AWAITING_CONFIRMATIONS &&
                ownerCredentialsRepository.hasCredentials() &&
                true == ownerCredentialsRepository.retrieveCredentials()?.let { credentials ->
            executionInfo.signers.contains(credentials.address) && !executionInfo.confirmations.map { it.signer }.contains(credentials.address)
        }


    var txDetails: TransactionDetails? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    fun submitRejection(transaction: TransactionDetails) {

        Timber.i("----> submitRejection()")

        val executionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
            ?: throw RuntimeException("MissingCorrectExecutionDetailsException")



        safeLaunch {
            val newExecutionInfo = DetailedExecutionInfo.MultisigExecutionDetails(
                submittedAt = Date(),
                nonce = executionInfo.nonce,
                safeTxHash = executionInfo.safeTxHash,// should not matter
                signers = emptyList(),
                confirmationsRequired = 0, // should not matter
                executor = null,
                confirmations = emptyList(),
                safeTxGas = BigInteger.ZERO,
                baseGas = BigInteger.ZERO,
                gasPrice = BigInteger.ZERO,
                gasToken = "0".asEthereumAddress()!!
            )
            val newTxDetails = TransactionDetails(
                txHash = null,
                txStatus = TransactionStatus.AWAITING_CONFIRMATIONS, // should not matter
                txInfo = TransactionInfo.Custom(safeRepository.getActiveSafe()!!.address, null, 0, BigInteger.ZERO, null, true),
                executedAt = null,
                txData = null,
                detailedExecutionInfo = newExecutionInfo
            )
            val safeTxHash =
                calculateSafeTxHash(
                    safeAddress = safeRepository.getActiveSafe()!!.address,
                    transaction = newTxDetails,
                    executionInfo = newExecutionInfo
                )!!.toHexString()     //TODO: TBD

            validateSafeTxHash(transaction, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }  //TODO: TBD
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            val ownerCredentials = ownerCredentialsRepository.retrieveCredentials() ?: run { throw MissingOwnerCredential }
            Timber.i("----> transactionRepository.proposeTransaction()")

            kotlin.runCatching {
                transactionRepository.proposeTransaction(
                    safeAddress = safeRepository.getActiveSafe()!!.address,
                    nonce = executionInfo.nonce,
                    signature = transactionRepository.sign(ownerCredentials.key, safeTxHash), //TODO: TBD
                    safeTxGas = 0,
                    safeTxHash = safeTxHash,
                    sender = ownerCredentials.address
                )
            }.onSuccess {
//                tracker.logTransactionConfirmed()  //TODO: TBD
//                val safes = safeRepository.getSafes()
                updateState { TransactionDetailsViewState(RejectionSubmitted) }
            }.onFailure {
                throw TxRejectionFailed(it)
            }
        }
    }

    fun submitConfirmation(transaction: TransactionDetails) {

        val executionInfo = txDetails?.detailedExecutionInfo as? DetailedExecutionInfo.MultisigExecutionDetails
            ?: throw RuntimeException("MissingCorrectExecutionDetailsException")

        safeLaunch {
            validateSafeTxHash(transaction, executionInfo).takeUnless { it }?.let { throw MismatchingSafeTxHash }
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            val ownerCredentials = ownerCredentialsRepository.retrieveCredentials() ?: run { throw MissingOwnerCredential }
            kotlin.runCatching {
                transactionRepository.submitConfirmation(
                    executionInfo.safeTxHash,
                    transactionRepository.sign(ownerCredentials.key, executionInfo.safeTxHash)
                )
            }.onSuccess {
                tracker.logTransactionConfirmed()
                val safes = safeRepository.getSafes()
                updateState { TransactionDetailsViewState(ConfirmationSubmitted(it.toTransactionDetailsViewData(safes))) }
            }.onFailure {
                throw TxConfirmationFailed(it)
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

class TxConfirmationFailed(override val cause: Throwable) : Throwable(cause)
class TxRejectionFailed(override val cause: Throwable) : Throwable(cause)
object MismatchingSafeTxHash : Throwable()
object MissingOwnerCredential : Throwable()
object RejectionSubmitted : BaseStateViewModel.ViewAction
