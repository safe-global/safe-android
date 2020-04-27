package io.gnosis.safe

import androidx.lifecycle.Observer
import org.junit.Assert
import java.util.concurrent.CopyOnWriteArrayList

open class TestLiveDataObserver<E> : Observer<E> {

    private val values = CopyOnWriteArrayList<E>()

    fun values(): List<E> = values

    fun assertValueCount(expected: Int): TestLiveDataObserver<E> {
        Assert.assertEquals("Value count differs;", expected, values.size)
        return this
    }

    fun assertValues(vararg expectedValues: E): TestLiveDataObserver<E> {
        assertValueCount(expectedValues.size)
        values.forEachIndexed { index, e ->
            Assert.assertEquals("Value at $index differs;", expectedValues[index], e)
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
