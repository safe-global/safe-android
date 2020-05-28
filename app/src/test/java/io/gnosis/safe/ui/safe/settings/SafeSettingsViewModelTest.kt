package io.gnosis.safe.ui.safe.settings

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.settings.safe.SafeRemoved
import io.gnosis.safe.ui.safe.settings.safe.SafeSettingsState
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
        coEvery { safeRepository.getSafes() } returns listOf(SAFE_2)
        coEvery { safeRepository.removeSafe(ACTIVE_SAFE) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs

        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, tracker, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSettingsViewModel.state.observeForever(stateObserver)

        safeSettingsViewModel.removeSafe()

        with(stateObserver.values()[0] as SafeSettingsState) {
            assert(safe == null && viewAction is BaseStateViewModel.ViewAction.None)
        }

        with(stateObserver.values()[1] as SafeSettingsState) {
            assert(safe == SAFE_1 && viewAction is SafeRemoved)
        }

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            safeRepository.removeSafe(SAFE_1)
            safeRepository.clearActiveSafe()
            safeRepository.getSafes()
            // verify SAFE_REMOVE event was tracked
            tracker.setNumSafes(1)
        }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        private val SAFES = listOf(SAFE_1, SAFE_2)
        private val ACTIVE_SAFE = SAFE_1
    }
}
