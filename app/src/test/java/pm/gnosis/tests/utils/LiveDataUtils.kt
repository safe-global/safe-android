package pm.gnosis.tests.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.runner.Description
import java.util.concurrent.CopyOnWriteArrayList

class TestLifecycleRule: InstantTaskExecutorRule(), LifecycleOwner {

    private val lifecycle = LifecycleRegistry(this)

    override fun getLifecycle() = lifecycle

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun finished(description: Description?) {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        Dispatchers.resetMain()
        super.finished(description)
    }

}

open class TestLiveDataObserver<E>: Observer<E> {

    private val values = CopyOnWriteArrayList<E>()

    fun values(): List<E> = values

    fun assertValueCount(expected: Int): TestLiveDataObserver<E> {
        assertEquals("Value count differs;", expected, values.size)
        return this
    }

    fun assertValues(vararg expectedValues: E): TestLiveDataObserver<E> {
        assertValueCount(expectedValues.size)
        values.forEachIndexed { index, e ->
            assertEquals("Value at $index differs;", expectedValues[index], e)
        }
        return this
    }


    fun assertValueAt(index: Int, predicate: (E) -> Unit): TestLiveDataObserver<E> {
        predicate(values[index])
        return this
    }

    fun assertEmpty(): TestLiveDataObserver<E> {
        assertValueCount(0)
        return this
    }

    fun clear(): TestLiveDataObserver<E> {
        values.clear()
        return this
    }

    override fun onChanged(t: E) {
        values.add(t)
    }
}
