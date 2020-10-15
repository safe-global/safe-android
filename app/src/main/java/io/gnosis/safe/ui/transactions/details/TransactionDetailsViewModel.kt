package io.gnosis.safe.ui.transactions.details

import io.gnosis.data.models.DetailedExecutionInfo
import io.gnosis.data.models.TransactionDetails
import io.gnosis.data.models.TransactionStatus
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.OwnerCredentialsRepository
import javax.inject.Inject

class TransactionDetailsViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
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
}

data class TransactionDetailsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateDetails(
    val txDetails: TransactionDetails?
) : BaseStateViewModel.ViewAction
