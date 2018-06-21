package pm.gnosis.heimdall.ui.settings.general.changepassword

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.dialog_change_password.view.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.helpers.PasswordHelper
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.heimdall.ui.settings.general.changepassword.ChangePasswordContract.State.*
import pm.gnosis.heimdall.utils.CustomAlertDialogBuilder
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.svalinn.security.EncryptionManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChangePasswordDialog : BaseDialog() {
    @Inject
    lateinit var encryptionManager: EncryptionManager

    @Inject
    lateinit var viewModel: ChangePasswordContract

    private lateinit var dialogView: View
    private lateinit var alertDialog: AlertDialog

    private val confirmSubject = PublishSubject.create<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_password, null)
        alertDialog = CustomAlertDialogBuilder.build(context!!, getString(R.string.change_password), dialogView, R.string.ok, { _ ->
            confirmSubject.onNext(Unit)
        })
        return alertDialog
    }

    override fun onStart() {
        super.onStart()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
        disposables += confirmSubject
            .doOnNext {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            }
            .switchMapSingle {
                viewModel.setPassword(
                    dialogView.dialog_change_password_current.text.toString(),
                    dialogView.dialog_change_password_new.text.toString(),
                    dialogView.dialog_change_password_new_repeat.text.toString()
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(::applyState) {
                Timber.e(it)
                snackbar(dialogView.dialog_change_password_current, R.string.unknown_error)
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
            }

        disposables += dialogView.dialog_change_password_new.textChanges()
            .debounce(500, TimeUnit.MILLISECONDS)
            .switchMapSingle {
                viewModel.validatePassword(it.toString())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                PasswordHelper.Handler.applyToView(
                    dialogView.dialog_change_password_new,
                    dialogView.dialog_change_password_validation_info,
                    it
                )
            }, onError = Timber::e)

        disposables += dialogView.dialog_change_password_new_repeat.textChanges()
            .debounce(500, TimeUnit.MILLISECONDS)
            .switchMapSingle {
                viewModel.validateRepeat(dialogView.dialog_change_password_new.text.toString(), it.toString())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                // Cannot set visibility because of ConstraintGroup
                dialogView.dialog_change_password_repeat_info.text = if (it) null else getString(R.string.password_doesnt_match)
            }, onError = Timber::e)

        disposables += dialogView.dialog_change_password_current.textChanges()
            .subscribeBy { dialogView.dialog_change_password_current_info.text = null }

    }

    private fun applyState(state: ChangePasswordContract.State) {
        when (state) {
            INVALID_PASSWORD -> {
                dialogView.dialog_change_password_current_info.text = getString(R.string.error_wrong_credentials)
                dialogView.dialog_change_password_current_input_group.visible(true)
                dialogView.dialog_change_password_new_input_group.visible(false)
            }
            ENTER_NEW_PASSWORD -> {
                dialogView.dialog_change_password_current_input_group.visible(false)
                dialogView.dialog_change_password_new_input_group.visible(true)
            }
            PASSWORD_CHANGED -> dismiss()
        }
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
    }

    override fun onPause() {
        dialogView.dialog_change_password_current.text = null
        dialogView.dialog_change_password_new.text = null
        dialogView.dialog_change_password_new_repeat.text = null
        super.onPause()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!].component)
            .build()
            .inject(this)
    }

    companion object {
        fun create() = ChangePasswordDialog()
    }
}
