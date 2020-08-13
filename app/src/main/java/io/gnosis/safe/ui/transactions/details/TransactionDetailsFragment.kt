package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import io.gnosis.data.backend.dto.MultisigExecutionDetails
import io.gnosis.data.models.CustomDetails
import io.gnosis.data.models.SettingsChangeDetails
import io.gnosis.data.models.TransactionDetails
import io.gnosis.data.models.TransferDetails
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsBinding
import io.gnosis.safe.databinding.TxDetailsCustomBinding
import io.gnosis.safe.databinding.TxDetailsSettingsChangeBinding
import io.gnosis.safe.databinding.TxDetailsTransferBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.TxStatusView
import io.gnosis.safe.utils.formatBackendDate
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class TransactionDetailsFragment : BaseViewBindingFragment<FragmentTransactionDetailsBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_DETAILS

    private val navArgs by navArgs<TransactionDetailsFragmentArgs>()
    private val txId by lazy { navArgs.txId }

    @Inject
    lateinit var viewModel: TransactionDetailsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsBinding =
        FragmentTransactionDetailsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            backButton.setOnClickListener {
                Navigation.findNavController(root).navigateUp()
            }

            refresh.setOnRefreshListener {
                viewModel.loadDetails(txId)
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val viewAction = state.viewAction) {
                is BaseStateViewModel.ViewAction.Loading -> {
                    updateUi(state.txDetails, viewAction.isLoading)
                }
                is BaseStateViewModel.ViewAction.ShowError -> {
                    binding.refresh.isRefreshing = false
                    //TODO: handle error here
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDetails(txId)
    }

    private lateinit var contentBinding: ViewBinding

    private fun updateUi(txDetails: TransactionDetails?, isLoading: Boolean) {

        binding.refresh.isRefreshing = isLoading

        when (txDetails) {
            is TransferDetails -> {
                val viewStub = binding.stubTransfer
                if (viewStub.parent != null) {
                    val inflate = viewStub.inflate()
                    contentBinding = TxDetailsTransferBinding.bind(inflate)
                }
                val txDetailsTransferBinding = contentBinding as TxDetailsTransferBinding

                txDetailsTransferBinding.txConfirmations.setExecutionData(
                    status = txDetails.txStatus,
                    confirmations = (txDetails.detailedExecutionInfo as? MultisigExecutionDetails)?.confirmations?.map { it.signer } ?: listOf(),
                    threshold = (txDetails.detailedExecutionInfo as? MultisigExecutionDetails)?.confirmationsRequired ?: 0,
                    executor = txDetails.executor
                )
                txDetailsTransferBinding.created.value = txDetails.createdAt?.formatBackendDate() ?: ""

                txDetailsTransferBinding.executed.value = txDetails.executedAt?.formatBackendDate() ?: ""
                txDetailsTransferBinding.etherscan.setOnClickListener {
                    requireContext().openUrl(
                        getString(
                            R.string.etherscan_transaction_url,
                            txDetails.txHash
                        )
                    )
                }
                val txType = if (txDetails.incoming == true) TxStatusView.TxType.TRANSFER_INCOMING else TxStatusView.TxType.TRANSFER_OUTGOING
                txDetailsTransferBinding.txStatus.setStatus(txType, txDetails.txStatus)
            }
            is SettingsChangeDetails -> {
                val viewStub = binding.stubSettingsChange
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsSettingsChangeBinding.bind(viewStub.inflate())
                }
                val txDetailsSettingsChangeBinding = contentBinding as TxDetailsSettingsChangeBinding

                txDetailsSettingsChangeBinding.txConfirmations.setExecutionData(
                    status = txDetails.txStatus,
                    confirmations = (txDetails.detailedExecutionInfo as? MultisigExecutionDetails)?.confirmations?.map { it.signer } ?: listOf(),
                    threshold = (txDetails.detailedExecutionInfo as? MultisigExecutionDetails)?.confirmationsRequired ?: 0,
                    executor = txDetails.executor
                )
                txDetailsSettingsChangeBinding.created.value = txDetails.createdAt?.formatBackendDate() ?: ""
                txDetailsSettingsChangeBinding.executed.value = txDetails.executedAt?.formatBackendDate() ?: ""

                txDetailsSettingsChangeBinding.etherscan.setOnClickListener {
                    requireContext().openUrl(
                        getString(
                            R.string.etherscan_transaction_url,
                            txDetails.txHash
                        )
                    )
                }
                txDetailsSettingsChangeBinding.txStatus.setStatus(TxStatusView.TxType.MODIFY_SETTINGS, txDetails.txStatus)
            }
            is CustomDetails -> {
                val viewStub = binding.stubCustom
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsCustomBinding.bind(viewStub.inflate())
                }
                val txDetailsCustomBinding = contentBinding as TxDetailsCustomBinding

                txDetailsCustomBinding.txConfirmations.setExecutionData(
                    status = txDetails.txStatus,
                    confirmations = (txDetails.detailedExecutionInfo as? MultisigExecutionDetails)?.confirmations?.map { it.signer } ?: listOf(),
                    threshold = (txDetails.detailedExecutionInfo as? MultisigExecutionDetails)?.confirmationsRequired ?: 0,
                    executor = txDetails.executor
                )
                txDetailsCustomBinding.created.value = txDetails.createdAt?.formatBackendDate() ?: ""
                txDetailsCustomBinding.executed.value = txDetails.executedAt?.formatBackendDate() ?: ""
                txDetailsCustomBinding.etherscan.setOnClickListener {
                    requireContext().openUrl(
                        getString(
                            R.string.etherscan_transaction_url,
                            txDetails.txHash
                        )
                    )
                }

                txDetailsCustomBinding.txStatus.setStatus(TxStatusView.TxType.CUSTOM, txDetails.txStatus)

                if (txDetails.txData != null) {
                    txDetailsCustomBinding.txData.visible(true)
                    txDetails.txData?.hexData?.let { txDetailsCustomBinding.txData.setData(it, txDetails.dataSize) }
                } else {
                    txDetailsCustomBinding.txData.visible(false)
                    txDetailsCustomBinding.txDataSeparator.visible(false)
                }
            }
        }
    }
}
