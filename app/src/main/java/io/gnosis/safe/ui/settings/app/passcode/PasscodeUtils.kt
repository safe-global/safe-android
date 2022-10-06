package io.gnosis.safe.ui.settings.app.passcode

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import io.gnosis.safe.R
import pm.gnosis.svalinn.common.utils.showKeyboardForView


fun View.delayShowKeyboardForView(delay: Long = 600) {
    postDelayed({
        showKeyboardForView()
    }, delay)
}

fun View.hideSoftKeyboard() {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(windowToken, 0)
}

fun onSixDigitsHandler(
    digits: List<ImageView>,
    context: Context,
    executeWithDigits: (digitsAsString: String) -> Unit
): (CharSequence?, Int, Int, Int) -> Unit {
    return { text, _, _, _ ->
        text?.let {
            if (text.length < 6) {
                digits.forEach {
                    it.background = ContextCompat.getDrawable(context, R.color.background_secondary)
                }
                (1..text.length).forEach { i ->
                    digits[i - 1].background = ContextCompat.getDrawable(context, R.drawable.ic_circle_passcode_filled_20dp)
                }
            } else {
                digits[digits.size - 1].background = ContextCompat.getDrawable(context, R.drawable.ic_circle_passcode_filled_20dp)
                executeWithDigits(text.toString())
            }
        }
    }
}
