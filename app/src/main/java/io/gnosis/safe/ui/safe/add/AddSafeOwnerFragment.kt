package io.gnosis.safe.ui.safe.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAddSafeOwnerBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import javax.inject.Inject

class AddSafeOwnerFragment : BaseViewBindingFragment<FragmentAddSafeOwnerBinding>()  {

    @Inject
    lateinit var viewModel: AddSafeOwnerViewModel

    private val navArgs by navArgs<AddSafeOwnerFragmentArgs>()
    private val name by lazy { navArgs.safeName }

    override fun screenId(): ScreenId? = ScreenId.SAFE_ADD_OWNER

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSafeOwnerBinding =
        FragmentAddSafeOwnerBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener { findNavController().navigateUp() }
            safeName.text = name
        }
    }
}
