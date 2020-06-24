package io.gnosis.safe.ui.safe.settings

import io.gnosis.data.models.Safe
import io.gnosis.data.models.SafeInfo
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
    fun `removeSafe (one safe) - should remove safe and clear active safe`() = runBlockingTest {

        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(SAFE_1, null)
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(SAFE_1)
            emit(null)
        }
            .conflate()
        coEvery { safeRepository.clearActiveSafe() } just Runs
        coEvery { safeRepository.getSafes() } returns listOf()
        coEvery { safeRepository.removeSafe(SAFE_1) } just Runs
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
            safeRepository.getSafes()
            safeRepository.clearActiveSafe()
            safeRepository.getSafes()
            // verify SAFE_REMOVE event was tracked
            tracker.setNumSafes(0)
        }
    }

    @Test
    fun `removeSafe (two or more safes) - should remove safe and select next safe`() = runBlockingTest {
        coEvery { safeRepository.getSafeInfo(any()) } returns SafeInfo(SAFE_1.address, BigInteger.ONE, 2)
        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(SAFE_1, SAFE_2)
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(SAFE_1)
            emit(SAFE_2)
        }
            .conflate()
        coEvery { safeRepository.setActiveSafe(any()) } just Runs
        coEvery { safeRepository.getSafes() } returns listOf(SAFE_2)
        coEvery { safeRepository.removeSafe(SAFE_1) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs

        safeSettingsViewModel = SafeSettingsViewModel(safeRepository, tracker, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSettingsViewModel.state.observeForever(stateObserver)

        safeSettingsViewModel.removeSafe()

        with(stateObserver.values()[0] as SafeSettingsState) {
            assert(safe == SAFE_2 && viewAction is BaseStateViewModel.ViewAction.None)
        }

        with(stateObserver.values()[1] as SafeSettingsState) {
            assert(safe == SAFE_1 && viewAction is SafeRemoved)
        }

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getSafeInfo(SAFE_2.address)
            safeRepository.getActiveSafe()
            safeRepository.removeSafe(SAFE_1)
            safeRepository.getSafes()
            safeRepository.setActiveSafe(SAFE_2)
            safeRepository.getSafes()
            // verify SAFE_REMOVE event was tracked
            tracker.setNumSafes(1)
        }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
    }
}
