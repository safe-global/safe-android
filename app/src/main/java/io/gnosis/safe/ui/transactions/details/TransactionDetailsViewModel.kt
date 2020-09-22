package io.gnosis.safe.ui.transactions.details

import com.squareup.moshi.Moshi
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.models.TransactionDetails
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class TransactionDetailsViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    moshi: Moshi,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionDetailsViewState>(appDispatchers) {

    private val dataDecodedAdapter = moshi.adapter(DataDecodedDto::class.java)

    override fun initialState() = TransactionDetailsViewState(ViewAction.Loading(true))

    fun loadDetails(txId: String) {
        safeLaunch {
            updateState { TransactionDetailsViewState(ViewAction.Loading(true)) }
            val txDetails = transactionRepository.getTransactionDetails(txId)
            updateState { TransactionDetailsViewState(ViewAction.Loading(false)) }
            updateState { TransactionDetailsViewState(UpdateDetails(txDetails)) }
        }
    }

    fun handleDetailAction(decodedData: DataDecodedDto) {
        safeLaunch {
            updateState {
                TransactionDetailsViewState(
                    ViewAction.NavigateTo(
                        TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToTransactionDetailsActionFragment(
                            dataDecodedAdapter.toJson(
                                decodedData
                            )
                        )
                    )
                )
            }
            updateState { TransactionDetailsViewState(ViewAction.None) }
        }
    }
}

data class TransactionDetailsViewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class UpdateDetails(
    val txDetails: TransactionDetails?
) : BaseStateViewModel.ViewAction
