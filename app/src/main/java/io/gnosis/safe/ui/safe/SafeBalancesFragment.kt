package io.gnosis.safe.ui.safe

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.databinding.FragmentSafeBalancesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment

class SafeBalancesFragment : BaseFragment<FragmentSafeBalancesBinding>() {

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeBalancesBinding =
        FragmentSafeBalancesBinding.inflate(inflater, container, false)
}
