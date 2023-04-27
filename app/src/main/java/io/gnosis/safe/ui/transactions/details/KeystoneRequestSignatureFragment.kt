package io.gnosis.safe.ui.transactions.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.keystone.sdk.KeystoneEthereumSDK
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentKeystoneRequestSignatureBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class KeystoneRequestSignatureFragment: BaseViewBindingFragment<FragmentKeystoneRequestSignatureBinding>() {
    private val navArgs by navArgs<KeystoneRequestSignatureFragmentArgs>()
    private val owner by lazy { navArgs.owner }
    private val chain by lazy { navArgs.chain }
    private val safeTxHash by lazy { navArgs.safeTxHash }

    override fun screenId() = ScreenId.KEYSTONE_REQUEST_SIGNATURE

    @Inject
    lateinit var viewModel: KeystoneSignViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKeystoneRequestSignatureBinding =
        FragmentKeystoneRequestSignatureBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                onBackNavigation()
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
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            state.viewAction?.let { action ->
                when (action) {
                    is Loading -> {

                    }

                    is UnsignedUrReady -> {
                        with(binding) {
                            ownerQrCode.setImageBitmap(action.qrCode)
                        }
                    }
                }
            }
        }

        owner?.asEthereumAddress()?.let {
            viewModel.setSignRequestUREncoder(
                ownerAddress = it,
                safeTxHash = safeTxHash!!,
                signType = KeystoneEthereumSDK.DataType.PersonalMessage,
                chainId = chain.chainId.toInt()
            )
        }
    }

    private fun onBackNavigation() {
        viewModel.onPause()
        findNavController().navigateUp()
    }
}