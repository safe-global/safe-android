package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSendAssetBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.AddressInputHelper
import io.gnosis.safe.toError
import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import java.math.BigDecimal
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

    private var recipientInput: String? = null
    private var amountInput: BigDecimal? = null

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

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
            senderItem.name = viewModel.activeSafe.localName
            senderItem.setAddress(chain, viewModel.activeSafe.address)
            recipientAddressInputLayout.hint = getString(R.string.coins_asset_send_recepient)
            recipientAddressInputLayout.setOnClickListener {
                addressInputHelper.showDialog()
            }
            recipientInput?.asEthereumAddress()?.let {
                updateAddress(it)
            }
            balanceValue.text = "${selectedAsset.balanceFormatted} ${selectedAsset.symbol}"
            assetSendAmount.setAssetLogo(selectedAsset.logoUri)
            assetSendAmount.doOnTextChanged { text, _, _, _ ->
                amountInput = nullOnThrow { BigDecimal(text.toString()) }
                reviewButton.isEnabled = viewModel.validateInputs(recipientInput, amountInput)
            }
            sendMax.setOnClickListener {
                amountInput = selectedAsset.balance
                assetSendAmount.setAmount(selectedAsset.balance)
            }
            reviewButton.setOnClickListener {
                viewModel.onReviewButtonClicked(chain, selectedAsset, recipientInput!!, amountInput!!)
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SendAssetState -> {
                    state.viewAction?.let { action ->
                        when (action) {
                            is NavigateTo -> {
                                findNavController().navigate(action.navDirections)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun updateAddress(address: Solidity.Address) {
        recipientInput = address.asEthereumAddressString()
        with(binding) {
            reviewButton.isEnabled = viewModel.validateInputs(recipientInput, amountInput)
            recipientAddressInputLayout.setNewAddress(address)
        }
    }

    private fun handleValid(address: Solidity.Address) {
        with(binding) {
            if (recipientAddressInputLayout.address == null) {
                recipientAddressInputLayout.setNewAddress(address)
            }
            reviewButton.isEnabled = viewModel.validateInputs(recipientInput, amountInput)
        }
    }

    private fun handleError(throwable: Throwable, input: String? = null) {
        Timber.e(throwable)
        input?.let {
            recipientInput = it
        }
        with(binding) {
            reviewButton.isEnabled = false

            val error = throwable.toError()
            if (error.trackingRequired) {
                tracker.logException(throwable)
            }

            val errorMsg = error.message(requireContext(), R.string.error_description_safe_address)

            recipientAddressInputLayout.setError(errorMsg, recipientInput)
        }
    }
}
