package io.gnosis.safe.ui.safe.share

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.DialogShareSafeBinding
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.BaseViewBindingDialogFragment
import io.gnosis.safe.utils.formatEthAddress
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class ShareSafeDialog : BaseViewBindingDialogFragment<DialogShareSafeBinding>() {

    @Inject
    lateinit var viewModel: ShareSafeViewModel

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): DialogShareSafeBinding =
        DialogShareSafeBinding.inflate(layoutInflater)

    override fun screenId(): ScreenId? = null

    override fun inject(component: ViewComponent) {
        component.inject(this)
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
        viewModel.load()
    }

    private fun loadSafeDetails(safeDetails: SafeDetails) {
        toggleLoading(false)
        with(binding) {
            blockies.setAddress(safeDetails.safe.address)
            safeLocalName.text = safeDetails.safe.localName
            safeAddress.text = safeDetails.safe.address.formatEthAddress(requireContext())
            safeEnsName.text = safeDetails.ensName ?: getString(R.string.safe_settings_not_set)
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
}
