package io.gnosis.safe.ui.safe

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.selection.AddSafeHeader
import io.gnosis.safe.ui.safe.selection.SafeSelectionState
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class SafeSelectionViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()

    private lateinit var safeSelectionViewModel: SafeSelectionViewModel

    @Before
    fun setup() {
        safeSelectionViewModel = SafeSelectionViewModel(safeRepository, appDispatchers)
    }

    @Test
    fun `loadSafes - should load safes and active safe`() = runBlockingTest {
        coEvery { safeRepository.getActiveSafe() } returns ACTIVE_SAFE
        coEvery { safeRepository.getSafes() } returns SAFES

        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSelectionViewModel.state.observeForever(stateObserver)

        safeSelectionViewModel.loadSafes()

        // check initial state
        with(stateObserver.values()[0] as SafeSelectionState.SafeListState) {
            assertEquals(listItems.size, 1)
            assertEquals(listItems[0], AddSafeHeader)
            assertEquals(activeSafe, null)
        }

        // check updated state
        val selectionItemsList = mutableListOf<Any>(AddSafeHeader)
        selectionItemsList.add(ACTIVE_SAFE)
        selectionItemsList.addAll(SAFES.filter { it != ACTIVE_SAFE })
        with(stateObserver.values()[1] as SafeSelectionState.SafeListState) {
            assertEquals(listItems, selectionItemsList)
            assertEquals(activeSafe, ACTIVE_SAFE)
            //check ordering
            assertEquals(listItems[0], AddSafeHeader)
            assertEquals(listItems[1], ACTIVE_SAFE)
        }

        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { safeRepository.getSafes() }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        private val SAFES = listOf(SAFE_1, SAFE_2)
        private val ACTIVE_SAFE = SAFE_2
    }
}
