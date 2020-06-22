package io.gnosis.safe.ui.safe.empty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentNoSafesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.withArgs

class NoSafeFragment : BaseViewBindingFragment<FragmentNoSafesBinding>() {

    override fun screenId() = when (requireArguments()[ARGS_POSITION] as Position) {
        Position.BALANCES -> ScreenId.ASSETS_NO_SAFE
        Position.TRANSACTIONS -> ScreenId.TRANSACTIONS_NO_SAFE
        Position.SETTINGS -> ScreenId.SETTINGS_SAFE_NO_SAFE
    }

    override fun inject(component: ViewComponent) {}

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNoSafesBinding =
        FragmentNoSafesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadSafeButton.setOnClickListener {
            requireParentFragment().findNavController().navigate(R.id.action_to_add_safe_nav)
        }
    }

    enum class Position {
        BALANCES,
        TRANSACTIONS,
        SETTINGS
    }

    companion object {

        private const val ARGS_POSITION = "args.serializable.position"

        fun newInstance(position: Position): NoSafeFragment {
           return NoSafeFragment().withArgs(Bundle().apply {
               putSerializable(ARGS_POSITION, position)
           }) as NoSafeFragment
        }
    }
}
