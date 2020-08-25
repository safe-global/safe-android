package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import io.gnosis.data.backend.dto.TransactionDirection
import io.gnosis.data.models.DetailedExecutionInfo
import io.gnosis.data.models.TransactionDetails
import io.gnosis.data.models.TransactionInfo
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsBinding
import io.gnosis.safe.databinding.TxDetailsCustomBinding
import io.gnosis.safe.databinding.TxDetailsSettingsChangeBinding
import io.gnosis.safe.databinding.TxDetailsTransferBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.TxStatusView
import io.gnosis.safe.utils.formatBackendDate
import io.gnosis.safe.utils.formattedAmount
import io.gnosis.safe.utils.getErrorResForException
import io.gnosis.safe.utils.logoUri
import io.gnosis.safe.utils.txActionInfoItems
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.HttpCodes
import retrofit2.HttpException
import java.math.BigInteger
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

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
                    when (viewAction.error) {
                        is Offline -> {
                            snackbar(requireView(), R.string.error_no_internet)
                        }
                        else -> {
                            snackbar(requireView(), viewAction.error.getErrorResForException())

                            // only show empty state if we don't have anything to show
                            if (binding.executed.value.isNullOrBlank() && binding.created.value.isNullOrBlank()) {
                                binding.content.visibility = View.GONE
                                binding.contentNoData.root.visible(true)
                            }
                        }
                    }
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
        binding.content.visible(true)
        binding.contentNoData.root.visible(false)

        when (val txInfo = txDetails?.txInfo) {
            is TransactionInfo.Transfer -> {
                val viewStub = binding.stubTransfer
                if (viewStub.parent != null) {
                    val inflate = viewStub.inflate()
                    contentBinding = TxDetailsTransferBinding.bind(inflate)
                }
                val txDetailsTransferBinding = contentBinding as TxDetailsTransferBinding

                val outgoing = txInfo.direction == TransactionDirection.OUTGOING
                val address = if (outgoing) txInfo.recipient else txInfo.sender
                txDetailsTransferBinding.txAction.setActionInfo(outgoing, txInfo.formattedAmount(), txInfo.logoUri() ?: "", address)

                val txType = if (txInfo.direction == TransactionDirection.INCOMING) {
                    TxStatusView.TxType.TRANSFER_INCOMING
                } else {
                    TxStatusView.TxType.TRANSFER_OUTGOING
                }
                txDetailsTransferBinding.txStatus.setStatus(txType, txDetails.txStatus)
            }
            is TransactionInfo.SettingsChange -> {
                val viewStub = binding.stubSettingsChange
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsSettingsChangeBinding.bind(viewStub.inflate())
                }
                val txDetailsSettingsChangeBinding = contentBinding as TxDetailsSettingsChangeBinding

                txDetailsSettingsChangeBinding.txAction.setActionInfoItems(txInfo.txActionInfoItems())
                txDetailsSettingsChangeBinding.txStatus.setStatus(TxStatusView.TxType.MODIFY_SETTINGS, txDetails.txStatus)
            }
            is TransactionInfo.Custom -> {
                val viewStub = binding.stubCustom
                if (viewStub.parent != null) {
                    contentBinding = TxDetailsCustomBinding.bind(viewStub.inflate())
                }
                val txDetailsCustomBinding = contentBinding as TxDetailsCustomBinding

                txDetailsCustomBinding.txAction.setActionInfo(true, txInfo.formattedAmount(), txInfo.logoUri()!!, txInfo.to)
                txDetailsCustomBinding.txStatus.setStatus(TxStatusView.TxType.CUSTOM, txDetails.txStatus)
                txDetailsCustomBinding.txData.setData(txDetails.txData?.hexData, txInfo.dataSize)
            }
        }
        var nonce: BigInteger? = null
        when (val executionInfo = txDetails?.detailedExecutionInfo) {
            is DetailedExecutionInfo.MultisigExecutionDetails -> {
                binding.txConfirmations.visible(true)
                binding.txConfirmationsDivider.visible(true)
                binding.txConfirmations.setExecutionData(
                    status = txDetails.txStatus,
                    confirmations = executionInfo.confirmations.sortedBy { it.submittedAt }.map { it.signer },
                    threshold = executionInfo.confirmationsRequired,
                    executor = executionInfo.executor
                )
                nonce = executionInfo.nonce

                binding.created.visible(true)
                binding.createdDivider.visible(true)
                binding.created.value = executionInfo.submittedAt.formatBackendDate()
            }
            is DetailedExecutionInfo.ModuleExecutionDetails -> {
                hideCreatedAndConfirmations()
            }
            else -> {
                hideCreatedAndConfirmations()
            }
        }
        if (txDetails?.detailedExecutionInfo != null) {
            binding.advanced.visible(true)
            binding.advanced.setOnClickListener {
                val operation = txDetails.txData?.operation?.name?.toLowerCase(Locale.getDefault()) ?: ""
                findNavController().navigate(
                    TransactionDetailsFragmentDirections.actionTransactionDetailsFragmentToAdvancedTransactionDetailsFragment(
                        nonce = nonce?.toString() ?: "",
                        operation = operation,
                        hash = txDetails.txHash
                    )
                )
            }
        } else {
            binding.advanced.visible(false)
        }
        if (txDetails?.executedAt != null) {
            binding.executed.visible(true)
            binding.executedDivider.visible(true)
            binding.executed.value = txDetails.executedAt!!.formatBackendDate()
        } else {
            binding.executed.visible(false)
            binding.executedDivider.visible(false)
        }

        if (txDetails?.txHash != null) {
            binding.etherscan.visible(true)
            binding.etherscan.setOnClickListener {
                requireContext().openUrl(
                    getString(
                        R.string.etherscan_transaction_url,
                        txDetails.txHash
                    )
                )
            }
        } else {
            binding.etherscan.visible(false)
        }
    }

    private fun hideCreatedAndConfirmations() {
        binding.txConfirmations.visible(false)
        binding.txConfirmationsDivider.visible(false)
        binding.created.visible(false)
        binding.createdDivider.visible(false)
    }
}
