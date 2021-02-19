package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentConfirmRejectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.appendLink
import io.gnosis.safe.utils.replaceDoubleNewlineWithParagraphLineSpacing

class ConfirmRejectionFragment : BaseViewBindingFragment<FragmentConfirmRejectionBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_REJECTION_CONFIRMATION

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentConfirmRejectionBinding =
        FragmentConfirmRejectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        }
    }
}
