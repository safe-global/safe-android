package pm.gnosis.heimdall.ui.exceptions

import android.content.Context
import android.support.annotation.StringRes
import io.reactivex.Observable


data class LocalizedException(override val message: String) : Exception(message) {

    class Handler private constructor(private val context: Context, private val translators: List<Translator>) {

        fun translate(exception: Throwable): Throwable {
            translators.forEach {
                if (it.checker(exception)) {
                    return LocalizedException(it.writer(context))
                }
            }
            return exception
        }

        fun <D> observable(exception: Throwable): Observable<D> {
            return Observable.error<D>(translate(exception))
        }

        private class Translator(val checker: (Throwable) -> Boolean, val writer: (Context) -> String) {
            constructor(checker: (Throwable) -> Boolean, @StringRes messageId: Int) :
                    this(checker, { it.getString(messageId) })
        }

        class Builder(val context: Context) {
            private val translators = ArrayList<Translator>()
            fun add(checker: (Throwable) -> Boolean, @StringRes messageId: Int): Builder {
                translators += Translator(checker, messageId)
                return this
            }

            fun add(checker: (Throwable) -> Boolean, writer: (Context) -> String): Builder {
                translators += Translator(checker, writer)
                return this
            }

            fun build(): Handler {
                return Handler(context, translators)
            }
        }
    }

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