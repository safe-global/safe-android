package io.gnosis.safe.ui.safe.settings

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.settings.safe.SafeSettingsViewModel
import io.mockk.*
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
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

    private val tracker = mockk<Tracker>()

    private lateinit var safeSettingsViewModel: SafeSettingsViewModel


    @Test
    fun `removeSafe - should remove safe`() = runBlockingTest {

        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(SAFE_1, null)
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(SAFE_1)
            emit(null)
        }
            .conflate()
        coEvery { safeRepository.clearActiveSafe() } just Runs
        coEvery { safeRepository.getSafes() } returnsMany listOf(SAFES, listOf(SAFE_2))
        coEvery { safeRepository.removeSafe(ACTIVE_SAFE) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs

        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, tracker, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSettingsViewModel.state.observeForever(stateObserver)

        val safeCount = safeRepository.getSafes().count()
        assert(safeCount == 2)

        safeSettingsViewModel.removeSafe()

        with(stateObserver.values()[0]) {
            val viewAction = this.viewAction
            assert (
                viewAction is BaseStateViewModel.ViewAction.UpdateActiveSafe &&
                        viewAction.newSafe == null
            )
        }

        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { safeRepository.removeSafe(SAFE_1) }
        coVerify(exactly = 1) { safeRepository.clearActiveSafe() }

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
