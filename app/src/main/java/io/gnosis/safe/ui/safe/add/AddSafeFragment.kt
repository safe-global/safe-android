package io.gnosis.safe.ui.safe.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.databinding.FragmentAddSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import javax.inject.Inject

class AddSafeFragment : BaseFragment<FragmentAddSafeBinding>() {

    @Inject
    lateinit var viewModel: AddSafeViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSafeBinding =
        FragmentAddSafeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
