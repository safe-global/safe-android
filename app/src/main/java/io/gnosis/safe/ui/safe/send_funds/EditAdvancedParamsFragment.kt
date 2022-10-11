package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentEditAdvancedParamsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.appendLink
import io.gnosis.safe.utils.toColor
import pm.gnosis.svalinn.common.utils.visible

class EditAdvancedParamsFragment : BaseViewBindingFragment<FragmentEditAdvancedParamsBinding>() {

    private val navArgs by navArgs<EditAdvancedParamsFragmentArgs>()
    private val selectedChain by lazy { navArgs.chain }
    private val safeNonce by lazy { navArgs.safeNonce }
    private val safeTxGas by lazy { navArgs.safeTxGas }

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER_ADVANCED_PARAMS

    override suspend fun chainId() = selectedChain.chainId

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditAdvancedParamsBinding =
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
            nonceValue.setText(safeNonce)
            safeTxGas?.let {
                txGasInfo.visible(true)
                txGasLayout.visible(true)
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

            }
        }
    }
}
