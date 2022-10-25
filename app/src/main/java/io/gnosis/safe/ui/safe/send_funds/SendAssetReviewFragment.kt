package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSendAssetReviewBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class SendAssetReviewFragment : BaseViewBindingFragment<FragmentSendAssetReviewBinding>() {

    private val navArgs by navArgs<SendAssetReviewFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val selectedAsset by lazy { navArgs.selectedAsset as CoinsViewData.CoinBalance }
    private val fromAddress by lazy { navArgs.fromAddress.asEthereumAddress() }
    private val toAddress by lazy { navArgs.toAddress.asEthereumAddress() }

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER_REVIEW

    override suspend fun chainId() = chain.chainId

    @Inject
    lateinit var viewModel: SendAssetReviewViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSendAssetReviewBinding =
        FragmentSendAssetReviewBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when(state) {
                is SendAssetReviewState -> {
                    state.viewAction?.let { action ->
                        when(action) {

                        }
                    }
                }
            }
        }
    }
}
