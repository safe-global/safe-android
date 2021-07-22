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
import java.math.BigInteger

class OwnerExportSeedFragment : BaseViewBindingFragment<FragmentOwnerExportSeedBinding>() {

    override fun screenId() = ScreenId.OWNER_EXPORT_SEED

    override suspend fun chainId(): BigInteger? = null

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerExportSeedBinding =
        FragmentOwnerExportSeedBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seed = requireArguments()[ARGS_SEED] as String
        val seedWords = seed.split(" ")
        with(binding) {
            seedWord1.word = seedWords[0]
            seedWord2.word = seedWords[1]
            seedWord3.word = seedWords[2]
            seedWord4.word = seedWords[3]
            seedWord5.word = seedWords[4]
            seedWord6.word = seedWords[5]
            seedWord7.word = seedWords[6]
            seedWord8.word = seedWords[7]
            seedWord9.word = seedWords[8]
            seedWord10.word = seedWords[9]
            seedWord11.word = seedWords[10]
            seedWord12.word = seedWords[11]
        }
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
