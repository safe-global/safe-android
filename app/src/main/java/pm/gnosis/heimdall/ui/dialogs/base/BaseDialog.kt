package pm.gnosis.heimdall.ui.dialogs.base

import android.support.v4.app.DialogFragment
import io.reactivex.disposables.CompositeDisposable

abstract class BaseDialog : DialogFragment() {
    protected val disposables = CompositeDisposable()

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }
}
