package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSendAssetBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.AddressInputHelper
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor
import pm.gnosis.model.Solidity
import javax.inject.Inject

class SendAssetFragment : BaseViewBindingFragment<FragmentSendAssetBinding>() {

    private val navArgs by navArgs<SendAssetFragmentArgs>()
    private val selectedChain by lazy { navArgs.chain }

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER

    override suspend fun chainId() = selectedChain.chainId

    @Inject
    lateinit var viewModel: SendAssetViewModel

    private val addressInputHelper by lazy {
        AddressInputHelper(
            fragment = this,
            tracker = tracker,
            selectedChain = selectedChain,
            addressCallback = ::updateAddress,
            errorCallback = ::handleError,
            enableUD = viewModel.enableUD(selectedChain),
            enableENS = viewModel.enableENS(selectedChain)
        )
    }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSendAssetBinding =
        FragmentSendAssetBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
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
            recepientAddressInputLayout.hint = getString(R.string.coins_asset_send_recepient)
            recepientAddressInputLayout.setOnClickListener {
                addressInputHelper.showDialog()
            }
        }
    }

    private fun updateAddress(address: Solidity.Address) {

    }

    private fun handleValid(address: Solidity.Address) {

    }

    private fun handleError(throwable: Throwable, input: String? = null) {

    }
}
