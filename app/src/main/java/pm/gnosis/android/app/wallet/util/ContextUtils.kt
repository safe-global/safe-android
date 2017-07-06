package pm.gnosis.android.app.wallet.util

import android.content.Context
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.Toast

fun Context.toast(text: CharSequence, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, duration).show()
}

fun snackbar(view: View, text: CharSequence, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(view, text, duration).show()
}
