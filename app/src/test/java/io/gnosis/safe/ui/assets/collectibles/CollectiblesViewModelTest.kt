package io.gnosis.safe.ui.assets.collectibles

import androidx.paging.PagingData
import io.gnosis.data.models.Chain
import io.gnosis.data.models.Safe
import io.gnosis.data.models.assets.Collectible
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.*
import io.gnosis.safe.ui.assets.collectibles.paging.CollectiblePagingProvider
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class CollectiblesViewModelTest {
    
    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private lateinit var viewModel: CollectiblesViewModel

    private val collectiblesPager = mockk<CollectiblePagingProvider>()
    private val safeRepository = mockk<SafeRepository>()

    @Test
    fun `init - should call load on safe change`() {
        val safe1 = Safe(Solidity.Address(BigInteger.ONE), "safe1")
        val safe10 = Safe(Solidity.Address(BigInteger.TEN), "safe10")
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(safe1)
            emit(safe10)
        }
        coEvery { safeRepository.getActiveSafe() } returnsMany listOf(safe1, safe10)

        coEvery { collectiblesPager.getCollectiblesStream(any()) } returns flow {
            emit(PagingData.empty<Collectible>())
        }

        viewModel = CollectiblesViewModel(collectiblesPager, safeRepository, appDispatchers)

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            collectiblesPager.getCollectiblesStream(safe1)
            safeRepository.getActiveSafe()
            collectiblesPager.getCollectiblesStream(safe10)
        }
    }

    @Test
    fun `load - should emit collectibles view data list`() = runTest(UnconfinedTestDispatcher()) {
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        val chain = Chain.DEFAULT_CHAIN
        val collectibles = buildCollectibleList()
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe1").apply {
            this.chain = chain
        }
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(safe)
        }
        coEvery { safeRepository.getActiveSafe() } returns safe

        coEvery { collectiblesPager.getCollectiblesStream(any()) } returns flowOf(PagingData.from(collectibles))

        val collectiblesViewData: List<CollectibleViewData> = listOf(
            CollectibleViewData.NftHeader(
                "tokenName1",
                null,
                true
            ),
            CollectibleViewData.CollectibleItem(
                collectibles[0],
                chain
            ),
            CollectibleViewData.CollectibleItem(
                collectibles[1],
                chain
            ),
            CollectibleViewData.NftHeader(
                "tokenName2",
                null,
                false
            ),
            CollectibleViewData.CollectibleItem(
                collectibles[2],
                chain
            )
        )

        viewModel = CollectiblesViewModel(collectiblesPager, safeRepository, appDispatchers)

        viewModel.state.observeForever(stateObserver)

        stateObserver.assertValueAt(0) {
            it is CollectiblesState && !it.loading && !it.refreshing && it.viewAction is UpdateCollectibles
        }
        val actualViewData = (stateObserver.values()[0].viewAction as UpdateCollectibles).collectibles.collectData()
        assertEquals(collectiblesViewData, actualViewData)

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            collectiblesPager.getCollectiblesStream(safe)
        }
    }

    private fun buildCollectibleList() = listOf(
        Collectible(
            "1",
            Solidity.Address(BigInteger.ZERO),
            "tokenName1",
            "TS1",
            null,
            "name1",
            null,
            null,
            null
        ),
        Collectible(
            "2",
            Solidity.Address(BigInteger.ZERO),
            "tokenName1",
            "TS1",
            null,
            "name2",
            null,
            null,
            null
        ),
        Collectible(
            "3",
            Solidity.Address(BigInteger.ONE),
            "tokenName2",
            "TS2",
            null,
            "name3",
            null,
            null,
            null
        )
    )
}
