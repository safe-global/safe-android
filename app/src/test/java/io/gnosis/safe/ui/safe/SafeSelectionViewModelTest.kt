package io.gnosis.safe.ui.safe

import io.gnosis.data.BuildConfig
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.ChainInfoRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.selection.SafeSelectionState
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewData.*
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
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>()

    private val chainInfoRepository = mockk<ChainInfoRepository>()

    private lateinit var safeSelectionViewModel: SafeSelectionViewModel

    @Before
    fun setup() {
        safeSelectionViewModel = SafeSelectionViewModel(safeRepository, chainInfoRepository, appDispatchers)
    }

    @Test
    fun `loadSafes - should load safes and active safe`() = runBlockingTest {
        coEvery { safeRepository.getActiveSafe() } returns ACTIVE_SAFE
        coEvery { safeRepository.getSafesForChain(any()) } returns SAFES
        coEvery { chainInfoRepository.getChains() } returns listOf()

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
        selectionItemsList.add(ChainHeader(BuildConfig.BLOCKCHAIN_NAME, BuildConfig.CHAIN_BACKGROUND_COLOR))
        selectionItemsList.add(SafeItem(ACTIVE_SAFE))
        selectionItemsList.addAll(SAFES.filter { it != ACTIVE_SAFE }.map { SafeItem(it) })
        with(stateObserver.values()[1] as SafeSelectionState.SafeListState) {
            assertEquals(listItems, selectionItemsList)
            assertEquals(activeSafe, ACTIVE_SAFE)
            //check ordering
            assertEquals(listItems[0], AddSafeHeader)
            assertEquals(listItems[1], ChainHeader(BuildConfig.BLOCKCHAIN_NAME, BuildConfig.CHAIN_BACKGROUND_COLOR))
            assertEquals(listItems[2], SafeItem(ACTIVE_SAFE))
        }

        coVerify(exactly = 1) { safeRepository.getActiveSafe() }
        coVerify(exactly = 1) { safeRepository.getSafesForChain(any()) }
        coVerify(exactly = 1) { chainInfoRepository.getChains() }
    }

    companion object {
        private val CHAIN = Chain.DEFAULT_CHAIN
        private val SAFE_1 = Safe(Solidity.Address(BigInteger.ZERO), "safe1").apply {
            chain = CHAIN
        }
        private val SAFE_2 = Safe(Solidity.Address(BigInteger.ONE), "safe2").apply {
            chain = CHAIN
        }
        private val SAFES = listOf(SAFE_1, SAFE_2)
        private val ACTIVE_SAFE = SAFE_2
    }
}
