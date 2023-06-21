package io.gnosis.safe.ui.transactions.execution

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTxEditFeeLegacyBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.appendLink
import io.gnosis.safe.utils.toColor
import java.math.BigInteger
import javax.inject.Inject

class TxEditFeeLegacyFragment : BaseViewBindingFragment<FragmentTxEditFeeLegacyBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS_EXEC_EDIT_FEE

    private val navArgs by navArgs<TxEditFeeLegacyFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val nonce by lazy { navArgs.nonce }
    private val minNonce by lazy { BigInteger(navArgs.minNonce) }
    private val gasLimit by lazy { navArgs.gasLimit }
    private val gasPrice by lazy { navArgs.gasPrice }

    @Inject
    lateinit var viewModel: TxEditFeeViewModel


    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTxEditFeeLegacyBinding =
        FragmentTxEditFeeLegacyBinding.inflate(inflater, container, false)

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
                viewModel.validateLegacyInputs(
                    requireContext(),
                    minNonce,
                    nonceValue.text.toString(),
                    gasLimitValue.text.toString(),
                    gasPriceValue.text.toString()
                )
            }
            gasLimitValue.setText(gasLimit)
            gasLimitValue.doOnTextChanged { text, _, _, _ ->
                viewModel.validateLegacyInputs(
                    requireContext(),
                    minNonce,
                    nonceValue.text.toString(),
                    gasLimitValue.text.toString(),
                    gasPriceValue.text.toString()
                )
            }
            gasPriceValue.setText(gasPrice)
            gasPriceValue.doOnTextChanged { text, _, _, _ ->
                viewModel.validateLegacyInputs(
                    requireContext(),
                    minNonce,
                    nonceValue.text.toString(),
                    gasLimitValue.text.toString(),
                    gasPriceValue.text.toString()
                )
            }
            totalFeeLabel.text = getString(
                R.string.tx_exec_estimated_fee_price,
                viewModel.totalFeeLegacy(
                    requireContext(),
                    chain,
                    gasLimitValue.text.toString(),
                    gasPriceValue.text.toString()
                )
            )
            configHowtoLink.appendLink(
                urlText = resources.getString(R.string.tx_advanced_params_config_howto),
                url = resources.getString(R.string.tx_advanced_params_config_howto_link),
                linkIcon = R.drawable.ic_external_link_green_16dp
            )
            saveButton.setOnClickListener {
                setFragmentResult(
                    REQUEST_EDIT_FEE,
                    bundleOf(
                        RESULT_NONCE to nonceValue.text.toString(),
                        RESULT_GAS_LIMIT to gasLimitValue.text.toString(),
                        RESULT_GAS_PRICE to gasPriceValue.text.toString(),
                    )
                )
                Navigation.findNavController(it).navigateUp()
            }
            saveButton.isEnabled = false
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TxEditFeeState -> {
                    binding.saveButton.isEnabled = state.saveEnabled
                    state.viewAction?.let { action ->
                        when (action) {
                            is ValidateLegacyFeeData -> {
                                validateInputs(
                                    action.nonceError,
                                    action.gasLimitError,
                                    action.gasPriceError
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
        gasPriceError: String?,
    ) {
        with(binding) {
            nonceLayout.error = nonceError
            gasLimitLayout.error = gasLimitError
            gasPriceLayout.error = gasPriceError
        }
    }

    private fun updateTotalFee() {
        with(binding) {
            totalFeeLabel.text = getString(
                R.string.tx_exec_estimated_fee_price,
                viewModel.totalFeeLegacy(
                    requireContext(),
                    chain,
                    gasLimitValue.text.toString(),
                    gasPriceValue.text.toString()
                )
            )
        }
    }

    companion object {
        const val REQUEST_EDIT_FEE = "request_edit_fee"
        const val RESULT_NONCE = "result_nonce"
        const val RESULT_GAS_LIMIT = "result_gas_limit"
        const val RESULT_GAS_PRICE = "result_gas_price"
    }
}
