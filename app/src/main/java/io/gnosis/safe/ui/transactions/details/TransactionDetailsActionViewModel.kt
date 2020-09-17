package io.gnosis.safe.ui.transactions.details

import com.squareup.moshi.Moshi
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class TransactionDetailsActionViewModel
@Inject constructor(
    moshi: Moshi,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TransactionDetailsActionViewState>(appDispatchers) {

    private val dataDecodedAdapter = moshi.adapter(DataDecodedDto::class.java)

    override fun initialState() = TransactionDetailsActionViewState(null, ViewAction.Loading(true))

    fun getDecodedData(decodedDataString: String) {
        safeLaunch {
            val decodedData = dataDecodedAdapter.fromJson(decodedDataString)
            updateState { TransactionDetailsActionViewState(decodedData, ViewAction.Loading(false)) }
        }
    }
}

data class TransactionDetailsActionViewState(
    val dataDecoded: DataDecodedDto?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
