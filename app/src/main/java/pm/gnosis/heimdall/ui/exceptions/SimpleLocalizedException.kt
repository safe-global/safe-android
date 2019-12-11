package pm.gnosis.heimdall.ui.exceptions

import android.content.Context
import androidx.annotation.StringRes
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.utils.HttpCodes
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

data class SimpleLocalizedException(override val message: String) : Exception(message), LocalizedException {

    override fun localizedMessage() = message

    class Handler private constructor(
        private val context: Context, private val translators: List<Translator>,
        private val logException: Boolean = true
    ) {

        fun translate(exception: Throwable): Throwable {
            if (logException) {
                Timber.e(exception)
            }
            translators.forEach {
                if (it.condition(exception)) {
                    return SimpleLocalizedException(it.writer(context, exception))
                }
            }
            return exception
        }

        fun toBuilder(): Builder {
            return Builder(this)
        }

        fun <D> observable(exception: Throwable): Observable<D> {
            return Observable.error<D>(translate(exception))
        }

        fun <D> single(exception: Throwable): Single<D> {
            return Single.error<D>(translate(exception))
        }

        fun completable(exception: Throwable): Completable {
            return Completable.error(translate(exception))
        }

        private class Translator(val condition: (Throwable) -> Boolean, val writer: (Context, Throwable) -> String) {
            constructor(checker: (Throwable) -> Boolean, @StringRes messageId: Int) :
                    this(checker, { context, _ -> context.getString(messageId) })
        }

        class Builder(val context: Context) {

            constructor(handler: Handler) : this(handler.context) {
                translators.addAll(handler.translators)
            }

            private val translators = ArrayList<Translator>()
            fun add(checker: (Throwable) -> Boolean, @StringRes messageId: Int): Builder {
                translators += Translator(checker, messageId)
                return this
            }

            fun add(checker: (Throwable) -> Boolean, writer: (Context, Throwable) -> String): Builder {
                translators += Translator(checker, writer)
                return this
            }

            fun build(logException: Boolean = true): Handler {
                return Handler(context, translators, logException)
            }
        }
    }

    companion object {

        fun networkErrorHandlerBuilder(context: Context) = Handler.Builder(context)
            .add({ it is HttpException }, { c, throwable ->
                (throwable as HttpException).let {
                    @Suppress("ConstantConditionIf")
                    if (BuildConfig.VERBOSE_EXCEPTIONS) {
                        return@add "${throwable.code()} (${throwable.message()}): ${throwable.response()?.errorBody()?.string()}"
                    }
                    when (throwable.code()) {
                        HttpCodes.FORBIDDEN, HttpCodes.UNAUTHORIZED -> c.getString(R.string.error_not_authorized_for_action)
                        HttpCodes.SERVER_ERROR, HttpCodes.BAD_REQUEST -> c.getString(R.string.error_try_again)
                        else -> context.getString(R.string.error_try_again)
                    }
                }
            })
            .add({ it is SSLHandshakeException || it.cause is SSLHandshakeException }, { c, _ -> c.getString(R.string.error_ssl_handshake) })
            .add(
                { it is UnknownHostException || it is SocketTimeoutException || it is ConnectException },
                { c, _ -> c.getString(R.string.error_check_internet_connection) })
            .add({ BuildConfig.VERBOSE_EXCEPTIONS }, { _, throwable ->
                "${throwable.javaClass.simpleName}: ${throwable.message}"
            })

        fun assert(condition: Boolean, context: Context, @StringRes messagedId: Int, vararg params: Any) {
            if (!condition) {
                throw SimpleLocalizedException(context.getString(messagedId, params))
            }
        }

        fun assert(condition: () -> Boolean, context: Context, @StringRes messagedId: Int, vararg params: Any) {
            assert(condition.invoke(), context, messagedId, params)
        }
    }
}
