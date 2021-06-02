package io.gnosis.safe.ui.settings.owner.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerExportKeyBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.withArgs

class OwnerExportKeyFragment : BaseViewBindingFragment<FragmentOwnerExportKeyBinding>() {

    override fun screenId() = ScreenId.OWNER_EXPORT_KEY

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerExportKeyBinding =
        FragmentOwnerExportKeyBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val key = requireArguments()[ARGS_KEY] as String
    }

    companion object {

        private const val ARGS_KEY = "args.string.key"

        fun newInstance(key: String): OwnerExportKeyFragment {
            return OwnerExportKeyFragment().withArgs(Bundle().apply {
                putString(ARGS_KEY, key)
            }) as OwnerExportKeyFragment
        }
    }
}
