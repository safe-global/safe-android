package pm.gnosis.heimdall.ui.dialogs.base

import android.content.DialogInterface
import android.support.v4.app.DialogFragment
import io.reactivex.disposables.CompositeDisposable

abstract class BaseDialog : DialogFragment() {
    protected val disposables = CompositeDisposable()

    var dismissListener: (() -> Unit)? = null

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        dismissListener?.invoke()
    }
}
