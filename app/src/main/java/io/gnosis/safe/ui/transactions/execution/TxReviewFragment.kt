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
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.ParamSerializer
import io.gnosis.safe.utils.toColor
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class TxReviewFragment : BaseViewBindingFragment<FragmentTxReviewBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_EXEC_REVIEW

    private val navArgs by navArgs<TxReviewFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val hash by lazy { navArgs.hash }
    private val data by lazy { navArgs.data?.let { paramSerializer.deserializeData(it) } }
    private val executionInfo by lazy { navArgs.executionInfo?.let { paramSerializer.deserializeExecutionInfo(it) } }

    @Inject
    lateinit var paramSerializer: ParamSerializer

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
            binding.reviewAdvanced.setOnClickListener {
                findNavController().navigate(
                    TxReviewFragmentDirections.actionTxReviewFragmentToTxAdvancedParamsFragment(
                        chain = chain,
                        hash = hash,
                        data = paramSerializer.serializeData(data),
                        executionInfo = paramSerializer.serializeExecutionInfo(executionInfo)
                    )
                )
            }
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

    private fun ownerSelected(): Solidity.Address? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<String>(
            SafeOverviewBaseFragment.OWNER_SELECTED_RESULT
        )
            ?.asEthereumAddress()
    }

    private fun ownerSigned(): String? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<String>(
            SafeOverviewBaseFragment.OWNER_SIGNED_RESULT
        )
    }

    private fun resetOwnerData() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(
            SafeOverviewBaseFragment.OWNER_SELECTED_RESULT,
            null
        )
        findNavController().currentBackStackEntry?.savedStateHandle?.set(
            SafeOverviewBaseFragment.OWNER_SIGNED_RESULT,
            null
        )
    }
}
