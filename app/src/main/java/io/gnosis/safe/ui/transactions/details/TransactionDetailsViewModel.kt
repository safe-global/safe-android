package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.utils.calculateSafeTxHash
import io.gnosis.safe.Tracker
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.OwnerCredentialsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.toHexString
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
            val txDetails = transactionRepository.getTransactionDetails(txId)
            val safes = safeRepository.getSafes()
            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState { TransactionDetailsViewState(UpdateDetails(txDetails.toTransactionDetailsViewData(safes))) }
        }
    }

    fun isAwaitingOwnerConfirmation(executionInfo: DetailedExecutionInfo.MultisigExecutionDetails, status: TransactionStatus): Boolean =
        status == TransactionStatus.AWAITING_CONFIRMATIONS &&
                ownerCredentialsRepository.hasCredentials() &&
                true == ownerCredentialsRepository.retrieveCredentials()?.let { credentials ->
            executionInfo.signers.contains(credentials.address) && !executionInfo.confirmations.map { it.signer }.contains(credentials.address)
        }

    fun submitConfirmation(transaction: TransactionDetailsViewData, executionInfo: DetailedExecutionInfo.MultisigExecutionDetails) {
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
        transaction: TransactionDetailsViewData,
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
object MismatchingSafeTxHash : Throwable()
object MissingOwnerCredential : Throwable()

class TransactionDetailsViewData(
    val txHash: String?,
    val txStatus: TransactionStatus,
    val txInfo: TransactionInfoViewData,
    val executedAt: Date?,
    val txData: TxData?,
    val detailedExecutionInfo: DetailedExecutionInfo?
)

sealed class TransactionInfoViewData(
    val type: TransactionType
) {
    data class Custom(
        val to: Solidity.Address,
        val toInfo: AddressInfo?,
        val dataSize: Int,
        val value: BigInteger,
        val methodName: String?
    ) : TransactionInfoViewData(TransactionType.Custom)

    data class SettingsChange(
        val dataDecoded: DataDecoded,
        val settingsInfo: SettingsInfo?
    ) : TransactionInfoViewData(TransactionType.SettingsChange)

    data class Transfer(
        val sender: Solidity.Address,
        val senderInfo: AddressInfo?,
        val recipient: Solidity.Address,
        val recipientInfo: AddressInfo?,
        val transferInfo: TransferInfo,
        val direction: TransactionDirection
    ) : TransactionInfoViewData(TransactionType.Transfer)

    data class Creation(
        val creator: Solidity.Address,
        val transactionHash: String,
        val implementation: Solidity.Address?,
        val factory: Solidity.Address?
    ) : TransactionInfoViewData(TransactionType.Creation)

    object Unknown : TransactionInfoViewData(TransactionType.Unknown)
}

fun TransactionDetails.toTransactionDetailsViewData(safes: List<Safe>): TransactionDetailsViewData =
    TransactionDetailsViewData(txHash, txStatus, txInfo.toTransactionInfoViewData(safes), executedAt, txData, detailedExecutionInfo)

private fun TransactionInfo.toTransactionInfoViewData(safes: List<Safe>): TransactionInfoViewData =
    when (this) {
        is TransactionInfo.Custom -> TransactionInfoViewData.Custom(to, toInfo, dataSize, value, methodName) // TODO convert toInfo
        is TransactionInfo.Creation -> TransactionInfoViewData.Creation(creator, transactionHash, implementation, factory)
        is TransactionInfo.SettingsChange -> TransactionInfoViewData.SettingsChange(dataDecoded, settingsInfo) //TODO settingsInfo.toXXX
        is TransactionInfo.Transfer -> TransactionInfoViewData.Transfer(sender, senderInfo, recipient, recipientInfo, transferInfo, direction)  // TODO convert senderInfo & recipientInfo
        is TransactionInfo.Unknown -> TransactionInfoViewData.Unknown
    }



