package io.gnosis.safe.ui.transactions.execution

import android.content.Context
import io.gnosis.data.models.Chain
import io.gnosis.safe.R
import io.gnosis.safe.qrscanner.nullOnThrow
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import java.math.BigInteger
import javax.inject.Inject

class TxEditFeeViewModel @Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<TxEditFeeState>(appDispatchers) {

    override fun initialState() = TxEditFeeState(
        saveEnabled = false,
        viewAction = null
    )

    fun validate1559Inputs(
        context: Context,
        minNonce: BigInteger,
        nonceValue: String?,
        gasLimitValue: String?,
        maxPriorityFeeValue: String?,
        maxFeeValue: String?
    ) {

        val nonceError = if (nonceValue.isNullOrBlank()) {
            context.getString(R.string.tx_exec_error_value_required)
        } else {
            val nonce = nullOnThrow { BigInteger(nonceValue) }
            if (nonce == null || nonce < BigInteger.ZERO) {
                context.getString(R.string.tx_exec_error_value_nan)
            } else {
                if (nonce < minNonce) {
                    context.getString(R.string.tx_exec_nonce_error_executed)
                } else {
                    null
                }
            }
        }

        val gasLimitError = if (gasLimitValue.isNullOrBlank()) {
            context.getString(R.string.tx_exec_error_value_required)
        } else {
            val gasLimit = nullOnThrow { BigInteger(gasLimitValue) }
            if (gasLimit == null || gasLimit < BigInteger.ZERO) {
                context.getString(R.string.tx_exec_error_value_nan)
            } else {
                null
            }
        }

        val maxPriorityFee = nullOnThrow { BigInteger(maxPriorityFeeValue) }
        val maxFee = nullOnThrow { BigInteger(maxFeeValue) }

        val maxPriorityFeeError = if (maxPriorityFeeValue.isNullOrBlank()) {
            context.getString(R.string.tx_exec_error_value_required)
        } else {
            if (maxPriorityFee == null || maxPriorityFee < BigInteger.ZERO) {
                context.getString(R.string.tx_exec_error_value_nan)
            } else {
                if (maxPriorityFee == BigInteger.ZERO) {
                    context.getString(R.string.tx_exec_error_value_zero)
                } else if (maxPriorityFee > maxFee) {
                    context.getString(R.string.tx_exec_max_priority_fee_error_too_big)
                } else {
                    null
                }
            }
        }

        val maxFeeError = if (maxPriorityFeeValue.isNullOrBlank()) {
            context.getString(R.string.tx_exec_error_value_required)
        } else {
            if (maxFee == null || maxFee < BigInteger.ZERO) {
                context.getString(R.string.tx_exec_error_value_nan)
            } else {
                if (maxFee == BigInteger.ZERO) {
                    context.getString(R.string.tx_exec_error_value_zero)
                } else if (maxFee < maxPriorityFee) {
                    context.getString(R.string.tx_exec_max_fee_error_too_small)
                } else {
                    null
                }
            }
        }

        safeLaunch {
            val saveEnabled = nonceError == null && gasLimitError == null && maxPriorityFeeError == null && maxFeeError == null
            updateState {
                TxEditFeeState(
                    saveEnabled = saveEnabled,
                    viewAction = Validate1559FeeData(
                        nonceError = nonceError,
                        gasLimitError = gasLimitError,
                        maxPriorityFeeError = maxPriorityFeeError,
                        maxFeeError = maxFeeError
                    )
                )
            }
        }
    }

    fun validateLegacyInputs(
        context: Context,
        minNonce: BigInteger,
        nonceValue: String?,
        gasLimitValue: String?,
        gasPriceValue: String?
    ) {

        val nonceError = if (nonceValue.isNullOrBlank()) {
            context.getString(R.string.tx_exec_error_value_required)
        } else {
            val nonce = nullOnThrow { BigInteger(nonceValue) }
            if (nonce == null || nonce < BigInteger.ZERO) {
                context.getString(R.string.tx_exec_error_value_nan)
            } else {
                if (nonce < minNonce) {
                    context.getString(R.string.tx_exec_nonce_error_executed)
                } else {
                    null
                }
            }
        }

        val gasLimitError = if (gasLimitValue.isNullOrBlank()) {
            context.getString(R.string.tx_exec_error_value_required)
        } else {
            val gasLimit = nullOnThrow { BigInteger(gasLimitValue) }
            if (gasLimit == null || gasLimit < BigInteger.ZERO) {
                context.getString(R.string.tx_exec_error_value_nan)
            } else {
                null
            }
        }

        val gasPriceError = if (gasPriceValue.isNullOrBlank()) {
            context.getString(R.string.tx_exec_error_value_required)
        } else {
            val gasPrice = nullOnThrow { BigInteger(gasPriceValue) }
            if (gasPrice == null || gasPrice < BigInteger.ZERO) {
                context.getString(R.string.tx_exec_error_value_nan)
            } else {
                null
            }
        }

        safeLaunch {
            val saveEnabled = nonceError == null && gasLimitError == null && gasPriceError == null
            updateState {
                TxEditFeeState(
                    saveEnabled = saveEnabled,
                    viewAction = ValidateLegacyFeeData(
                        nonceError = nonceError,
                        gasLimitError = gasLimitError,
                        gasPriceError = gasPriceError
                    )
                )
            }
        }
    }

    fun totalFee1559(
        context: Context,
        chain: Chain,
        gasLimitValue: String?,
        maxFeeValue: String?
    ): String {
        val gasLimit = nullOnThrow { BigInteger(gasLimitValue) }
        val maxFee = nullOnThrow { BigInteger(maxFeeValue) }
        return if (gasLimit == null || maxFee == null) {
            context.getString(R.string.value_not_available)
        } else {
            val totalFee = gasLimit * maxFee
            "$totalFee ${chain.currency.symbol}"
        }
    }

    fun totalFeeLegacy(
        context: Context,
        chain: Chain,
        gasLimitValue: String?,
        gasPriceValue: String?
    ): String {
        val gasLimit = nullOnThrow { BigInteger(gasLimitValue) }
        val gasPrice = nullOnThrow { BigInteger(gasPriceValue) }
        return if (gasLimit == null || gasPrice == null) {
            context.getString(R.string.value_not_available)
        } else {
            val totalFee = gasLimit * gasPrice
            "$totalFee ${chain.currency.symbol}"
        }
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
