package io.gnosis.safe.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.R
import io.gnosis.safe.databinding.*
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_FULL
import io.gnosis.safe.utils.formatForTxList

enum class TransactionViewType {
    TRANSFER,
    TRANSFER_QUEUED,
    CHANGE_MASTERCOPY,
    CHANGE_MASTERCOPY_QUEUED,
    SETTINGS_CHANGE,
    SETTINGS_CHANGE_QUEUED,
    CUSTOM_TRANSACTION,
    CUSTOM_TRANSACTION_QUEUED,
    SECTION_HEADER,
    CREATION
}

class TransactionViewHolderFactory : BaseFactory<BaseTransactionViewHolder<TransactionView>, TransactionView>() {

    @Suppress("UNCHECKED_CAST")
    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseTransactionViewHolder<TransactionView> =
        when (viewType) {
            TransactionViewType.CHANGE_MASTERCOPY.ordinal -> ChangeMastercopyViewHolder(viewBinding as ItemTxChangeMastercopyBinding)
            TransactionViewType.CHANGE_MASTERCOPY_QUEUED.ordinal -> ChangeMastercopyQueuedViewHolder(viewBinding as ItemTxQueuedChangeMastercopyBinding)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> SettingsChangeViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> SettingsChangeQueuedViewHolder(viewBinding as ItemTxQueuedSettingsChangeBinding)
            TransactionViewType.TRANSFER.ordinal -> TransferViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> TransferQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            TransactionViewType.CUSTOM_TRANSACTION.ordinal -> CustomTransactionViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.CUSTOM_TRANSACTION_QUEUED.ordinal -> CustomTransactionQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            TransactionViewType.SECTION_HEADER.ordinal -> SectionHeaderViewHolder(viewBinding as ItemTxSectionHeaderBinding)
            TransactionViewType.CREATION.ordinal -> CreationTransactionViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseTransactionViewHolder<TransactionView>

    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding =
        when (viewType) {
            TransactionViewType.CHANGE_MASTERCOPY.ordinal -> ItemTxChangeMastercopyBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CHANGE_MASTERCOPY_QUEUED.ordinal -> ItemTxQueuedChangeMastercopyBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> ItemTxQueuedSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER.ordinal,
            TransactionViewType.CUSTOM_TRANSACTION.ordinal -> ItemTxTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER_QUEUED.ordinal,
            TransactionViewType.CUSTOM_TRANSACTION_QUEUED.ordinal -> ItemTxQueuedTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_HEADER.ordinal -> ItemTxSectionHeaderBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CREATION.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun viewTypeFor(item: TransactionView): Int =
        when (item) {
            is TransactionView.Transfer -> TransactionViewType.TRANSFER
            is TransactionView.TransferQueued -> TransactionViewType.TRANSFER_QUEUED
            is TransactionView.SettingsChange -> TransactionViewType.SETTINGS_CHANGE
            is TransactionView.SettingsChangeQueued -> TransactionViewType.SETTINGS_CHANGE_QUEUED
            is TransactionView.SettingsChangeVariant -> TransactionViewType.CHANGE_MASTERCOPY
            is TransactionView.SettingsChangeVariantQueued -> TransactionViewType.CHANGE_MASTERCOPY_QUEUED
            is TransactionView.SectionHeader -> TransactionViewType.SECTION_HEADER
            is TransactionView.CustomTransaction -> TransactionViewType.CUSTOM_TRANSACTION
            is TransactionView.CustomTransactionQueued -> TransactionViewType.CUSTOM_TRANSACTION_QUEUED
            is TransactionView.Creation -> TransactionViewType.CREATION
            is TransactionView.Unknown -> throw UnsupportedViewType(javaClass.name)
        }.ordinal
}

abstract class BaseTransactionViewHolder<T : TransactionView>(viewBinding: ViewBinding) : Adapter.ViewHolder<T>(viewBinding.root)

class TransferViewHolder(private val viewBinding: ItemTxTransferBinding) :
    BaseTransactionViewHolder<TransactionView.Transfer>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.Transfer, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            amount.text = viewTransfer.amountText
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            dateTime.text = viewTransfer.dateTimeText
            txTypeIcon.setImageResource(viewTransfer.txTypeIcon)
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address.formatForTxList()
            nonce.text = viewTransfer.nonce

            finalStatus.alpha = OPACITY_FULL
            amount.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            txTypeIcon.alpha = viewTransfer.alpha
            blockies.alpha = viewTransfer.alpha
            ellipsizedAddress.alpha = viewTransfer.alpha

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class TransferQueuedViewHolder(private val viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<TransactionView.TransferQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.TransferQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            amount.text = viewTransfer.amountText
            dateTime.text = viewTransfer.dateTimeText
            txTypeIcon.setImageResource(viewTransfer.txTypeIcon)
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address.formatForTxList()
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class SettingsChangeViewHolder(private val viewBinding: ItemTxSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChange>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.SettingsChange, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme

        with(viewBinding) {
            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText
            settingName.text = viewTransfer.method
            nonce.text = viewTransfer.nonce

            finalStatus.alpha = OPACITY_FULL
            dateTime.alpha = viewTransfer.alpha
            settingName.alpha = viewTransfer.alpha

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class SettingsChangeQueuedViewHolder(private val viewBinding: ItemTxQueuedSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.SettingsChangeQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText
            settingName.text = viewTransfer.settingNameText

            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class ChangeMastercopyViewHolder(private val viewBinding: ItemTxChangeMastercopyBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeVariant>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.SettingsChangeVariant, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText

            version.text = viewTransfer.version
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address?.formatForTxList() ?: ""
            label.setText(viewTransfer.label)

            finalStatus.alpha = OPACITY_FULL
            dateTime.alpha = viewTransfer.alpha
            version.alpha = viewTransfer.alpha
            blockies.alpha = viewTransfer.alpha
            ellipsizedAddress.alpha = viewTransfer.alpha
            nonce.text = viewTransfer.nonce

            moduleAddress.text = viewTransfer.address?.formatForTxList() ?: ""
            version.visibility = viewTransfer.visibilityVersion
            ellipsizedAddress.visibility = viewTransfer.visibilityEllipsizedAddress
            moduleAddress.visibility = viewTransfer.visibilityModuleAddress

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class ChangeMastercopyQueuedViewHolder(private val viewBinding: ItemTxQueuedChangeMastercopyBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeVariantQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.SettingsChangeVariantQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText

            version.text = viewTransfer.version
            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address?.formatForTxList() ?: ""
            label.setText(viewTransfer.label)
            nonce.text = viewTransfer.nonce

            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)
            confirmationsIcon.visibility = View.VISIBLE

            moduleAddress.text = viewTransfer.address?.formatForTxList() ?: ""
            version.visibility = viewTransfer.visibilityVersion
            ellipsizedAddress.visibility = viewTransfer.visibilityEllipsizedAddress
            moduleAddress.visibility = viewTransfer.visibilityModuleAddress

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class CustomTransactionQueuedViewHolder(private val viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransactionQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.CustomTransactionQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            txTypeIcon.setImageResource(R.drawable.ic_code_16dp)

            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText

            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)
            confirmationsIcon.visibility = View.VISIBLE

            dataSize.text = viewTransfer.dataSizeText
            amount.text = viewTransfer.amountText
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))

            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address.formatForTxList()
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class CustomTransactionViewHolder(private val viewBinding: ItemTxTransferBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransaction>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.CustomTransaction, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            txTypeIcon.setImageResource(R.drawable.ic_code_16dp)

            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText

            blockies.setAddress(viewTransfer.address)
            ellipsizedAddress.text = viewTransfer.address.formatForTxList()

            dataSize.text = viewTransfer.dataSizeText
            amount.text = viewTransfer.amountText
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            nonce.text = viewTransfer.nonce

            finalStatus.alpha = OPACITY_FULL
            txTypeIcon.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            blockies.alpha = viewTransfer.alpha
            ellipsizedAddress.alpha = viewTransfer.alpha
            dataSize.alpha = viewTransfer.alpha
            amount.alpha = viewTransfer.alpha

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class CreationTransactionViewHolder(private val viewBinding: ItemTxSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.Creation>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.Creation, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText
            settingName.setText(viewTransfer.label)

            if (viewTransfer.creationDetails != null) {
                root.setOnClickListener {
                    navigateToCreationDetails(it, viewTransfer.creationDetails)
                }
            }
        }
    }
}

private fun navigateToCreationDetails(view: View, details: TransactionView.CreationDetails) {
    Navigation.findNavController(view)
        .navigate(
            TransactionListFragmentDirections.actionTransactionListFragmentToTransactionCreationDetailsFragment(
                statusColorRes = details.statusColorRes,
                statusTextRes = details.statusText,
                dateTimeText = details.dateTimeText,
                implementation = details.implementation,
                factory = details.factory,
                creator = details.creator,
                transActionHash = details.transactionHash
            )
        )
}

private fun navigateToTxDetails(view: View, id: String) {
    Navigation.findNavController(view).navigate(TransactionListFragmentDirections.actionTransactionListFragmentToTransactionDetailsFragment(id))
}

class SectionHeaderViewHolder(private val viewBinding: ItemTxSectionHeaderBinding) :
    BaseTransactionViewHolder<TransactionView.SectionHeader>(viewBinding) {

    override fun bind(sectionHeader: TransactionView.SectionHeader, payloads: List<Any>) {
        with(viewBinding) {
            sectionTitle.setText(sectionHeader.title)
        }
    }
}
