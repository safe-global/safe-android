package io.gnosis.safe.ui.transactions

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Chain
import io.gnosis.data.models.transaction.ConflictType
import io.gnosis.data.models.transaction.LabelType
import io.gnosis.safe.R
import io.gnosis.safe.databinding.*
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.BaseFactory
import io.gnosis.safe.ui.base.adapter.UnsupportedViewType
import io.gnosis.safe.ui.transactions.TransactionListViewModel.Companion.OPACITY_FULL
import io.gnosis.safe.utils.ElapsedInterval
import io.gnosis.safe.utils.appendLink
import io.gnosis.safe.utils.elapsedIntervalTo
import io.gnosis.safe.utils.formatBackendDate
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.ExperimentalTime

enum class TransactionViewType {
    TRANSFER,
    TRANSFER_QUEUED,
    SETTINGS_CHANGE,
    SETTINGS_CHANGE_QUEUED,
    CONTRACT_INTERACTION,
    CONTRACT_INTERACTION_QUEUED,
    SECTION_DATE_HEADER,
    SECTION_CONFLICT_HEADER,
    SECTION_LABEL_HEADER,
    CREATION,
    REJECTION,
    REJECTION_QUEUED,
    CONFLICT
}

class TransactionViewHolderFactory : BaseFactory<BaseTransactionViewHolder<TransactionView>, TransactionView>() {

    @Suppress("UNCHECKED_CAST")
    override fun newViewHolder(viewBinding: ViewBinding, viewType: Int): BaseTransactionViewHolder<TransactionView> =
        when (viewType) {
            TransactionViewType.SETTINGS_CHANGE.ordinal -> SettingsChangeViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> SettingsChangeQueuedViewHolder(viewBinding as ItemTxQueuedSettingsChangeBinding)
            TransactionViewType.TRANSFER.ordinal -> TransferViewHolder(viewBinding as ItemTxTransferBinding)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> TransferQueuedViewHolder(viewBinding as ItemTxQueuedTransferBinding)
            TransactionViewType.CONTRACT_INTERACTION.ordinal -> ContractInteractionViewHolder(viewBinding as ItemTxContractInteractionBinding)
            TransactionViewType.CONTRACT_INTERACTION_QUEUED.ordinal -> ContractInteractionQueuedViewHolder(viewBinding as ItemTxQueuedContractInteractionBinding)
            TransactionViewType.SECTION_DATE_HEADER.ordinal -> SectionDateHeaderViewHolder(viewBinding as ItemTxSectionHeaderBinding)
            TransactionViewType.SECTION_CONFLICT_HEADER.ordinal -> SectionConflictHeaderViewHolder(viewBinding as ItemTxConflictSectionHeaderBinding)
            TransactionViewType.SECTION_LABEL_HEADER.ordinal -> SectionLabelHeaderViewHolder(viewBinding as ItemTxSectionHeaderBinding)
            TransactionViewType.CREATION.ordinal -> CreationTransactionViewHolder(viewBinding as ItemTxSettingsChangeBinding)
            TransactionViewType.CONFLICT.ordinal -> ConflictViewHolder(viewBinding as ItemTxConflictTxBinding, this)
            TransactionViewType.REJECTION.ordinal -> RejectionViewHolder(viewBinding as ItemTxRejectionBinding)
            TransactionViewType.REJECTION_QUEUED.ordinal -> RejectionQueuedViewHolder(viewBinding as ItemTxQueuedRejectionBinding)
            else -> throw UnsupportedViewType(javaClass.name)
        } as BaseTransactionViewHolder<TransactionView>

    override fun layout(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding =
        when (viewType) {
            TransactionViewType.SETTINGS_CHANGE.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SETTINGS_CHANGE_QUEUED.ordinal -> ItemTxQueuedSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER.ordinal -> ItemTxTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.TRANSFER_QUEUED.ordinal -> ItemTxQueuedTransferBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CONTRACT_INTERACTION.ordinal -> ItemTxContractInteractionBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CONTRACT_INTERACTION_QUEUED.ordinal -> ItemTxQueuedContractInteractionBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_DATE_HEADER.ordinal -> ItemTxSectionHeaderBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_CONFLICT_HEADER.ordinal -> ItemTxConflictSectionHeaderBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.SECTION_LABEL_HEADER.ordinal -> ItemTxSectionHeaderBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CREATION.ordinal -> ItemTxSettingsChangeBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.CONFLICT.ordinal -> ItemTxConflictTxBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.REJECTION.ordinal -> ItemTxRejectionBinding.inflate(layoutInflater, parent, false)
            TransactionViewType.REJECTION_QUEUED.ordinal -> ItemTxQueuedRejectionBinding.inflate(layoutInflater, parent, false)
            else -> throw UnsupportedViewType(javaClass.name)
        }

    override fun viewTypeFor(item: TransactionView): Int =
        when (item) {
            is TransactionView.Transfer -> TransactionViewType.TRANSFER
            is TransactionView.TransferQueued -> TransactionViewType.TRANSFER_QUEUED
            is TransactionView.SettingsChange -> TransactionViewType.SETTINGS_CHANGE
            is TransactionView.SettingsChangeQueued -> TransactionViewType.SETTINGS_CHANGE_QUEUED
            is TransactionView.SectionDateHeader -> TransactionViewType.SECTION_DATE_HEADER
            is TransactionView.SectionLabelHeader -> TransactionViewType.SECTION_LABEL_HEADER
            is TransactionView.SectionConflictHeader -> TransactionViewType.SECTION_CONFLICT_HEADER
            is TransactionView.CustomTransaction -> TransactionViewType.CONTRACT_INTERACTION
            is TransactionView.CustomTransactionQueued -> TransactionViewType.CONTRACT_INTERACTION_QUEUED
            is TransactionView.Creation -> TransactionViewType.CREATION
            is TransactionView.Unknown -> throw UnsupportedViewType(javaClass.name)
            is TransactionView.Conflict -> TransactionViewType.CONFLICT
            is TransactionView.RejectionTransaction -> TransactionViewType.REJECTION
            is TransactionView.RejectionTransactionQueued -> TransactionViewType.REJECTION_QUEUED
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
                navigateToTxDetails(it, viewTransfer.chain, viewTransfer.id)
            }
        }
    }
}

class TransferQueuedViewHolder(private val viewBinding: ItemTxQueuedTransferBinding) :
    BaseTransactionViewHolder<TransactionView.TransferQueued>(viewBinding) {

    @ExperimentalTime
    override fun bind(viewTransfer: TransactionView.TransferQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            amount.text = viewTransfer.amountText
            dateTime.text = viewTransfer.dateTime.elapsedIntervalTo(Date.from(Instant.now())).format(resources)
            txTypeIcon.setImageResource(viewTransfer.txTypeIcon)
            action.setText(viewTransfer.direction)
            amount.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.amountColor, theme))
            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.chain, viewTransfer.id)
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
                navigateToTxDetails(it, viewTransfer.chain, viewTransfer.id)
            }
        }
    }
}

class SettingsChangeQueuedViewHolder(private val viewBinding: ItemTxQueuedSettingsChangeBinding) :
    BaseTransactionViewHolder<TransactionView.SettingsChangeQueued>(viewBinding) {

    @ExperimentalTime
    override fun bind(viewTransfer: TransactionView.SettingsChangeQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme
        with(viewBinding) {
            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTime.elapsedIntervalTo(Date.from(Instant.now())).format(resources)
            settingName.text = viewTransfer.method

            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.chain, viewTransfer.id)
            }
        }
    }
}

class ContractInteractionQueuedViewHolder(private val viewBinding: ItemTxQueuedContractInteractionBinding) :
    BaseTransactionViewHolder<TransactionView.CustomTransactionQueued>(viewBinding) {

    @ExperimentalTime
    override fun bind(viewTransfer: TransactionView.CustomTransactionQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme

        with(viewBinding) {

            when (val addressInfo = viewTransfer.addressInfo) {
                is AddressInfoData.Local -> {
                    addressName.text = addressInfo.name
                    addressLogo.setAddress(addressInfo.address.asEthereumAddress())
                    appLabel.visible(addressInfo.appInfo)
                }
                is AddressInfoData.Remote -> {
                    addressName.text = addressInfo.name
                    addressLogo.loadKnownSafeAppLogo(
                        addressInfo.addressLogoUri,
                        R.drawable.ic_code_16dp
                    )
                    appLabel.visible(addressInfo.appInfo)
                }
                is AddressInfoData.Default -> {
                    addressName.setText(addressInfo.nameResId)
                    addressLogo.setAddress(null)
                    addressLogo.setImageResource(addressInfo.logoResId)
                    appLabel.visible(false)
                }
            }

            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTime.elapsedIntervalTo(Date.from(Instant.now())).format(resources)

            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)

            action.text = resources.getAction(viewTransfer.methodName, viewTransfer.actionCount)
            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.chain, viewTransfer.id)
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

            when (val addressInfo = viewTransfer.addressInfo) {
                is AddressInfoData.Local -> {
                    addressName.text = addressInfo.name
                    addressLogo.setAddress(addressInfo.address.asEthereumAddress())
                    appLabel.visible(addressInfo.appInfo)
                }
                is AddressInfoData.Remote -> {
                    addressName.text = addressInfo.name
                    addressLogo.loadKnownSafeAppLogo(
                        addressInfo.addressLogoUri,
                        R.drawable.ic_code_16dp
                    )
                    appLabel.visible(addressInfo.appInfo)
                }
                is AddressInfoData.Default -> {
                    addressName.setText(addressInfo.nameResId)
                    addressLogo.setAddress(null)
                    addressLogo.setImageResource(addressInfo.logoResId)
                    appLabel.visible(false)
                }
            }

            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            dateTime.text = viewTransfer.dateTimeText

            action.text = resources.getAction(viewTransfer.methodName, viewTransfer.actionCount)
            nonce.text = viewTransfer.nonce

            addressLogo.alpha = viewTransfer.alpha
            addressName.alpha = viewTransfer.alpha
            finalStatus.alpha = OPACITY_FULL
            dateTime.alpha = viewTransfer.alpha
            action.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.chain, viewTransfer.id)
            }
        }
    }
}

private fun Resources.getAction(methodName: String?, actionCount: Int?): String? =
    if (actionCount != null) {
        if (actionCount > 0) this.getQuantityString(R.plurals.tx_list_actions, actionCount, actionCount) else methodName
    } else {
        methodName
    }

class RejectionQueuedViewHolder(private val viewBinding: ItemTxQueuedRejectionBinding) :
    BaseTransactionViewHolder<TransactionView.RejectionTransactionQueued>(viewBinding) {

    @ExperimentalTime
    override fun bind(viewTransfer: TransactionView.RejectionTransactionQueued, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme

        with(viewBinding) {

            txType.text = resources.getString(R.string.tx_list_rejection)

            status.setText(viewTransfer.statusText)
            status.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))

            dateTime.text = viewTransfer.dateTime.elapsedIntervalTo(Date.from(Instant.now())).format(resources)

            confirmationsIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, viewTransfer.confirmationsIcon, theme))
            confirmations.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.confirmationsTextColor, theme))
            confirmations.text = resources.getString(R.string.tx_list_confirmations, viewTransfer.confirmations, viewTransfer.threshold)

            nonce.text = viewTransfer.nonce

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.chain, viewTransfer.id)
            }
        }
    }
}

class RejectionViewHolder(private val viewBinding: ItemTxRejectionBinding) :
    BaseTransactionViewHolder<TransactionView.RejectionTransaction>(viewBinding) {

    override fun bind(viewTransfer: TransactionView.RejectionTransaction, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources
        val theme = viewBinding.root.context.theme

        with(viewBinding) {

            txType.text = resources.getString(R.string.tx_list_rejection)

            finalStatus.setText(viewTransfer.statusText)
            finalStatus.setTextColor(ResourcesCompat.getColor(resources, viewTransfer.statusColorRes, theme))
            dateTime.text = viewTransfer.dateTimeText

            nonce.text = viewTransfer.nonce

            addressLogo.alpha = viewTransfer.alpha
            txType.alpha = viewTransfer.alpha
            finalStatus.alpha = OPACITY_FULL
            dateTime.alpha = viewTransfer.alpha
            nonce.alpha = viewTransfer.alpha

            root.setOnClickListener {
                navigateToTxDetails(it, viewTransfer.chain, viewTransfer.id)
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
                    navigateToCreationDetails(it, viewTransfer.chain, viewTransfer.creationDetails)
                }
            }
        }
    }
}

private fun navigateToCreationDetails(view: View, chain: Chain, details: TransactionView.CreationDetails) {

    var creatorName: String? = null
    var creatorLogoUri: String? = null
    var creatorLocal = false
    when (details.creatorInfo) {
        is AddressInfoData.Local -> {
            creatorName = details.creatorInfo.name
            creatorLocal = true
        }
        is AddressInfoData.Remote -> {
            creatorName = details.creatorInfo.name
            creatorLogoUri = details.creatorInfo.addressLogoUri
            creatorLocal = false
        }
    }
    Navigation.findNavController(view)
        .navigate(
            TransactionsFragmentDirections.actionTransactionsFragmentToTransactionCreationDetailsFragment(
                chain = chain,
                statusColorRes = details.statusColorRes,
                statusTextRes = details.statusText,
                dateTimeText = details.dateTimeText,
                implementation = details.implementation,
                implementationName = details.implementationInfo.name,
                implementationLogoUri = details.implementationInfo.addressLogoUri,
                factory = details.factory,
                factoryName = details.factoryInfo.name,
                factoryLogoUri = details.factoryInfo.addressLogoUri,
                creator = details.creator,
                creatorName =  creatorName,
                creatorLogoUri = creatorLogoUri,
                creatorLocal = creatorLocal,
                transActionHash = details.transactionHash
            )
        )
}

private fun navigateToTxDetails(view: View, chain: Chain, id: String) {
    Navigation.findNavController(view).navigate(TransactionsFragmentDirections.actionTransactionsFragmentToTransactionDetailsFragment(chain, id))
}

class SectionDateHeaderViewHolder(private val viewBinding: ItemTxSectionHeaderBinding) :
    BaseTransactionViewHolder<TransactionView.SectionDateHeader>(viewBinding) {

    override fun bind(sectionDateHeader: TransactionView.SectionDateHeader, payloads: List<Any>) {
        with(viewBinding) {
            sectionTitle.text = sectionDateHeader.date.formatBackendDate()
        }
    }
}

class SectionConflictHeaderViewHolder(private val viewBinding: ItemTxConflictSectionHeaderBinding) :
    BaseTransactionViewHolder<TransactionView.SectionConflictHeader>(viewBinding) {

    override fun bind(sectionDateHeader: TransactionView.SectionConflictHeader, payloads: List<Any>) {
        val resources = viewBinding.root.context.resources

        with(viewBinding) {
            nonce.text = sectionDateHeader.nonce.toString()
            sectionTitle.text = resources.getString(R.string.tx_list_conflict_header_explainer)
            sectionTitle.appendLink(
                url = resources.getString(R.string.tx_list_conflict_header_link),
                urlText = resources.getString(R.string.tx_list_conflict_header_learn_more),
                linkIcon = R.drawable.ic_external_link_green_16dp,
                prefix = " "
            )
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

class ConflictViewHolder(private val viewBinding: ItemTxConflictTxBinding, private val factory: TransactionViewHolderFactory) :
    BaseTransactionViewHolder<TransactionView.Conflict>(viewBinding) {

    private lateinit var conflictView: TransactionView.Conflict

    override fun bind(data: TransactionView.Conflict, payloads: List<Any>) {
        conflictView = data
        viewBinding.txContainer.removeAllViews()
        val viewType = factory.viewTypeFor(data.innerView)
        val innerBinding = factory.layout(LayoutInflater.from(viewBinding.txContainer.context), viewBinding.txContainer, viewType)
        val innerViewHolder = factory.newViewHolder(innerBinding, viewType)
        innerViewHolder.bind(data.innerView, payloads)

        viewBinding.txContainer.addView(innerBinding.root)

        viewBinding.lineBottom.isVisible = data.conflictType != ConflictType.End
    }

    fun hasNext() = conflictView.conflictType != ConflictType.End
}

fun ElapsedInterval.format(resources: Resources) = when (unit) {
    ChronoUnit.SECONDS -> resources.getString(R.string.tx_list_ago_now)
    ChronoUnit.DAYS -> resources.getQuantityString(R.plurals.tx_list_ago_day, value, value)
    ChronoUnit.HOURS -> resources.getQuantityString(R.plurals.tx_list_ago_hour, value, value)
    else -> resources.getString(R.string.tx_list_ago_min, value)
}

