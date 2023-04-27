package io.gnosis.safe.ui.transactions.details

import android.content.Intent
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
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.handleQrCodeActivityResult
import io.gnosis.safe.utils.toColor
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class KeystoneRequestSignatureFragment :
    BaseViewBindingFragment<FragmentKeystoneRequestSignatureBinding>() {
    private val navArgs by navArgs<KeystoneRequestSignatureFragmentArgs>()
    private val owner by lazy { navArgs.owner }
    private val signingMode by lazy { navArgs.signingMode }
    private val chain by lazy { navArgs.chain }
    private val safeTxHash by lazy { navArgs.safeTxHash }

    override fun screenId() = ScreenId.KEYSTONE_REQUEST_SIGNATURE

    @Inject
    lateinit var viewModel: KeystoneSignViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentKeystoneRequestSignatureBinding =
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

            getSignatureButton.setOnClickListener {
                QRCodeScanActivity.startForResult(
                    this@KeystoneRequestSignatureFragment,
                    getString(R.string.import_owner_key_keystone_scanner_description),
                    viewModel::validator
                )
                tracker.logScreen(ScreenId.SCANNER, null)
            }
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
        viewModel.stopUpdatingQrCode()
        findNavController().navigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, {
            viewModel.handleQrResult()
        })
    }
}