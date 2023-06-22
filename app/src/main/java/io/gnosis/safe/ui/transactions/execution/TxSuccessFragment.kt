package io.gnosis.safe.ui.transactions.execution

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTxSuccessBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class TxSuccessFragment: BaseViewBindingFragment<FragmentTxSuccessBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_SUCCESS_SIGNER

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTxSuccessBinding =
        FragmentTxSuccessBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // disable default back navigation
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            })
        with(binding) {
            doneButton.setOnClickListener {
                findNavController().popBackStack(R.id.transactionDetailsFragment, false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.lottieSuccess.playAnimation()
    }
}
