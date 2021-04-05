package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionDetailsCreationBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class SigningOwnerSelectionFragment  : BaseViewBindingFragment<FragmentTransactionDetailsCreationBinding>() {


    // TODO: add navarg for rejection or confirmation

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionDetailsCreationBinding {
        TODO("Not yet implemented")
    }

    override fun screenId(): ScreenId? {
        TODO("Not yet implemented")
    }

    override fun inject(component: ViewComponent) {
        TODO("Not yet implemented")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            title.text = ""
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
//        updateUi()
    }

}
