package io.gnosis.safe.ui.transactions.execution

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTxReviewBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor
import javax.inject.Inject

class TxReviewFragment : BaseViewBindingFragment<FragmentTxReviewBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_EXEC_REVIEW

    private val navArgs by navArgs<TxReviewFragmentArgs>()
    private val chain by lazy { navArgs.chain }

    @Inject
    lateinit var viewModel: TxReviewViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTxReviewBinding =
        FragmentTxReviewBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            chainRibbon.text = chain.name
            chainRibbon.setTextColor(
                chain.textColor.toColor(
                    requireContext(),
                    R.color.white
                )
            )
            chainRibbon.setBackgroundColor(
                chain.backgroundColor.toColor(
                    requireContext(),
                    R.color.primary
                )
            )
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TxReviewState -> {
                    state.viewAction?.let { action ->

                    }
                }
            }
        }
    }
}
