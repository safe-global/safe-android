package io.gnosis.safe.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.R
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.helpers.AddressHelper
import io.gnosis.safe.utils.CustomAlertDialogBuilder
import io.gnosis.safe.utils.debounce
import kotlinx.android.synthetic.main.dialog_ens_input.view.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class EnsInputDialog : DialogFragment() {

    @Inject
    lateinit var viewModel: EnsInputViewModel

    @Inject
    lateinit var addressHelper: AddressHelper

    private lateinit var dialogView: View
    private lateinit var alertDialog: AlertDialog

    var callback: ((Solidity.Address) -> Unit)? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        inject()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ens_input, null)

        alertDialog = CustomAlertDialogBuilder.build(requireContext(), getString(R.string.ens_input_title), dialogView, R.string.ok, {
            onClick.offer(Unit)
        })
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        return alertDialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onStart() {
        super.onStart()
        processInput()
        lifecycleScope.launch {
            onClick.asFlow().collect {
                onNewAddress.valueOrNull?.let { propagateResult(it) }
            }
        }
    }

    private val onNewAddress = ConflatedBroadcastChannel<Solidity.Address?>()
    private val onClick = ConflatedBroadcastChannel<Unit>()

    private fun onUrlAvailable(string: String) {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 0.5f
        dialogView.dialog_ens_input_progress.visible(true)
        lifecycleScope.launch {
            runCatching { viewModel.processEnsInput(string) }
                .onSuccess { address ->
                    dialogView.dialog_ens_input_progress.visible(false)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 1f
                    dialogView.dialog_ens_input_name.visible(false)
                    dialogView.dialog_ens_input_address.visible(true)
                    addressHelper.populateAddressInfo(
                        dialogView.dialog_ens_input_address,
//                        dialogView.dialog_ens_input_name,
                        dialogView.dialog_ens_input_address_image,
                        address
                    )
                    onNewAddress.offer(address)
                }
                .onFailure {
                    dialogView.dialog_ens_input_progress.visible(false)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 0.5f
                    dialogView.dialog_ens_input_name.visible(true)
                    dialogView.dialog_ens_input_address.visible(false)
                    dialogView.dialog_ens_input_address.text = null
                    dialogView.dialog_ens_input_address_image.setAddress(null)
                    dialogView.dialog_ens_input_name.text = getString(R.string.error_resolve_ens)
                    onNewAddress.offer(null)
                }
        }
    }

    val onUrlChanged: (String) -> Unit = debounce(1000, lifecycleScope, this::onUrlAvailable)

    private fun processInput() {
        dialogView.dialog_ens_input_url.doOnTextChanged { text, _, _, _ ->
            onUrlChanged(text.toString())
        }
    }

    private fun propagateResult(state: Solidity.Address) {
        callback?.invoke(state)
        onNewAddress.close()
        onClick.close()
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        callback = null
        super.onDismiss(dialog)
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(requireContext()))
            .applicationComponent(HeimdallApplication[requireContext()])
            .build()
            .inject(this)
    }

    companion object {
        fun create() = EnsInputDialog()
    }
}
