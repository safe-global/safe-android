package pm.gnosis.heimdall.ui.base

import android.view.MotionEvent
import android.widget.Toast
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.toast

abstract class SecuredBaseActivity : BaseActivity() {

    // Once set to true the activity needs to be restarted
    private var obscuredWindow: Boolean = false

    override fun onStart() {
        super.onStart()
        if (obscuredWindow) {
            onWindowObscured()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val newState = obscuredWindow || event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0
        // Only notify on change
        if (newState && !obscuredWindow) {
            obscuredWindow = newState
            onWindowObscured()
        }
        return super.dispatchTouchEvent(event)
    }

    protected open fun onWindowObscured() {
        showWarning()
    }

    private fun showWarning() {
        toast(R.string.obscured_window_warning, Toast.LENGTH_LONG)
    }

}
