package pm.gnosis.heimdall.common.util

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import timber.log.Timber

class WhatTheFuck(cause: Throwable) : IllegalStateException(cause)

fun <D> Observable<Result<D>>.subscribeForResult(onNext: ((D) -> Unit)?, onError: ((Throwable) -> Unit)?): Disposable =
        subscribe({ it.handle(onNext, onError) }, {
            Timber.e(WhatTheFuck(it))
            onError?.invoke(it)
        })

data class Result<out D>(val data: D? = null, val error: Throwable? = null) {
    fun handle(dataFun: ((D) -> Unit)?, errorFun: ((Throwable) -> Unit)?) {
        if (error == null) {
            data?.let { dataFun?.invoke(it) }
        } else {
            errorFun?.invoke(error)
        }
    }

    constructor(data: D) : this(data, null)
    constructor(error: Throwable) : this(null, error)
}
