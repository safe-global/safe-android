package io.gnosis.safe.ui.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSafeOverviewBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import io.gnosis.safe.ui.safe.settings.SafeSettingsFragmentDirections
import io.gnosis.safe.utils.asMiddleEllipsized
import io.gnosis.safe.utils.navigateFromChild
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class SafeOverviewFragment : BaseFragment<FragmentSafeOverviewBinding>() {

    @Inject
    lateinit var viewModel: SafeOverviewViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeOverviewBinding =
        FragmentSafeOverviewBinding.inflate(inflater, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        with(findNavController(activity!!, R.id.safe_overview_content)) {
            binding.bottomNavigation.setupWithNavController(this)
            addOnDestinationChangedListener { _, destination, _ ->
            }
        }

        with(binding) {
            safeSelection.setOnClickListener {
                SafeSelectionDialog.show(requireContext())
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer {

            when(it) {
                is SafeOverviewState.ActiveSafe -> {
                    with(binding) {
                        if (it.safe == null) {

                            navigateFromChild(childFragmentManager, SafeSettingsFragmentDirections.actionSafeSettingsFragmentToNoSafeFragment())

                            bottomNavigation.menu.findItem(R.id.safeBalancesFragment).isEnabled = false
                            bottomNavigation.menu.findItem(R.id.safeBalancesFragment).isChecked = true
                            bottomNavigation.menu.findItem(R.id.safeSettingsFragment).isEnabled = false

                            safeSelection.isEnabled = false

                            safeImage.setAddress(null)
                            safeImage.setImageResource(R.drawable.ic_no_safe_loaded_36dp)
                            safeName.visible(false)
                            safeAddress.text = getString(R.string.no_safes_loaded)


                        } else {

                            navigateFromChild(childFragmentManager, NoSafeFragmentDirections.actionNoSafeFragmentToSafeBalancesFragment())

                            bottomNavigation.menu.findItem(R.id.safeBalancesFragment).isEnabled = true
                            bottomNavigation.menu.findItem(R.id.safeSettingsFragment).isEnabled = true

                            safeSelection.isEnabled = true

                            safeImage.setAddress(it.safe.address)
                            safeName.visible(true)
                            safeName.text = it.safe.localName
                            safeAddress.text = it.safe.address.asEthereumAddressString().asMiddleEllipsized(4)
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSafe()
    }
}
