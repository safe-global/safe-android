package pm.gnosis.heimdall.ui.dialogs.ens

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.dialog_change_password.view.*
import kotlinx.android.synthetic.main.dialog_ens_input.view.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.heimdall.utils.CustomAlertDialogBuilder
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.svalinn.security.EncryptionManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EnsInputDialog : BaseDialog() {
    @Inject
    lateinit var encryptionManager: EncryptionManager

    @Inject
    lateinit var eventTracker: EventTracker

    @Inject
    lateinit var viewModel: EnsInputContract

    @Inject
    lateinit var addressHelper: AddressHelper

    private lateinit var dialogView: View
    private lateinit var alertDialog: AlertDialog

    private val confirmSubject = PublishSubject.create<Unit>()

    var callback: ((Solidity.Address) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ens_input, null)
        alertDialog = CustomAlertDialogBuilder.build(context!!, "Input ENS Address", dialogView, R.string.ok, {
            confirmSubject.onNext(Unit)
        })
        return alertDialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.INPUT_SAFE_ADDRESS)) // TODO change

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        disposables +=
            Observable.combineLatest(
                processInput(),
                confirmSubject,
                BiFunction { address: Result<Solidity.Address>, _: Unit -> address }
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(::propagateResult) {
                    Timber.e(it)
                    dismiss()
                }

    }

    private fun processInput() =
        dialogView.dialog_ens_input_url.textChanges()
            .debounce(1000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 0.5f
                dialogView.dialog_ens_input_progress.visible(true)
            }
            .compose(viewModel.processEnsInput())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                dialogView.dialog_ens_input_progress.visible(false)
                (it as? DataResult)?.let {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 1f
                    dialogView.dialog_ens_input_name.visible(false)
                    addressHelper.populateAddressInfo(
                        dialogView.dialog_ens_input_address,
                        dialogView.dialog_ens_input_name,
                        dialogView.dialog_ens_input_address_image,
                        it.data
                    ).forEach { disposables += it }
                } ?: run {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = 0.5f
                    dialogView.dialog_ens_input_name.visible(true)
                    dialogView.dialog_ens_input_address.text = null
                    dialogView.dialog_ens_input_address_image.setAddress(null)
                    dialogView.dialog_ens_input_name.text = "Could not load address"
                }
            }

    private fun propagateResult(state: Solidity.Address) {
        callback?.invoke(state)
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface?) {
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
