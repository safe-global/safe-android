package io.gnosis.safe.ui.assets.collectibles

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentCollectiblesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment

class CollectiblesFragment : BaseViewBindingFragment<FragmentCollectiblesBinding>() {

    override fun screenId() = ScreenId.ASSETS_COLLECTIBLES

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCollectiblesBinding =
        FragmentCollectiblesBinding.inflate(inflater, container, false)

    companion object {
        fun newInstance(): CollectiblesFragment = CollectiblesFragment()
    }
}
