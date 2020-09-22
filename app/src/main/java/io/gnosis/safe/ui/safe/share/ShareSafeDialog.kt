package io.gnosis.safe.ui.safe.share

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.DialogShareSafeBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingDialogFragment
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class ShareSafeDialog : BaseViewBindingDialogFragment<DialogShareSafeBinding>() {

    @Inject
    lateinit var viewModel: ShareSafeViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): DialogShareSafeBinding =
        DialogShareSafeBinding.inflate(layoutInflater, container, false)

    override fun screenId(): ScreenId = ScreenId.SAFE_RECEIVE

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_TITLE, R.style.BaseDialog)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (val viewAction = it.viewAction) {
                is BaseStateViewModel.ViewAction.Loading -> toggleLoading(viewAction.isLoading)
                is ShowSafeDetails -> loadSafeDetails(viewAction.safeDetails)
                is BaseStateViewModel.ViewAction.ShowError -> showError(viewAction.error)
            }
        })
        dismissBehaviour()
    }

    override fun onResume() {
        super.onResume()
        resetUI()
        viewModel.load()
    }

    private fun dismissBehaviour() {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(0))
        binding.root.setOnClickListener {
            dismiss()
        }
        binding.cardContainer.setOnClickListener { }
    }

    private fun loadSafeDetails(safeDetails: SafeDetails) {
        toggleLoading(false)
        with(binding) {
            blockies.setAddress(safeDetails.safe.address)
            safeLocalName.text = safeDetails.safe.localName
            safeDetails.safe.address.let { address ->
                safeAddress.text = safeDetails.safe.address.formatEthAddress(requireContext(), addMiddleLinebreak = false)
                safeAddress.setOnClickListener {
                    requireContext().copyToClipboard(getString(R.string.address_copied), address.asEthereumAddressChecksumString()) {
                        snackbar(requireView(), getString(R.string.copied_success))
                    }
                }
                link.setOnClickListener {
                    requireContext().openUrl(
                        getString(
                            R.string.etherscan_address_url,
                            address.asEthereumAddressChecksumString()
                        )
                    )
                }
            }
            safeQrCode.setImageBitmap(safeDetails.qrCode)
            if (safeDetails.ensName.isNullOrBlank()) {
                safeEnsName.visible(false)
            } else {
                safeEnsName.text = safeDetails.ensName
                safeEnsName.visible(true)
            }
        }
    }

    private fun toggleLoading(loading: Boolean) {
        with(binding) {
            progress.visible(loading)
            safeFields.visible(!loading)
        }
    }

    private fun showError(error: Throwable) {
        with(binding) {
            progress.visible(false)
            safeFields.visible(false)
        }
        Timber.e(error)
        snackbar(requireView(), Snackbar.LENGTH_LONG, R.string.error_invalid_safe)
    }

    private fun resetUI() {
        with(binding) {
            blockies.setAddress(null)
            safeLocalName.text = ""
            progress.visible(true)
            safeFields.visible(false)
            safeEnsName.visible(false)
        }
    }
}
