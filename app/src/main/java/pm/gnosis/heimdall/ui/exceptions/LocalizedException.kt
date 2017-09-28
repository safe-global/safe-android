package pm.gnosis.heimdall.ui.exceptions

import android.content.Context
import android.support.annotation.StringRes


data class LocalizedException(override val message: String) : Exception(message) {
    companion object {
        fun assert(condition: Boolean, context: Context, @StringRes messagedId: Int, vararg params: Any) {
            if (!condition) {
                throw LocalizedException(context.getString(messagedId, params))
            }
        }

        fun assert(condition: () -> Boolean, context: Context, @StringRes messagedId: Int, vararg params: Any) {
            assert(condition.invoke(), context, messagedId, params)
        }
    }
}