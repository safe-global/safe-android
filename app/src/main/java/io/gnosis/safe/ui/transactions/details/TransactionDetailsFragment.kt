package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.TransactionDetails
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
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
                is BaseStateViewModel.ViewAction.Loading -> updateUi(state.txDetails, viewAction.isLoading)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDetails(txId)
    }

    private fun updateUi(txDetails: TransactionDetails?, isLoading: Boolean) {
        binding.refresh.isRefreshing = isLoading
        txDetails?.let {
            binding.content.visible(true)
            binding.txConfirmations.setExecutionData(
                status = txDetails.txStatus,
                confirmations = txDetails.detailedExecutionInfo.confirmations.map { it.signer },
                threshold = txDetails.detailedExecutionInfo.confirmationsRequired,
                executor = txDetails.executor
            )
            binding.created.value = txDetails.createdAt?.formatBackendDate() ?: ""
            binding.executed.value = txDetails.executedAt?.formatBackendDate() ?: ""
            binding.etherscan.setOnClickListener {
                requireContext().openUrl(
                    getString(
                        R.string.etherscan_transaction_url,
                        txDetails.txHash
                    )
                )
            }
        }
    }
}
