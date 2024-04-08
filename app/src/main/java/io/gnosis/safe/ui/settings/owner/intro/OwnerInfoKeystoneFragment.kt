package io.gnosis.safe.ui.settings.owner.intro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.keystone.sdk.KeystoneSDK
import com.sparrowwallet.hummingbird.UR
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerInfoKeystoneBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.qrscanner.HasFinished
import io.gnosis.safe.qrscanner.IsValid
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.keystone.KeystoneAccount
import io.gnosis.safe.ui.settings.owner.keystone.KeystoneMultiAccount
import io.gnosis.safe.utils.handleQrCodeActivityResult

class OwnerInfoKeystoneFragment : BaseViewBindingFragment<FragmentOwnerInfoKeystoneBinding>() {

    private val keystoneSDK = KeystoneSDK()
    private var ur: UR? = null

    companion object {
        const val UR_PREFIX_OF_HDKEY = "UR:CRYPTO-HDKEY"
        const val UR_PREFIX_OF_ACCOUNT = "UR:CRYPTO-ACCOUNT"
    }

    override fun screenId() = ScreenId.OWNER_KEYSTONE_INFO

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOwnerInfoKeystoneBinding =
        FragmentOwnerInfoKeystoneBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            nextButton.setOnClickListener {
                QRCodeScanActivity.startForResult(
                    this@OwnerInfoKeystoneFragment,
                    getString(R.string.import_owner_key_keystone_scanner_description),
                    ::validator
                )
                tracker.logScreen(ScreenId.SCANNER_KEYSTONE, null)
            }
            backButton.setOnClickListener { findNavController().navigateUp() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, {

            if (it.startsWith(UR_PREFIX_OF_HDKEY)) {
                this.ur?.let { ur ->
                    // TODO KST: parseExtendedPublicKey(ur) -> parseAccount(ur)
                    val hdKey = keystoneSDK.parseAccount(ur)

                    findNavController().navigate(
                        OwnerInfoKeystoneFragmentDirections.actionOwnerInfoKeystoneFragmentToKeystoneOwnerSelectionFragment(
                            KeystoneAccount(hdKey),
                            null
                        )
                    )
                }
            } else if (it.startsWith(UR_PREFIX_OF_ACCOUNT)) {
                this.ur?.let { ur ->
                    // TODO KST: parseMultiPublicKeys -> parseCryptoAccount (or parseMultiAccounts?)
                    val multiHDKeys = keystoneSDK.parseCryptoAccount(ur)

                    findNavController().navigate(
                        OwnerInfoKeystoneFragmentDirections.actionOwnerInfoKeystoneFragmentToKeystoneOwnerSelectionFragment(
                            null,
                            KeystoneMultiAccount(multiHDKeys)
                        )
                    )
                }
            }
        })
    }

    private fun validator(scannedValue: String): Pair<IsValid, HasFinished> {
        return if (scannedValue.startsWith(UR_PREFIX_OF_HDKEY) || scannedValue.startsWith(
                UR_PREFIX_OF_ACCOUNT
            )
        ) {
            keystoneSDK.decodeQR(scannedValue)?.let {
                this.ur = it.ur
                keystoneSDK.resetQRDecoder()
                Pair(true, true)
            } ?: Pair(true, false)
        } else {
            Pair(false, true)
        }
    }
}
