package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentEditAdvancedParamsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.qrscanner.nullOnThrow
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.appendLink
import io.gnosis.safe.utils.toColor
import pm.gnosis.svalinn.common.utils.visible
import java.math.BigInteger

class EditAdvancedParamsFragment : BaseViewBindingFragment<FragmentEditAdvancedParamsBinding>() {

    private val navArgs by navArgs<EditAdvancedParamsFragmentArgs>()
    private val selectedChain by lazy { navArgs.chain }
    private val safeNonce by lazy { BigInteger(navArgs.safeNonce) }
    private val minSafeNonce by lazy { BigInteger(navArgs.minSafeNonce) }

    // should be nil for contracts of v1.3.0 and higher
    private val safeTxGas by lazy { navArgs.safeTxGas?.let { BigInteger(it) } }
    private val proposedSafeTxGas by lazy { navArgs.proposedSafeTxGas?.let { BigInteger(it) } }

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER_ADVANCED_PARAMS

    override suspend fun chainId() = selectedChain.chainId

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditAdvancedParamsBinding =
        FragmentEditAdvancedParamsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            chainRibbon.text = selectedChain.name
            chainRibbon.setTextColor(
                selectedChain.textColor.toColor(
                    requireContext(),
                    R.color.white
                )
            )
            chainRibbon.setBackgroundColor(
                selectedChain.backgroundColor.toColor(
                    requireContext(),
                    R.color.primary
                )
            )
            nonceValue.setText(safeNonce.toString())
            nonceValue.doOnTextChanged { text, _, _, _ ->
                validateInputs()
            }
            safeTxGas?.let {
                txGasInfo.visible(true)
                txGasLayout.visible(true)
                txGasValue.setText(it.toString())
                txGasValue.doOnTextChanged { text, _, _, _ ->
                    validateInputs()
                }
            }
            configHowtoLink.appendLink(
                urlText = resources.getString(R.string.tx_advanced_params_config_howto),
                url = resources.getString(R.string.tx_advanced_params_config_howto_link),
                linkIcon = R.drawable.ic_external_link_green_16dp
            )
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            saveButton.setOnClickListener {
                setFragmentResult(
                    REQUEST_EDIT_ADVANCED_PARAMS,
                    bundleOf(
                        RESULT_SAFE_TX_NONCE to binding.nonceValue.text.toString(),
                        RESULT_SAFE_TX_GAS to safeTxGas?.let { binding.txGasValue.text.toString() }
                    )
                )
            }
        }
    }

    private fun validateInputs() {

        binding.saveButton.isEnabled = false

        var canBeSaved = true

        val nonce = nullOnThrow { BigInteger(binding.nonceValue.text.toString()) }

        when {

            nonce == null -> {
                canBeSaved = false
                binding.nonceLayout.error = null
            }

            nonce < minSafeNonce -> {
                canBeSaved = false
                binding.nonceLayout.error = getString(R.string.tx_safe_nonce_error_executed)
            }

            nonce == minSafeNonce -> {
                binding.nonceLayout.error = null
            }

            nonce > minSafeNonce -> {
                val offset = (nonce - minSafeNonce).toInt()
                binding.nonceLayout.error = resources.getQuantityString(R.plurals.tx_safe_nonce_error_offset, offset, offset)
            }
        }

        safeTxGas?.let {

            val txGas = nullOnThrow { BigInteger(binding.txGasValue.text.toString()) }

            when {

                txGas == null -> {
                    canBeSaved = false
                    binding.txGasLayout.error = null
                }

                txGas < proposedSafeTxGas -> {
                    binding.txGasLayout.error = getString(R.string.tx_safe_tx_gas_error_insufficient)
                }

                else -> {
                    binding.txGasLayout.error = null
                }
            }
        }

        binding.saveButton.isEnabled = canBeSaved
    }

    companion object {
        const val REQUEST_EDIT_ADVANCED_PARAMS = "request_edit_advanced_params"
        const val RESULT_SAFE_TX_NONCE = "result_safe_tx_nonce"
        const val RESULT_SAFE_TX_GAS = "result_safe_tx_gas"
    }
}
