package io.gnosis.safe.ui.transactions.execution

import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class TxEditFeeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TxEditFeeState>(appDispatchers) {

    override fun initialState() = TxEditFeeState(
        saveEnabled = false,
        viewAction = null
    )

    fun validate1559Inputs(
        nonceValue: String?,
        gasLimitValue: String?,
        maxPriorityFeeValue: String?,
        maxFeeValue: String?
    ) {

    }

    fun validateLegacyInputs(
        nonceValue: String?,
        gasLimitValue: String?,
        gasPriceValue: String?
    ) {

    }

    fun loadEstimation() {
        //TODO load estimation
    }
}

data class TxEditFeeState(
    val saveEnabled: Boolean,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class Validate1559FeeData(
    val nonceError: String?,
    val gasLimitError: String?,
    val maxPriorityFeeError: String?,
    val maxFeeError: String?
) : BaseStateViewModel.ViewAction

data class ValidateLegacyFeeData(
    val nonceError: String?,
    val gasLimitError: String?,
    val gasPriceError: String?
) : BaseStateViewModel.ViewAction

data class UpdateEstimation(
    val estimation: String
) : BaseStateViewModel.ViewAction
