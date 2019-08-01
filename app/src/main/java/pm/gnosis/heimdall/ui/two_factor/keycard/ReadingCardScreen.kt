package pm.gnosis.heimdall.ui.two_factor.keycard

import android.content.Context
import android.view.View
import kotlinx.android.synthetic.main.screen_keycard_reading.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.svalinn.common.utils.getColorCompat

interface ReadingCardScreen {
    val appContext: Context
    val screen: View

    fun setupView(cancelListener: (View) -> Unit) {
        screen.keycard_reading_cancel_button.setOnClickListener(cancelListener)
    }

    fun updateView(reading: Boolean, error: String?) {
        when {
            error != null -> {
                screen.keycard_reading_txt.text = error
                screen.keycard_reading_txt.setTextColor(appContext.getColorCompat(R.color.tomato))
            }
            reading -> {
                screen.keycard_reading_txt.text = appContext.getString(R.string.reading_keycard)
                screen.keycard_reading_txt.setTextColor(appContext.getColorCompat(R.color.light_text))
            }
            else -> {
                screen.keycard_reading_txt.text = appContext.getString(R.string.waiting_for_keycard)
                screen.keycard_reading_txt.setTextColor(appContext.getColorCompat(R.color.light_text))
            }
        }
    }
}
