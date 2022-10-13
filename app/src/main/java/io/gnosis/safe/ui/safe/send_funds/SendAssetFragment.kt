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
import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor
import pm.gnosis.model.Solidity
import javax.inject.Inject

class SendAssetFragment : BaseViewBindingFragment<FragmentSendAssetBinding>() {

    private val navArgs by navArgs<SendAssetFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val selectedAsset by lazy { navArgs.selectedAsset as CoinsViewData.CoinBalance }

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER

    override suspend fun chainId() = chain.chainId

    @Inject
    lateinit var viewModel: SendAssetViewModel

    private val addressInputHelper by lazy {
        AddressInputHelper(
            fragment = this,
            tracker = tracker,
            selectedChain = chain,
            addressCallback = ::updateAddress,
            errorCallback = ::handleError,
            enableUD = viewModel.enableUD(chain),
            enableENS = viewModel.enableENS(chain)
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
            title.text = getString(R.string.coins_asset_send, selectedAsset.symbol)
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
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
            senderItem.showLink = false
            recepientAddressInputLayout.hint = getString(R.string.coins_asset_send_recepient)
            recepientAddressInputLayout.setOnClickListener {
                addressInputHelper.showDialog()
            }
            balanceValue.text = "${selectedAsset.balanceFormatted} ${selectedAsset.symbol}"
            sendMax.setOnClickListener {

            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SendAssetState -> {
                    state.viewAction?.let { action ->
                        when (action) {
                            is UpdateActiveSafe -> {
                                binding.senderItem.name = action.newSafe!!.localName
                                binding.senderItem.setAddress(chain, action.newSafe.address)
                            }
                        }
                    }
                }
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
