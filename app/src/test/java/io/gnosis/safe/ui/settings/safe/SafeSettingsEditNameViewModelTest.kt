package io.gnosis.safe.ui.settings.safe

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.notifications.NotificationManager
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class SafeSettingsEditNameViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>(relaxed = true)
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private lateinit var viewModel: SafeSettingsEditNameViewModel

    @Test
    fun `saveLocalName (name) - should change local name for safe`() = runBlockingTest {

        coEvery { safeRepository.activeSafeFlow() } returnsMany listOf(
            flow {
                emit(SAFE_1)
            },
            flow {
                emit(SAFE_2)
            }
        )
        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(SAFE_1, SAFE_2)
        coEvery { safeRepository.saveSafe(any()) } just Runs

        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel = SafeSettingsEditNameViewModel(safeRepository, notificationManager, appDispatchers)
        viewModel.state.observeForever(stateObserver)

        with(stateObserver.values()[0] as EditNameState) {
            assertTrue(name == "safe1" && viewAction is BaseStateViewModel.ViewAction.None)
        }

        viewModel.saveLocalName("safe2")

        with(stateObserver.values()[1] as EditNameState) {
            assertTrue(name == "safe2" && viewAction is BaseStateViewModel.ViewAction.CloseScreen)
        }
    }

    @Test
    fun `saveLocalName (safeRepository fails to save active safe) - should keep local name and emit ShowError`() = runBlockingTest {
        val throwable = IllegalStateException()
        coEvery { safeRepository.activeSafeFlow() } returnsMany listOf(
            flow {
                emit(SAFE_1)
            },
            flow {
                emit(SAFE_1)
            }
        )

        coEvery { safeRepository.getActiveSafe() } returnsMany  listOf(SAFE_1, SAFE_1)
        coEvery { safeRepository.saveSafe(any()) } throws throwable

        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        viewModel = SafeSettingsEditNameViewModel(safeRepository, notificationManager, appDispatchers)
        viewModel.state.observeForever(stateObserver)

        with(stateObserver.values()[0] as EditNameState) {
            assertTrue(name == "safe1" && viewAction is BaseStateViewModel.ViewAction.None)
        }

        viewModel.saveLocalName("safe2")

        with(stateObserver.values()[1] as EditNameState) {
            assertTrue(name == "safe1" && viewAction is BaseStateViewModel.ViewAction.ShowError)
        }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ZERO), "safe2")
    }
}
