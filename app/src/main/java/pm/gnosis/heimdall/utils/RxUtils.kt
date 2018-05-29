package pm.gnosis.heimdall.utils

import io.reactivex.Observable
import io.reactivex.Single

fun <I, O> Observable<I>.emitAndNext(emit: (I) -> O, next: (I) -> Observable<O>) =
    flatMap {
        Observable.just(emit(it)).concatWith(next(it))
    }

fun <I, O> Single<I>.emitAndNext(emit: (I) -> O, next: (I) -> Observable<O>) =
    flatMapObservable {
        Observable.just(emit(it)).concatWith(next(it))
    }
