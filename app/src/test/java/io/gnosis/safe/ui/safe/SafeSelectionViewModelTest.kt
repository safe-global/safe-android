package io.gnosis.safe.ui.safe

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
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
    private val repositories = mockk<Repositories>().apply {
        every { safeRepository() } returns safeRepository
    }

    private lateinit var safeSelectionViewModel: SafeSelectionViewModel

    @Before
    fun setup() {
        safeSelectionViewModel = SafeSelectionViewModel(repositories, appDispatchers)
    }

    @Test
    fun `loadSafes - should load safes and active safe`() = runBlockingTest {

        coEvery { safeRepository.getActiveSafe()} returns ACTIVE_SAFE
        coEvery { safeRepository.getSafes() } returns SAFES

        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        safeSelectionViewModel.state.observeForever(stateObserver)

        safeSelectionViewModel.loadSafes()

        // check initial state
        stateObserver.assertValueAt(0) {
            it is SafeSelectionState.SafeListState &&
                    it.listItems == listOf(AddSafeHeader()) &&
                    it.activeSafe == null
        }

        // check updated state
        val selectionItemsList = mutableListOf<Any>(AddSafeHeader())
        selectionItemsList.addAll(SAFES)
        stateObserver.assertValueAt(0) {
            it is SafeSelectionState.SafeListState &&
                    it.listItems == selectionItemsList &&
                    it.activeSafe == ACTIVE_SAFE
        }

        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { safeRepository.getSafes() }
    }

    companion object {
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1")
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2")
        private val SAFES = listOf(SAFE_1, SAFE_2)
        private val ACTIVE_SAFE = SAFE_1
    }
}
