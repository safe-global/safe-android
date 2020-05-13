package io.gnosis.safe.ui.safe.balances.collectibles

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.databinding.FragmentCollectiblesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment

class CollectiblesFragment : BaseFragment<FragmentCollectiblesBinding>() {

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCollectiblesBinding =
        FragmentCollectiblesBinding.inflate(inflater, container, false)

    companion object {
        fun newInstance(): CollectiblesFragment = CollectiblesFragment()
    }
}
