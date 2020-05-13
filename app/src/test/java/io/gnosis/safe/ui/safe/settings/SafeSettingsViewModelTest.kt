package io.gnosis.safe.ui.safe.settings

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.*
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class SafeSettingsViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()
    private val repositories = mockk<Repositories>().apply {
        every { safeRepository() } returns safeRepository
    }

    private val tracker = mockk<Tracker>()

    private lateinit var safeSettingsViewModel: SafeSettingsViewModel

    @Before
    fun setup() {
        safeSettingsViewModel = SafeSettingsViewModel(repositories, appDispatchers, tracker)
    }

    @Test
    fun `removeSafe - should remove safe`() = runBlockingTest {

        coEvery { safeRepository.getActiveSafe()} returnsMany  listOf(SAFE_1, null)
        coEvery { safeRepository.getSafes() } returnsMany listOf(SAFES, listOf(SAFE_2))
        coEvery { safeRepository.removeSafe(ACTIVE_SAFE) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs

        val safeCount = safeRepository.getSafes().count()
        assert(safeCount == 2)

        safeSettingsViewModel.removeSafe()

        safeSettingsViewModel.state.test().assertValueAt(0) {
            it is SafeSettingsState.SafeRemoved &&
                    it.viewAction is BaseStateViewModel.ViewAction.NavigateTo
        }

        safeSettingsViewModel.state.test().assertValueAt(0) {
            it is SafeSettingsState.SafeSettings &&
                    it.safe == null
        }

        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { safeRepository.removeSafe(SAFE_1) }

        // verify SAFE_REMOVE event was tracked
        coVerify(exactly = 1) { tracker.setNumSafes(1) }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        private val SAFES = listOf(SAFE_1, SAFE_2)
        private val ACTIVE_SAFE = SAFE_1
    }
}
