package io.gnosis.safe.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.transaction.LabelType
import io.gnosis.safe.R
import io.gnosis.safe.databinding.*
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_FULL
import io.gnosis.safe.utils.formatBackendDate

enum class TransactionViewType {
    TRANSFER,
    TRANSFER_QUEUED,
    CHANGE_IMPLEMENTATION,
    CHANGE_IMPLEMENTATION_QUEUED,
    SETTINGS_CHANGE,
    SETTINGS_CHANGE_QUEUED,
    CONTRACT_INTERACTION,
    CONTRACT_INTERACTION_QUEUED,
    SECTION_DATE_HEADER,
    SECTION_CONFLICT_HEADER,
    SECTION_LABEL_HEADER,
    CREATION
}

class TransactionViewHolderFactory : BaseFactory<BaseTransactionViewHolder<TransactionView>, TransactionView>() {

    @Suppress("UNCHECKED_CAST")
    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseTransactionViewHolder<TransactionView> =
        when (viewType) {
            //TODO: Remove CHANGE_IMPLEMENTATION and CHANGE_IMPLEMENTATION_QUEUED
            TransactionViewType.CHANGE_IMPLEMENTATION.ordinal -> SettingsChangeViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.CHANGE_IMPLEMENTATION_QUEUED.ordinal -> SettingsChangeQueuedViewHolder(viewBinding as ItemTxQueuedSettingsChangeBinding)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> SettingsChangeViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> SettingsChangeQueuedViewHolder(viewBinding as ItemTxQueuedSettingsChangeBinding)
            TransactionViewType.TRANSFER.ordinal -> TransferViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> TransferQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            TransactionViewType.CONTRACT_INTERACTION.ordinal -> ContractInteractionViewHolder(viewBinding as ItemTxContractInteractionBinding)
            TransactionViewType.CONTRACT_INTERACTION_QUEUED.ordinal -> ContractInteractionQueuedViewHolder(viewBinding as ItemTxQueuedContractInteractionBinding)
            TransactionViewType.SECTION_DATE_HEADER.ordinal -> SectionDateHeaderViewHolder(viewBinding as ItemTxSectionHeaderBinding)
            TransactionViewType.SECTION_CONFLICT_HEADER.ordinal -> SectionConflictHeaderViewHolder(viewBinding as ItemTxSectionHeaderBinding)
            TransactionViewType.SECTION_LABEL_HEADER.ordinal -> SectionLabelHeaderViewHolder(viewBinding as ItemTxSectionHeaderBinding)
            TransactionViewType.CREATION.ordinal -> CreationTransactionViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseTransactionViewHolder<TransactionView>

    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding =
        when (viewType) {
            TransactionViewType.CHANGE_IMPLEMENTATION.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CHANGE_IMPLEMENTATION_QUEUED.ordinal -> ItemTxQueuedSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> ItemTxQueuedSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER.ordinal -> ItemTxTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> ItemTxQueuedTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CONTRACT_INTERACTION.ordinal -> ItemTxContractInteractionBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CONTRACT_INTERACTION_QUEUED.ordinal -> ItemTxQueuedContractInteractionBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_DATE_HEADER.ordinal -> ItemTxSectionHeaderBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_CONFLICT_HEADER.ordinal -> ItemTxSectionHeaderBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_LABEL_HEADER.ordinal -> ItemTxSectionHeaderBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CREATION.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun viewTypeFor(item: TransactionView): Int =
        when (item) {
            is TransactionView.Transfer -> TransactionViewType.TRANSFER
            is TransactionView.TransferQueued -> TransactionViewType.TRANSFER_QUEUED
            is TransactionView.SettingsChange -> TransactionViewType.SETTINGS_CHANGE
            is TransactionView.SettingsChangeQueued -> TransactionViewType.SETTINGS_CHANGE_QUEUED
//            is TransactionView.SettingsChangeVariant -> TransactionViewType.CHANGE_IMPLEMENTATION
//            is TransactionView.SettingsChangeVariantQueued -> TransactionViewType.CHANGE_IMPLEMENTATION_QUEUED
            is TransactionView.SectionDateHeader -> TransactionViewType.SECTION_DATE_HEADER
            is TransactionView.SectionLabelHeader -> TransactionViewType.SECTION_LABEL_HEADER
            is TransactionView.SectionConflictHeader -> TransactionViewType.SECTION_CONFLICT_HEADER
            is TransactionView.CustomTransaction -> TransactionViewType.CONTRACT_INTERACTION
            is TransactionView.CustomTransactionQueued -> TransactionViewType.CONTRACT_INTERACTION_QUEUED
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
            txTypeIcon.setImageResource(viewTransfer.txTypeIcon)
            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            dateTime.text = viewTransfer.dateTimeText
            action.setText(viewTransfer.direction)
            amount.text = viewTransfer.amountText
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            nonce.text = viewTransfer.nonce

            finalStatus.alpha = OPACITY_FULL
            txTypeIcon.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            action.alpha = viewTransfer.alpha
            amount.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha

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
            action.setText(viewTransfer.direction)
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
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
            txTypeIcon.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            settingName.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha

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
            settingName.text = viewTransfer.method

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

class ContractInteractionQueuedViewHolder(private val viewBinding: ItemTxQueuedContractInteractionBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransactionQueued>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.CustomTransactionQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme

        with(viewBinding) {
            txTypeIcon.setImageResource(R.drawable.ic_code_16dp)

            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTimeText

            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)

            action.setText(R.string.tx_list_contract_interaction)
            label.text = viewTransfer.methodName
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.id)
            }
        }
    }
}

class ContractInteractionViewHolder(private val viewBinding: ItemTxContractInteractionBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransaction>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.CustomTransaction, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme

        with(viewBinding) {
            txTypeIcon.setImageResource(R.drawable.ic_code_16dp)

            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            dateTime.text = viewTransfer.dateTimeText
            action.setText(R.string.tx_list_contract_interaction)
            label.text = viewTransfer.methodName
            nonce.text = viewTransfer.nonce

            finalStatus.alpha = OPACITY_FULL
            txTypeIcon.alpha = viewTransfer.alpha
            dateTime.alpha = viewTransfer.alpha
            action.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha

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
            TransactionsFragmentDirections.actionTransactionsFragmentToTransactionCreationDetailsFragment(
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
    Navigation.findNavController(view).navigate(TransactionsFragmentDirections.actionTransactionsFragmentToTransactionDetailsFragment(id))
}

class SectionDateHeaderViewHolder(private val viewBinding: ItemTxSectionHeaderBinding) :
    BaseTransactionViewHolder<TransactionView.SectionDateHeader>(viewBinding) {

    override fun bind(sectionDateHeader: TransactionView.SectionDateHeader, payloads: List<Any>) {
        with(viewBinding) {
            sectionTitle.text = sectionDateHeader.date.formatBackendDate()
        }
    }
}

class SectionConflictHeaderViewHolder(private val viewBinding: ItemTxSectionHeaderBinding) :
    BaseTransactionViewHolder<TransactionView.SectionConflictHeader>(viewBinding) {

    override fun bind(sectionDateHeader: TransactionView.SectionConflictHeader, payloads: List<Any>) {
        with(viewBinding) {
            sectionTitle.text = sectionDateHeader.nonce.toString()
        }
    }
}

class SectionLabelHeaderViewHolder(private val viewBinding: ItemTxSectionHeaderBinding) :
    BaseTransactionViewHolder<TransactionView.SectionLabelHeader>(viewBinding) {

    override fun bind(sectionDateHeader: TransactionView.SectionLabelHeader, payloads: List<Any>) {
        with(viewBinding) {
            sectionTitle.setText(getLabelResourceId(sectionDateHeader.label))
        }
    }

    private fun getLabelResourceId(label: LabelType): Int {
        return when (label) {
            LabelType.Next -> R.string.tx_list_label_type_next
            LabelType.Queued -> R.string.tx_list_label_type_queued
        }
    }
}

