package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSendAssetReviewBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.NavigateTo
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.safe.send_funds.EditAdvancedParamsFragment.Companion.REQUEST_EDIT_ADVANCED_PARAMS
import io.gnosis.safe.ui.safe.send_funds.EditAdvancedParamsFragment.Companion.RESULT_SAFE_TX_GAS
import io.gnosis.safe.ui.safe.send_funds.EditAdvancedParamsFragment.Companion.RESULT_SAFE_TX_NONCE
import io.gnosis.safe.utils.toColor
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject


class SendAssetReviewFragment : BaseViewBindingFragment<FragmentSendAssetReviewBinding>() {

    private val navArgs by navArgs<SendAssetReviewFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val selectedAsset by lazy { navArgs.selectedAsset as CoinsViewData.CoinBalance }
    private val fromAddress by lazy { navArgs.fromAddress.asEthereumAddress()!! }
    private val toAddress by lazy { navArgs.toAddress.asEthereumAddress()!! }
    private val amount by lazy { navArgs.amount }

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER_REVIEW

    override suspend fun chainId() = chain.chainId

    @Inject
    lateinit var viewModel: SendAssetReviewViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSendAssetReviewBinding =
        FragmentSendAssetReviewBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            title.text = getString(R.string.coins_asset_send, selectedAsset.symbol)
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
            transferAmount.setAmount(amount, selectedAsset.symbol, selectedAsset.logoUri)
            fromAddressItem.name = viewModel.activeSafe.localName
            fromAddressItem.setAddress(chain, fromAddress)
            toAddressItem.setAddress(chain, toAddress)
            reviewAdvanced.setOnClickListener {
                if (confirmButton.isEnabled) viewModel.onAdvancedParamsEdit()
            }
            confirmButton.setOnClickListener {
                viewModel.onConfirm()
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when(state) {
                is SendAssetReviewState -> {
                    state.viewAction?.let { action ->
                        when(action) {
                            is EstimationDataLoaded -> {
                                binding.confirmButton.isEnabled = true
                            }
                            is ShowError -> {
                                binding.confirmButton.isEnabled = false
                                val error = action.error.toError()
                                if (error.trackingRequired) {
                                    tracker.logException(action.error)
                                }
                                errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_assets_coins_send))
                            }
                            is NavigateTo -> {
                                findNavController().navigate(action.navDirections)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        setFragmentResultListener(REQUEST_EDIT_ADVANCED_PARAMS) { requestKey, bundle ->
            val safeTxNonce = bundle.getString(RESULT_SAFE_TX_NONCE)
            val safeTxGas = bundle.getString(RESULT_SAFE_TX_GAS)
            viewModel.updateAdvancedParams(safeTxNonce, safeTxGas)
        }

        viewModel.loadTxEstimationData(
            chain.chainId,
            fromAddress,
            toAddress,
            amount,
            selectedAsset
        )
    }

    override fun onResume() {
        super.onResume()
        if (ownerSelected() != null) {
            viewModel.initiateTransfer(ownerSelected()!!, ownerSigned())
            resetOwnerData()
        }
    }

    private fun ownerSelected(): Solidity.Address? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<String>(
            SafeOverviewBaseFragment.OWNER_SELECTED_RESULT)
            ?.asEthereumAddress()
    }

    private fun ownerSigned(): String? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<String>(SafeOverviewBaseFragment.OWNER_SIGNED_RESULT)
    }

    private fun resetOwnerData() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.OWNER_SELECTED_RESULT, null)
        findNavController().currentBackStackEntry?.savedStateHandle?.set(SafeOverviewBaseFragment.OWNER_SIGNED_RESULT, null)
    }
}
