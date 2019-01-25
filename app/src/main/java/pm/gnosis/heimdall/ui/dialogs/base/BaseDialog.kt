package pm.gnosis.heimdall.ui.dialogs.base

import androidx.fragment.app.DialogFragment
import io.reactivex.disposables.CompositeDisposable

abstract class BaseDialog : DialogFragment() {
    protected val disposables = CompositeDisposable()

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }
}
