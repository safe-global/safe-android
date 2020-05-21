package io.gnosis.safe.ui.safe.empty


import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class NoSafeViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>(relaxed = true)

    private lateinit var viewModel: NoSafeViewModel


    @Test
    fun `init - should emit active safe`() = runBlockingTest {

        coEvery { safeRepository.getActiveSafe() } returns SAFE_1
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(SAFE_1)
        }
            .conflate()

        viewModel = NoSafeViewModel(safeRepository, appDispatchers)

        viewModel.state.test().assertValueAt(0) {
            it.activeSafe == null &&
                    it.viewAction is BaseStateViewModel.ViewAction.Loading
        }

        viewModel.state.test().assertValueAt(0) {
            it.activeSafe == SAFE_1 &&
                    it.viewAction is BaseStateViewModel.ViewAction.None

        }
    }

    @Test
    fun `on remove safe - should update state`()  = runBlockingTest {

        coEvery { safeRepository.removeSafe(ACTIVE_SAFE) } just Runs
        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(SAFE_1, null)
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(SAFE_1)
            emit(null)
        }
            .conflate()

        viewModel = NoSafeViewModel(safeRepository, appDispatchers)
        val stateObserver = TestLiveDataObserver<SafesState>()
        viewModel.state.observeForever(stateObserver)

        stateObserver.assertValueAt(0) {
            it.activeSafe == null &&
                    it.viewAction is BaseStateViewModel.ViewAction.Loading
        }

        stateObserver.assertValueAt(0) {
            it.activeSafe == SAFE_1 &&
                    it.viewAction is BaseStateViewModel.ViewAction.NavigateTo
        }

        safeRepository.removeSafe(SAFE_1)

        stateObserver.assertValueAt(0) {
            it.activeSafe == null &&
                    it.viewAction is BaseStateViewModel.ViewAction.None
    }}

    @Test
    fun `on select safe - should update state`() = runBlockingTest {

        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(null, SAFE_2)
        coEvery { safeRepository.setActiveSafe(any()) } just Runs

        viewModel = NoSafeViewModel(safeRepository, appDispatchers)

        viewModel.state.test().assertValueAt(0) {
            it.activeSafe == null &&
                    it.viewAction is BaseStateViewModel.ViewAction.Loading
        }

        viewModel.state.test().assertValueAt(0) {
            it.activeSafe == null &&
                    it.viewAction is BaseStateViewModel.ViewAction.None
        }

        safeRepository.setActiveSafe(SAFE_2)

        viewModel.state.test().assertValueAt(0) {
            it.activeSafe == SAFE_2 &&
                    it.viewAction is BaseStateViewModel.ViewAction.NavigateTo

        }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        private val ACTIVE_SAFE = SAFE_1
    }
}
