package io.gnosis.safe.ui.settings.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerHowToAddBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class OwnerHowToAddFragment : BaseViewBindingFragment<FragmentOwnerHowToAddBinding>() {

    override fun screenId() = ScreenId.OWNER_HOW_TO_ADD

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerHowToAddBinding =
        FragmentOwnerHowToAddBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            itemImportExisting.setOnClickListener {
                findNavController().navigate(OwnerHowToAddFragmentDirections.actionOwnerHowToAddFragmentToOwnerInfoFragment())
            }
            itemCreateNew.setOnClickListener {
                findNavController().navigate(OwnerHowToAddFragmentDirections.actionOwnerHowToAddFragmentToOwnerInfoGenerateFragment())
            }
        }
    }
}
