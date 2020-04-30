package io.gnosis.safe.ui.safe

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.di.Repositories
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import pm.gnosis.model.Solidity
import java.math.BigInteger


class SafeOverviewViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val repositories = mockk<Repositories>().apply {
        every { safeRepository() } returns safeRepository
    }

    private lateinit var safeOverviewViewModel: SafeOverviewViewModel

    @Before
    fun setup() {
        safeOverviewViewModel = SafeOverviewViewModel(repositories, appDispatchers)
    }


    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        private val SAFES = listOf(SAFE_1, SAFE_2)
        private val ACTIVE_SAFE = SAFE_1
    }
}
