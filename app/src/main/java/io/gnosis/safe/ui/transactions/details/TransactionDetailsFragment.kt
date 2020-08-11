package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.data.backend.dto.MultisigExecutionDetails
import io.gnosis.data.models.CustomDetails
import io.gnosis.data.models.SettingsChangeDetails
import io.gnosis.data.models.TransactionDetails
import io.gnosis.data.models.TransferDetails
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.details.view.TimeInfoItem
import io.gnosis.safe.ui.transactions.details.view.TxConfirmationsView
import io.gnosis.safe.utils.formatBackendDate
import kotlinx.android.synthetic.main.fragment_transaction_details.*
import kotlinx.android.synthetic.main.tx_details_transfer.*
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
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
                is BaseStateViewModel.ViewAction.Loading -> updateUi(state.txDetails, viewAction.isLoading)
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

    private fun updateUi(txDetails: TransactionDetails?, isLoading: Boolean) {

        binding.refresh.isRefreshing = isLoading
        binding.content.visible(true)

        when (txDetails) {
            is TransferDetails -> {
                val view =  stub_transfer.inflate()
                view.visibility = View.VISIBLE

                Timber.i("---> updateUi(): inflated view: $view")

                view.findViewById<TxConfirmationsView>(R.id.tx_confirmations).setExecutionData(
                    status = txDetails.txStatus,
                    confirmations = (txDetails.detailedExecutionInfo as? MultisigExecutionDetails)?.confirmations?.map { it.signer } ?: listOf(),
                    threshold = (txDetails.detailedExecutionInfo as? MultisigExecutionDetails)?.confirmationsRequired ?: 0,
                    executor = txDetails.executor
                )
                view.findViewById<TimeInfoItem>(R.id.created).value = txDetails.createdAt?.formatBackendDate() ?: ""

                executed.value = txDetails.executedAt?.formatBackendDate() ?: ""
                etherscan.setOnClickListener {
                    requireContext().openUrl(
                        getString(
                            R.string.etherscan_transaction_url,
                            txDetails.txHash
                        )
                    )
                }
            }
            is CustomDetails -> {
                val view =  stub_custom.inflate()
                view.visibility = View.VISIBLE
            }
            is SettingsChangeDetails -> {
                val view =  stub_setttings_change.inflate()
                view.visibility = View.VISIBLE
            }

        }
    }
}
