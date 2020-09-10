package io.gnosis.safe.ui.assets.collectibles

import io.gnosis.data.models.Collectible
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.adapter.Adapter
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class CollectiblesViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private lateinit var viewModel: CollectiblesViewModel

    private val tokenRepository = mockk<TokenRepository>()
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

        viewModel = CollectiblesViewModel(tokenRepository, safeRepository, appDispatchers)

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            tokenRepository.loadCollectiblesOf(safe1.address)
            safeRepository.getActiveSafe()
            tokenRepository.loadCollectiblesOf(safe10.address)
        }
    }

    @Test
    fun `load - should emit collectibles view data list`() {
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        val collectibles = buildCollectibleList()
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe1")
        coEvery { safeRepository.activeSafeFlow() } returns flow {
            emit(safe)
        }
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { tokenRepository.loadCollectiblesOf(any()) } returns collectibles

        val collectiblesViewData: List<CollectibleViewData> = listOf(
            CollectibleViewData.NftHeader(
                "tokenName1",
                null,
                true
            ),
            CollectibleViewData.CollectibleItem(
                collectibles[0]
            ),
            CollectibleViewData.CollectibleItem(
                collectibles[1]
            ),
            CollectibleViewData.NftHeader(
                "tokenName2",
                null,
                false
            ),
            CollectibleViewData.CollectibleItem(
                collectibles[2]
            )
        )

        viewModel = CollectiblesViewModel(tokenRepository, safeRepository, appDispatchers)

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(
            CollectiblesState(loading = false, refreshing = false, viewAction = UpdateCollectibles(Adapter.Data(null, collectiblesViewData)))
        )
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            tokenRepository.loadCollectiblesOf(safe.address)
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
