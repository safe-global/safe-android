package pm.gnosis.heimdall.common.util

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import timber.log.Timber

class WhatTheFuck(cause: Throwable) : IllegalStateException(cause)

fun <D> Observable<D>.onErrorDefaultBeforeThrow(default: D): Observable<D> =
        onErrorResumeNext { throwable: Throwable ->
            Observable.just(default).thenThrow(throwable)
        }

fun <D> Observable<D>.thenThrow(throwable: Throwable): Observable<D> = this.concatWith(Observable.error(throwable))

fun <D> Observable<out Result<D>>.subscribeForResult(onNext: ((D) -> Unit)?, onError: ((Throwable) -> Unit)?): Disposable =
        subscribe({ it.handle(onNext, onError) }, {
            Timber.e(WhatTheFuck(it))
            onError?.invoke(it)
        })

fun <D> Flowable<out Result<D>>.subscribeForResult(onNext: ((D) -> Unit)?, onError: ((Throwable) -> Unit)?): Disposable =
        subscribe({ it.handle(onNext, onError) }, {
            Timber.e(WhatTheFuck(it))
            onError?.invoke(it)
        })

fun <D> Observable<D>.mapToResult(): Observable<Result<D>> =
        this.map<Result<D>> { DataResult(it) }.onErrorReturn { ErrorResult(it) }

fun <D> Flowable<D>.mapToResult(): Flowable<Result<D>> =
        this.map<Result<D>> { DataResult(it) }.onErrorReturn { ErrorResult(it) }

fun <D> Single<D>.mapToResult(): Single<Result<D>> =
        this.map<Result<D>> { DataResult(it) }.onErrorReturn { ErrorResult(it) }

sealed class Result<out D> {
    fun handle(dataFun: ((D) -> Unit)?, errorFun: ((Throwable) -> Unit)?) {
        when (this) {
            is DataResult -> dataFun?.invoke(data)
            is ErrorResult -> errorFun?.invoke(error)
        }
    }
}

data class DataResult<out D>(val data: D) : Result<D>()

data class ErrorResult<out D>(val error: Throwable) : Result<D>()
