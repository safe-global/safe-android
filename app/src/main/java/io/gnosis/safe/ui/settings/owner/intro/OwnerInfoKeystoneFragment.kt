package io.gnosis.safe.ui.settings.owner.intro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerInfoKeystoneBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.OwnerSeedPhraseFragmentDirections
import io.gnosis.safe.utils.handleQrCodeActivityResult

class OwnerInfoKeystoneFragment : BaseViewBindingFragment<FragmentOwnerInfoKeystoneBinding>() {
    companion object {
        const val UR_PREFIX_OF_HDKEY = "UR:CRYPTO-HDKEY"
        const val UR_PREFIX_OF_ACCOUNT = "UR:CRYPTO-ACCOUNT"
    }
    override fun screenId() = ScreenId.OWNER_KEYSTONE_INFO

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerInfoKeystoneBinding =
        FragmentOwnerInfoKeystoneBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            nextButton.setOnClickListener {
                QRCodeScanActivity.startForResult(this@OwnerInfoKeystoneFragment)
                tracker.logScreen(ScreenId.SCANNER, null)
            }
            backButton.setOnClickListener { findNavController().navigateUp() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, {
            if (it.startsWith(UR_PREFIX_OF_HDKEY) || it.startsWith(UR_PREFIX_OF_ACCOUNT)) {
                print(it)
            }
        })
    }
}
