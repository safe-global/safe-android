package io.gnosis.safe.ui.dialogs


import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.R
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.helpers.AddressHelper
import io.gnosis.safe.ui.base.BaseDialog
import io.gnosis.safe.utils.CustomAlertDialogBuilder
import io.gnosis.safe.utils.debounce
import kotlinx.android.synthetic.main.dialog_ens_input.view.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EnsInputDialog : BaseDialog() {

    @Inject
    lateinit var viewModel: EnsInputViewModel

    @Inject
    lateinit var addressHelper: AddressHelper

    private lateinit var dialogView: View
    private lateinit var alertDialog: AlertDialog


    var callback: ((Solidity.Address) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ens_input, null)
        alertDialog = CustomAlertDialogBuilder.build(context!!, getString(R.string.ens_input_title), dialogView, R.string.ok, {
//            propagateResult(addressHelper)
            dismiss()
        })
        return alertDialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onStart() {
        super.onStart()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        processInput()
//        lifecycleScope.launch { combine(,) }
//        Observable.combineLatest(
//            processInput(),
//            confirmSubject,
//            BiFunction { address: Result<Solidity.Address>, _: Unit -> address }
//        ).subscribeForResult(::propagateResult) {
//            Timber.e(it)
//            dismiss()
//        }
    }

    private fun onUrlAvailable(string: String) {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 0.5f
        dialogView.dialog_ens_input_progress.visible(true)
        lifecycleScope.launch {
            runCatching { viewModel.processEnsInput(string) }
                .onSuccess { address ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 1f
                    dialogView.dialog_ens_input_name.visible(false)
                    dialogView.dialog_ens_input_address.visible(true)
                    addressHelper.populateAddressInfo(
                        dialogView.dialog_ens_input_address,
//                        dialogView.dialog_ens_input_name,
                        dialogView.dialog_ens_input_address_image,
                        address!!
                    )
                }
                .onFailure {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 0.5f
                    dialogView.dialog_ens_input_name.visible(true)
                    dialogView.dialog_ens_input_address.visible(false)
                    dialogView.dialog_ens_input_address.text = null
                    dialogView.dialog_ens_input_address_image.setAddress(null)
                    dialogView.dialog_ens_input_name.text = getString(R.string.error_resolve_ens)
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
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        callback = null
        super.onDismiss(dialog)
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!])
            .build()
            .inject(this)
    }

    companion object {
        fun create() = EnsInputDialog()
    }
}
