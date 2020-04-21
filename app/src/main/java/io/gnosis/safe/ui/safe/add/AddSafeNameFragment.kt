package io.gnosis.safe.ui.safe.add

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.databinding.FragmentAddSafeNameBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment

class AddSafeNameFragment : BaseFragment<FragmentAddSafeNameBinding>() {

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSafeNameBinding =
        FragmentAddSafeNameBinding.inflate(inflater, container, false)
}
