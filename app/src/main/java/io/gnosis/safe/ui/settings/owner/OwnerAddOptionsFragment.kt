package io.gnosis.safe.ui.settings.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerAddOptionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import java.math.BigInteger

class OwnerAddOptionsFragment : BaseViewBindingFragment<FragmentOwnerAddOptionsBinding>() {

    override fun screenId() = ScreenId.OWNER_ADD_OPTIONS

    override suspend fun chainId(): BigInteger? = null

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerAddOptionsBinding =
        FragmentOwnerAddOptionsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            itemImportExisting.setOnClickListener {
                findNavController().navigate(OwnerAddOptionsFragmentDirections.actionOwnerAddOptionsFragmentToOwnerInfoFragment())
            }
            itemCreateNew.setOnClickListener {
                findNavController().navigate(OwnerAddOptionsFragmentDirections.actionOwnerAddOptionsFragmentToOwnerInfoGenerateFragment())
            }
            itemConnectLedger.setOnClickListener {
                findNavController().navigate(OwnerAddOptionsFragmentDirections.actionOwnerAddOptionsFragmentToOwnerInfoLedgerFragment())
            }
        }
    }
}
