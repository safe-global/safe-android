package io.gnosis.safe.ui.safe.overview

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.databinding.FragmentSafeOverviewBinding
import io.gnosis.safe.di.components.ApplicationComponent
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.base.BaseFragment

class SafeOverviewFragment : BaseFragment<FragmentSafeOverviewBinding>() {

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeOverviewBinding =
        FragmentSafeOverviewBinding.inflate(inflater, container, false)
}
