package io.gnosis.safe.ui.safe

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.databinding.FragmentNoSafesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment

class NoSafeFragment : BaseFragment<FragmentNoSafesBinding>() {

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNoSafesBinding =
        FragmentNoSafesBinding.inflate(inflater, container, false)
}
