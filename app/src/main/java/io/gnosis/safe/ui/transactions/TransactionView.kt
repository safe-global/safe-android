package io.gnosis.safe.ui.transactions

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.gnosis.data.models.transaction.LabelType
import io.gnosis.data.models.transaction.TransactionStatus
import java.util.*

sealed class TransactionView(
    open val status: TransactionStatus?,
    open val id: String
) {
    data class Transfer(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        @StringRes val direction: Int,
        @ColorRes val amountColor: Int,
        val alpha: Float,
        val nonce: String
    ) : TransactionView(status, id)

    data class TransferQueued(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        @StringRes val direction: Int,
        @ColorRes val amountColor: Int,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status, id)

    data class SettingsChange(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val method: String,
        val alpha: Float,
        val nonce: String
    ) : TransactionView(status, id)

    data class SettingsChangeQueued(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val method: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status, id)

    data class CustomTransaction(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val alpha: Float,
        val nonce: String,
        val methodName: String?
    ) : TransactionView(status, id)

    data class CustomTransactionQueued(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String,
        val methodName: String?
    ) : TransactionView(status, id)

    data class Creation(
        override val id: String,
        override val status: TransactionStatus,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        @StringRes val label: Int,
        val creationDetails: CreationDetails? = null
    ) : TransactionView(status, id)

    data class CreationDetails(
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val creator: String,
        val implementation: String?,
        val factory: String?,
        val transactionHash: String
    )

    data class SectionDateHeader(val date: Date, override val id: String = "<unused>") : TransactionView(null, id)
    data class SectionLabelHeader(val label: LabelType, override val id: String = "<unused>") : TransactionView(null, id)
    data class SectionConflictHeader(val nonce: Long, override val id: String = "<unused>") : TransactionView(null, id)
    object Unknown : TransactionView(null, "<unused>")
}
