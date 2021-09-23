package io.gnosis.safe.ui.settings.owner.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerInfoLedgerBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.ledger.LedgerDeviceListFragment

class OwnerInfoLedgerFragment : BaseViewBindingFragment<FragmentOwnerInfoLedgerBinding>() {

    override fun screenId() = ScreenId.OWNER_LEDGER_INFO

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerInfoLedgerBinding =
        FragmentOwnerInfoLedgerBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            nextButton.setOnClickListener {
                findNavController().navigate(
                    OwnerInfoLedgerFragmentDirections.actionOwnerInfoLedgerFragmentToLedgerDeviceListFragment(
                        LedgerDeviceListFragment.Mode.ADDRESS_SELECTION.name
                    )
                )
            }
            backButton.setOnClickListener { findNavController().navigateUp() }
        }
    }
}
