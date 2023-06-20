package io.gnosis.safe.ui.transactions.execution

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTxEditFeeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.appendLink
import io.gnosis.safe.utils.toColor
import java.math.BigInteger
import javax.inject.Inject

class TxEditFee1559Fragment : BaseViewBindingFragment<FragmentTxEditFeeBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_EXEC_EDIT_FEE

    private val navArgs by navArgs<TxEditFee1559FragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val nonce by lazy { navArgs.nonce }
    private val minNonce by lazy { BigInteger(navArgs.minNonce) }
    private val gasLimit by lazy { navArgs.gasLimit }
    private val maxPriorityFee by lazy { navArgs.maxPriorityFee }
    private val maxFee by lazy { navArgs.maxFee }

    @Inject
    lateinit var viewModel: TxEditFeeViewModel


    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTxEditFeeBinding =
        FragmentTxEditFeeBinding.inflate(inflater, container, false)

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
            nonceValue.setText(nonce)
            nonceValue.doOnTextChanged { text, _, _, _ ->
                viewModel.validate1559Inputs(
                    requireContext(),
                    minNonce,
                    nonceValue.text.toString(),
                    gasLimitValue.text.toString(),
                    maxPriorityFeeValue.text.toString(),
                    maxFeeValue.text.toString()
                )
            }
            gasLimitValue.setText(gasLimit)
            gasLimitValue.doOnTextChanged { text, _, _, _ ->
                viewModel.validate1559Inputs(
                    requireContext(),
                    minNonce,
                    nonceValue.text.toString(),
                    gasLimitValue.text.toString(),
                    maxPriorityFeeValue.text.toString(),
                    maxFeeValue.text.toString()
                )
            }
            maxPriorityFeeValue.setText(maxPriorityFee)
            maxPriorityFeeValue.doOnTextChanged { text, _, _, _ ->
                viewModel.validate1559Inputs(
                    requireContext(),
                    minNonce,
                    nonceValue.text.toString(),
                    gasLimitValue.text.toString(),
                    maxPriorityFeeValue.text.toString(),
                    maxFeeValue.text.toString()
                )
            }
            maxFeeValue.setText(maxFee)
            maxFeeValue.doOnTextChanged { text, _, _, _ ->
                viewModel.validate1559Inputs(
                    requireContext(),
                    minNonce,
                    nonceValue.text.toString(),
                    gasLimitValue.text.toString(),
                    maxPriorityFeeValue.text.toString(),
                    maxFeeValue.text.toString()
                )
            }
            totalFeeLabel.text = getString(
                R.string.tx_exec_estimated_fee_price,
                viewModel.totalFee1559(
                    requireContext(),
                    chain,
                    gasLimitValue.text.toString(),
                    maxFeeValue.text.toString()
                )
            )
            configHowtoLink.appendLink(
                urlText = resources.getString(R.string.tx_advanced_params_config_howto),
                url = resources.getString(R.string.tx_advanced_params_config_howto_link),
                linkIcon = R.drawable.ic_external_link_green_16dp
            )
            saveButton.setOnClickListener {

            }
            saveButton.isEnabled = false
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TxEditFeeState -> {
                    binding.saveButton.isEnabled = state.saveEnabled
                    state.viewAction?.let { action ->
                        when (action) {
                            is Validate1559FeeData -> {
                                validateInputs(
                                    action.nonceError,
                                    action.gasLimitError,
                                    action.maxPriorityFeeError,
                                    action.maxFeeError
                                )
                                updateTotalFee()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateInputs(
        nonceError: String?,
        gasLimitError: String?,
        maxPriorityFeeError: String?,
        maxFeeError: String?
    ) {
        with(binding) {
            nonceLayout.error = nonceError
            gasLimitLayout.error = gasLimitError
            maxPriorityFeeLayout.error = maxPriorityFeeError
            maxFeeLayout.error = maxFeeError
        }
    }

    private fun updateTotalFee() {
        with(binding) {
            totalFeeLabel.text = getString(
                R.string.tx_exec_estimated_fee_price,
                viewModel.totalFee1559(
                    requireContext(),
                    chain,
                    gasLimitValue.text.toString(),
                    maxFeeValue.text.toString()
                )
            )
        }
    }
}
