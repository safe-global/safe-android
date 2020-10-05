package io.gnosis.safe.ui.settings.owner.import

import android.view.LayoutInflater
import android.view.ViewGroup
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentImportOwnerKeyBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import javax.inject.Inject

class ImportOwnerKeyFragment : BaseViewBindingFragment<FragmentImportOwnerKeyBinding>() {

    @Inject
    lateinit var viewModel: ImportOwnerKeyViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentImportOwnerKeyBinding =
        FragmentImportOwnerKeyBinding.inflate(inflater, container, false)

    override fun screenId(): ScreenId = ScreenId.OWNER_ENTER_SEED

    override fun viewModelProvider() = this

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }
}
