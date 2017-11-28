package pm.gnosis.tests.utils


import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue


class TestObservableFactory<T> {
    private val emitterList = ArrayList<ObservableEmitter<T>>()

    fun get(): Observable<T> {
        return Observable.create({ emitterList.add(it) })
    }

    fun assertCount(count: Int): TestObservableFactory<T> {
        assertEquals(String.format("Should have %s subscription!", count), count, emitterList.size)
        return this
    }

    fun assertAllSubscribed(): TestObservableFactory<T> {
        for (emitter in emitterList) {
            assertTrue("Should be subscribed!", !emitter.isDisposed)
        }
        return this
    }

    fun assertAllCanceled(): TestObservableFactory<T> {
        for (emitter in emitterList) {
            assertTrue("Should be disposed!", emitter.isDisposed)
        }
        return this
    }

    fun success(t: T) {
        for (emitter in emitterList) {
            emitter.onNext(t)
        }
        emitterList.clear()
    }

    fun error(t: Throwable) {
        for (emitter in emitterList) {
            emitter.onError(t)
        }
        emitterList.clear()
    }

    fun complete() {
        for (emitter in emitterList) {
            emitter.onComplete()
        }
        emitterList.clear()
    }
}