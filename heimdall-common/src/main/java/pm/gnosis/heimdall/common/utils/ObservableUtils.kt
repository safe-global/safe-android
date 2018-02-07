package pm.gnosis.heimdall.common.utils

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import timber.log.Timber

class WhatTheFuck(cause: Throwable) : IllegalStateException(cause)

fun <D> Observable<D>.onErrorDefaultBeforeThrow(default: D?): Observable<D> =
        onErrorResumeNext { throwable: Throwable ->
            default ?: return@onErrorResumeNext Observable.error(throwable)
            Observable.just(default).thenThrow(throwable)
        }

fun <D> Observable<D>.thenThrow(throwable: Throwable): Observable<D> = this.concatWith(Observable.error(throwable))

fun <D> Observable<out Result<D>>.subscribeForResult(onNext: ((D) -> Unit)?, onError: ((Throwable) -> Unit)?): Disposable =
        subscribe({ it.handle(onNext, onError) }, {
            Timber.e(WhatTheFuck(it))
            onError?.invoke(it)
        })

fun <D> Observable<Result<D>>.doOnNextForResult(onNext: ((D) -> Unit)? = null, onError: ((Throwable) -> Unit)? = null): Observable<Result<D>> =
        doOnNext { it.handle(onNext, onError) }

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

fun Completable.mapToResult(): Single<Result<Unit>> =
        this.andThen(Single.just<Result<Unit>>(DataResult(Unit))).onErrorReturn { ErrorResult(it) }

fun <K, D> MutableMap<K, Observable<D>>.getSharedObservable(key: K, source: Observable<D>): Observable<D> =
        getOrPut(key, {
            source.doOnTerminate { remove(key) }
                    .publish()
                    .autoConnect()
        })

fun <D, O> Observable<Result<D>>.flatMapResult(
        mapper: (D) -> Single<Result<O>>,
        errorMapper: ((Throwable) -> Single<Result<O>>)? = null
): Observable<Result<O>> =
        flatMapSingle {
            it.mapSingle(mapper, errorMapper)
        }

sealed class Result<out D> {
    fun handle(dataFun: ((D) -> Unit)?, errorFun: ((Throwable) -> Unit)?) {
        when (this) {
            is DataResult -> dataFun?.invoke(data)
            is ErrorResult -> errorFun?.invoke(error)
        }
    }

    fun <O> map(mapper: (D) -> O): Result<O> = when (this) {
        is DataResult -> try {
            DataResult(mapper(data))
        } catch (throwable: Throwable) {
            ErrorResult<O>(throwable)
        }
        is ErrorResult -> ErrorResult(error)
    }

    fun <O> mapSingle(mapper: (D) -> Single<Result<O>>, errorMapper: ((Throwable) -> Single<Result<O>>)? = null):
            Single<Result<O>> = when (this) {
        is DataResult -> mapper(data)
        is ErrorResult -> errorMapper?.invoke(error) ?: Single.just(ErrorResult(error))
    }
}

data class DataResult<out D>(val data: D) : Result<D>()

data class ErrorResult<out D>(val error: Throwable) : Result<D>()
