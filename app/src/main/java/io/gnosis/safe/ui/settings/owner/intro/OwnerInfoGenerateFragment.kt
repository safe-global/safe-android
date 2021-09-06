package io.gnosis.safe.ui.settings.owner.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.data.models.Owner
import io.gnosis.data.models.OwnerTypeConverter
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerInfoGenerateBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import java.math.BigInteger
import javax.inject.Inject

class OwnerInfoGenerateFragment : BaseViewBindingFragment<FragmentOwnerInfoGenerateBinding>() {

    override fun screenId() = ScreenId.OWNER_GENERATE_INFO

    override suspend fun chainId(): BigInteger? = null

    @Inject
    lateinit var viewModel: OwnerGenerateViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerInfoGenerateBinding =
        FragmentOwnerInfoGenerateBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            nextButton.setOnClickListener {
                /*
                    proceed with key generation
                    app creates a unique seed phrase and derives the key at index 0, and uses that to show the key address in the next screen.
                    When the user goes back to this screen and presses “Next” again then the user must be able to see the same key again.
                */
                 viewModel.ownerData?.let {
                     findNavController().navigate(OwnerInfoGenerateFragmentDirections.actionOwnerInfoGenerateFragmentToOwnerEnterNameFragment(
                         ownerAddress = it.address,
                         ownerKey = it.key,
                         fromSeedPhrase = false,
                         ownerSeedPhrase = it.mnemonic,
                         ownerType = OwnerTypeConverter().toValue(Owner.Type.GENERATED)
                     ))
                 }
            }
            backButton.setOnClickListener { findNavController().navigateUp() }
            infoPrivateKey.addInfoLink(
                getString(R.string.generate_owner_key_intro_private_key_help),
                getString(R.string.owner_key_intro_private_key_url)
            )
        }
    }
}
