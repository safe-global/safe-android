package pm.gnosis.tests.utils


import io.reactivex.Single
import io.reactivex.SingleEmitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue


class TestSingleFactory<T> {
    private val emitterList = ArrayList<SingleEmitter<T>>()

    fun get(): Single<T> {
        return Single.create({ emitterList.add(it) })
    }

    fun assertCount(count: Int): TestSingleFactory<T> {
        assertEquals(String.format("Should have %s subscription!", count), count, emitterList.size)
        return this
    }

    fun assertAllSubscribed(): TestSingleFactory<T> {
        for (emitter in emitterList) {
            assertTrue("Should be subscribed!", !emitter.isDisposed)
        }
        return this
    }

    fun assertAllCanceled(): TestSingleFactory<T> {
        for (emitter in emitterList) {
            assertTrue("Should be disposed!", emitter.isDisposed)
        }
        return this
    }

    fun success(t: T) {
        for (emitter in emitterList) {
            emitter.onSuccess(t)
        }
        emitterList.clear()
    }

    fun error(t: Throwable) {
        for (emitter in emitterList) {
            emitter.onError(t)
        }
        emitterList.clear()
    }
}
