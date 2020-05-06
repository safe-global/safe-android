package io.gnosis.safe.ui.safe.balances.coins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import io.gnosis.data.models.Safe
import io.gnosis.safe.databinding.FragmentCoinsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import io.gnosis.safe.ui.safe.balances.ActiveSafeListener
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class CoinsFragment : BaseFragment<FragmentCoinsBinding>(), ActiveSafeListener {

    @Inject
    lateinit var viewModel: CoinsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCoinsBinding =
        FragmentCoinsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner, Observer {
            binding.text.text = it.toString()
        })
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadFor()
    }

    override fun onActiveSafeChanged() {
        viewModel.loadFor()
    }

    companion object {
        fun newInstance(): CoinsFragment = CoinsFragment()

    }
}
