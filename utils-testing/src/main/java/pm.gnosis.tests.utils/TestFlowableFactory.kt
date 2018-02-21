package pm.gnosis.tests.utils


import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue


class TestFlowableFactory<T> {
    private val emitterList = ArrayList<FlowableEmitter<T>>()

    fun get(): Flowable<T> {
        return Flowable.create({ emitterList.add(it) }, BackpressureStrategy.BUFFER)
    }

    fun assertCount(count: Int): TestFlowableFactory<T> {
        assertEquals(String.format("Should have %s subscription!", count), count, emitterList.size)
        return this
    }

    fun assertAllSubscribed(): TestFlowableFactory<T> {
        for (emitter in emitterList) {
            assertTrue("Should be subscribed!", !emitter.isCancelled)
        }
        return this
    }

    fun assertAllCanceled(): TestFlowableFactory<T> {
        for (emitter in emitterList) {
            assertTrue("Should be disposed!", emitter.isCancelled)
        }
        return this
    }

    fun offer(t: T) {
        for (emitter in emitterList) {
            emitter.onNext(t)
        }
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
