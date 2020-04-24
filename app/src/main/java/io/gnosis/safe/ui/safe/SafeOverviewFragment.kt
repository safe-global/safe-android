package io.gnosis.safe.ui.safe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentSafeOverviewBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import pm.gnosis.svalinn.common.utils.transaction
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
                            childFragmentManager.transaction {
                                replace(R.id.content, NoSafeFragment())
                            }
                        } else {
                            safeImage.setAddress(it.safe?.address)
                            safeName.text = it.safe?.localName
                            safeAddress.text = it.safe?.address?.asEthereumAddressString()
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
