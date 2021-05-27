package io.gnosis.safe.ui.settings.owner.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerInfoBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class OwnerInfoFragment : BaseViewBindingFragment<FragmentOwnerInfoBinding>() {

    override fun screenId() = ScreenId.OWNER_INFO

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerInfoBinding =
        FragmentOwnerInfoBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            nextButton.setOnClickListener {
                findNavController().navigate(OwnerInfoFragmentDirections.actionOwnerInfoFragmentToOwnerSeedPhraseFragment())
            }
            backButton.setOnClickListener { findNavController().navigateUp() }
            infoPrivateKey.addInfoLink(
                getString(R.string.import_owner_key_intro_private_key_help),
                getString(R.string.owner_key_intro_private_key_url)
            )
        }
    }
}
