package io.gnosis.data.repositories

import io.gnosis.data.db.daos.SafeDao
import io.mockk.mockk
import org.junit.Test

class SafeRepositoryTest {

    private val safeDao = mockk<SafeDao>()
    private val safeRepository = SafeRepository(safeDao)

    @Test
    fun `submitTx - (actualThreshold lt expectedThreshold) should fail`() {

    }
}
