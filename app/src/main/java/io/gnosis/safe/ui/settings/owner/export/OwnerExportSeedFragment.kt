package io.gnosis.safe.ui.settings.owner.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerExportSeedBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.withArgs

class OwnerExportSeedFragment : BaseViewBindingFragment<FragmentOwnerExportSeedBinding>() {

    override fun screenId() = ScreenId.OWNER_EXPORT_SEED

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerExportSeedBinding =
        FragmentOwnerExportSeedBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seed = requireArguments()[ARGS_SEED] as String
    }

    companion object {

        private const val ARGS_SEED = "args.string.seed"

        fun newInstance(seed: String): OwnerExportSeedFragment {
            return OwnerExportSeedFragment().withArgs(Bundle().apply {
                putString(ARGS_SEED, seed)
            }) as OwnerExportSeedFragment
        }
    }
}
