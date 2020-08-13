package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.models.DomainDetailedExecutionInfo
import io.gnosis.data.models.DomainTransactionDetails
import io.gnosis.data.models.DomainTransactionInfo
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

    private fun updateUi(txDetails: DomainTransactionDetails?, isLoading: Boolean) {

        binding.refresh.isRefreshing = isLoading

        when (val txInfo = txDetails?.txInfo) {
            is DomainTransactionInfo.Transfer -> {
                val viewStub = binding.stubTransfer
                if (viewStub.parent != null) {
                    val inflate = viewStub.inflate()
                    contentBinding = TxDetailsTransferBinding.bind(inflate)
                }
                val txDetailsTransferBinding = contentBinding as TxDetailsTransferBinding

                val txType = if (txInfo.direction == TransactionDirection.INCOMING) TxStatusView.TxType.TRANSFER_INCOMING else TxStatusView.TxType.TRANSFER_OUTGOING
                txDetailsTransferBinding.txStatus.setStatus(txType, txDetails.txStatus)

            }
            is DomainTransactionInfo.SettingsChange -> {
                val viewStub = binding.stubSettingsChange
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsSettingsChangeBinding.bind(viewStub.inflate())
                }
                val txDetailsSettingsChangeBinding = contentBinding as TxDetailsSettingsChangeBinding

                txDetailsSettingsChangeBinding.txStatus.setStatus(TxStatusView.TxType.MODIFY_SETTINGS, txDetails.txStatus)
            }
            is DomainTransactionInfo.Custom -> {
                val viewStub = binding.stubCustom
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsCustomBinding.bind(viewStub.inflate())
                }
                val txDetailsCustomBinding = contentBinding as TxDetailsCustomBinding

                txDetailsCustomBinding.txStatus.setStatus(TxStatusView.TxType.CUSTOM, txDetails.txStatus)

                if (txDetails.txData != null) {
                    txDetailsCustomBinding.txData.visible(true)
                    txDetails.txData?.hexData?.let { txDetailsCustomBinding.txData.setData(it, txInfo.dataSize) }
                } else {
                    txDetailsCustomBinding.txData.visible(false)
                    txDetailsCustomBinding.txDataSeparator.visible(false)
                }
            }

        }

        when (val executionInfo = txDetails?.detailedExecutionInfo) {
            is DomainDetailedExecutionInfo.DomainMultisigExecutionDetails -> {

                binding.txConfirmations.setExecutionData(
                    status = txDetails.txStatus,
                    confirmations = executionInfo.confirmations.map { it.signer },
                    threshold = executionInfo.confirmationsRequired,
                    executor = executionInfo.signers.last()
                )

                binding.executed.value = executionInfo.submittedAt.formatBackendDate()

            }
            is DomainDetailedExecutionInfo.DomainModuleExecutionDetails -> { // do nothing}
            }
        }

        binding.created.value = txDetails?.executedAt?.formatBackendDate() ?: ""

        binding.etherscan.setOnClickListener {
            requireContext().openUrl(
                getString(
                    R.string.etherscan_transaction_url,
                    txDetails?.txHash
                )
            )
        }
    }
}
