package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentConfirmRejectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment.Companion.REJECTION_CONFIRMATION_RESULT
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.transactions.TxPagerAdapter
import io.gnosis.safe.utils.appendLink
import io.gnosis.safe.utils.replaceDoubleNewlineWithParagraphLineSpacing
import javax.inject.Inject

class ConfirmRejectionFragment : BaseViewBindingFragment<FragmentConfirmRejectionBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_REJECTION_CONFIRMATION

    private val navArgs by navArgs<ConfirmRejectionFragmentArgs>()
    private val txId by lazy { navArgs.txId }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    @Inject
    lateinit var viewModel: ConfirmRejectionViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentConfirmRejectionBinding =
        FragmentConfirmRejectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadDetails(txId)

        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            info.text = resources.replaceDoubleNewlineWithParagraphLineSpacing(R.string.tx_details_rejection_confirmation_info)
            learnMore.appendLink(
                url = getString(R.string.tx_details_rejection_payment_reason_link),
                urlText = getString(R.string.tx_details_rejection_payment_reason),
                linkIcon = R.drawable.ic_external_link_green_16dp,
                prefix = " ",
                underline = true
            )
            confirmRejection.setOnClickListener {
                confirmRejection.isEnabled = false
                viewModel.submitRejection()
            }
        }
        binding.confirmRejection.isEnabled = false
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val viewAction = state.viewAction) {
                is RejectionSubmitted -> {
                    Navigation.findNavController(view).navigate(
                        ConfirmRejectionFragmentDirections.actionConfirmRejectionFragmentToTransactionsFragment(
                            TxPagerAdapter.Tabs.QUEUE.ordinal
                        )
                    )
                    findNavController().currentBackStackEntry?.savedStateHandle?.set(REJECTION_CONFIRMATION_RESULT, true)
                }
                is BaseStateViewModel.ViewAction.Loading -> {
                    if (!viewAction.isLoading) {
                        binding.confirmRejection.isEnabled = true
                    }
                }
                is BaseStateViewModel.ViewAction.ShowError -> {
                    binding.confirmRejection.isEnabled = true

                    viewAction.error.let {
                        when (it) {
                            is TxRejectionFailed -> {
                                val error = it.cause.toError()
                                tracker.logException(it.cause, error.trackingRequired)
                                errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_tx_rejection))
                            }
                            else -> {
                                val error = it.toError()
                                tracker.logException(it, error.trackingRequired)
                                errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_tx_details))
                            }
                        }
                    }
                }

            }
        })
    }
}
