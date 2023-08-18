package io.gnosis.safe.ui.transactions

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.gnosis.data.models.Chain
import io.gnosis.data.models.transaction.ConflictType
import io.gnosis.data.models.transaction.LabelType
import io.gnosis.data.models.transaction.TransactionStatus
import io.gnosis.safe.R
import kotlinx.parcelize.Parcelize
import java.util.Date

sealed class TransactionView(
    open val status: TransactionStatus?,
    open val id: String,
    open val chain: Chain?
) {
    data class Conflict(
        val innerView: TransactionView,
        val conflictType: ConflictType,
        override val id: String = innerView.id,
        override val status: TransactionStatus? = innerView.status
    ) : TransactionView(status, id, innerView.chain)

    data class Transfer(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTimeText: String,
        @DrawableRes val txTypeIcon: Int,
        @StringRes val direction: Int,
        @ColorRes val amountColor: Int,
        val alpha: Float,
        val nonce: String
    ) : TransactionView(status, id, chain)

    data class TransferQueued(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val amountText: String,
        val dateTime: Date,
        @DrawableRes val txTypeIcon: Int,
        @StringRes val direction: Int,
        @ColorRes val amountColor: Int,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status, id, chain)

    data class SettingsChange(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val method: String,
        val alpha: Float,
        val nonce: String
    ) : TransactionView(status, id, chain)

    data class SettingsChangeQueued(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTime: Date,
        val method: String,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status, id, chain)

    data class CustomTransaction(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val alpha: Float,
        val nonce: String,
        val methodName: String?,
        val actionCount: Int?,
        val addressInfo: AddressInfoData
    ) : TransactionView(status, id, chain)

    data class CustomTransactionQueued(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTime: Date,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String,
        val methodName: String?,
        val actionCount: Int?,
        val addressInfo: AddressInfoData
    ) : TransactionView(status, id, chain)

    data class Creation(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        @StringRes val label: Int,
        val creationDetails: CreationDetails? = null
    ) : TransactionView(status, id, chain)

    data class CreationDetails(
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val creator: String,
        val creatorInfo: AddressInfoData,
        val implementation: String?,
        val implementationInfo: AddressInfoData.Remote,
        val factory: String?,
        val factoryInfo: AddressInfoData.Remote,
        val transactionHash: String
    )

    data class RejectionTransaction(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTimeText: String,
        val alpha: Float,
        val nonce: String
    ) : TransactionView(status, id, chain)

    data class RejectionTransactionQueued(
        override val id: String,
        override val status: TransactionStatus,
        override val chain: Chain,
        @StringRes val statusText: Int,
        @ColorRes val statusColorRes: Int,
        val dateTime: Date,
        val confirmations: Int,
        val threshold: Int,
        @ColorRes val confirmationsTextColor: Int,
        @DrawableRes val confirmationsIcon: Int,
        val nonce: String
    ) : TransactionView(status, id, chain)

    data class SectionDateHeader(val date: Date, override val id: String = "<unused>") : TransactionView(null, id, null)
    data class SectionLabelHeader(val label: LabelType, override val id: String = "<unused>") : TransactionView(null, id, null)
    data class SectionConflictHeader(val nonce: Long, override val id: String = "<unused>") : TransactionView(null, id, null)
    object Unknown : TransactionView(null, "<unused>", null)
}

sealed class AddressInfoData: Parcelable {

    @Parcelize
    data class Local(
        val name: String?,
        val address: String
    ) : AddressInfoData()

    @Parcelize
    data class Remote(
        val name: String?,
        val addressLogoUri: String?,
        val address: String?,
        val appInfo: Boolean = false
    ) : AddressInfoData()

    @Parcelize
    object Default : AddressInfoData() {
        @StringRes
        val nameResId: Int = R.string.tx_list_contract_interaction

        @DrawableRes
        val logoResId: Int = R.drawable.ic_code_16dp
    }
}
