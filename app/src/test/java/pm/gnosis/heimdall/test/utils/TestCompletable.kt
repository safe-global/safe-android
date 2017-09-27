package pm.gnosis.heimdall.test.utils

import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.disposables.Disposables

class TestCompletable : Completable() {

    var callCount = 0
        private set

    override fun subscribeActual(s: CompletableObserver) {
        val d = Disposables.empty()
        s.onSubscribe(d)
        callCount++
        s.onComplete()
    }
}
