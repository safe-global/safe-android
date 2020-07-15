package io.gnosis.safe.ui.safe.transactions

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.gnosis.data.models.TransactionStatus
import pm.gnosis.model.Solidity

sealed class TransactionView(open val status: TransactionStatus?) {
    data class Transfer(
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        val address: Solidity.Address,
        @ColorRes val amountColor: Int,
        val alpha: Float
    ) : TransactionView(status)

    data class TransferQueued(
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        val address: Solidity.Address,
        @ColorRes val amountColor: Int,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status)

    data class SettingsChange(
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val method: String,
        val alpha: Float
    ) : TransactionView(status)

    data class SettingsChangeQueued(
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val settingNameText: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status)

    data class SettingsChangeVariant(
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address?,
        val version: String,
        val alpha: Float,
        @StringRes val label: Int,
        val visibilityVersion: Int = View.VISIBLE,
        val visibilityEllipsizedAddress: Int = View.VISIBLE,
        val visibilityModuleAddress: Int = View.GONE

    ) : TransactionView(status)

    data class SettingsChangeVariantQueued(
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String,
        val address: Solidity.Address?,
        val version: String,
        @StringRes val label: Int,
        val visibilityVersion: Int = View.VISIBLE,
        val visibilityEllipsizedAddress: Int = View.VISIBLE,
        val visibilityModuleAddress: Int = View.GONE
    ) : TransactionView(status)

    data class CustomTransaction(
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address,
        val dataSizeText: String,
        val amountText: String,
        @ColorRes val amountColor: Int,
        val alpha: Float
    ) : TransactionView(status)

    data class CustomTransactionQueued(
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val address: Solidity.Address,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val dataSizeText: String,
        val amountText: String,
        @ColorRes val amountColor: Int,
        val nonce: String
    ) : TransactionView(status)

    data class SectionHeader(@StringRes val title: Int) : TransactionView(TransactionStatus.Pending)

    object Unknown : TransactionView(null)
}
