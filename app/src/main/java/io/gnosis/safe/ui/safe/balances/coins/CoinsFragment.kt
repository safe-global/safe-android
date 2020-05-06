package io.gnosis.safe.ui.safe.balances.coins

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.databinding.FragmentCoinsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment

class CoinsFragment : BaseFragment<FragmentCoinsBinding>() {

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCoinsBinding =
        FragmentCoinsBinding.inflate(inflater, container, false)

    companion object {
        fun newInstance(): CoinsFragment = CoinsFragment()
    }
}
