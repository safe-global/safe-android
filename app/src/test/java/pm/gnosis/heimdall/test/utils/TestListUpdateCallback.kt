package pm.gnosis.heimdall.test.utils

import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class TestListUpdateCallback : ListUpdateCallback {

    private val changes = HashSet<Int>()
    private val removes = HashSet<Int>()
    private val inserts = HashSet<Int>()
    private val moves = HashSet<Move>()

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        for (i in position..position + count) {
            changes.add(position)
        }
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        moves.add(Move(fromPosition, toPosition))
    }

    override fun onInserted(position: Int, count: Int) {
        for (i in position..position + count) {
            inserts.add(position)
        }
    }

    override fun onRemoved(position: Int, count: Int) {
        for (i in position..position + count) {
            removes.add(position)
        }
    }

    fun apply(result: DiffUtil.DiffResult): TestListUpdateCallback {
        result.dispatchUpdatesTo(this)
        return this
    }

    fun assertMovesCount(expected: Int): TestListUpdateCallback {
        assertEquals(expected, moves.size)
        return this
    }

    fun assertNoMoves(): TestListUpdateCallback {
        return assertMovesCount(0)
    }

    fun assertMove(expected: Move): TestListUpdateCallback {
        assertTrue("Expected move at $expected not found", moves.contains(expected))
        return this
    }

    fun assertChangesCount(expected: Int): TestListUpdateCallback {
        assertEquals(expected, changes.size)
        return this
    }

    fun assertNoChanges(): TestListUpdateCallback {
        return assertChangesCount(0)
    }

    fun assertChange(expected: Int): TestListUpdateCallback {
        assertTrue("Expected change at $expected not found", changes.contains(expected))
        return this
    }

    fun assertRemovesCount(expected: Int): TestListUpdateCallback {
        assertEquals(expected, removes.size)
        return this
    }

    fun assertNoRemoves(): TestListUpdateCallback {
        return assertRemovesCount(0)
    }

    fun assertRemove(expected: Int): TestListUpdateCallback {
        assertTrue("Expected removal at $expected not found", removes.contains(expected))
        return this
    }

    fun assertInsertsCount(expected: Int): TestListUpdateCallback {
        assertEquals(expected, inserts.size)
        return this
    }

    fun assertNoInserts(): TestListUpdateCallback {
        return assertInsertsCount(0)
    }

    fun assertInsert(expected: Int): TestListUpdateCallback {
        assertTrue("Expected insertion at $expected not found", inserts.contains(expected))
        return this
    }

    fun reset() {
        changes.clear()
        removes.clear()
        inserts.clear()
        moves.clear()
    }

    data class Move(val from: Int, val to: Int)
}