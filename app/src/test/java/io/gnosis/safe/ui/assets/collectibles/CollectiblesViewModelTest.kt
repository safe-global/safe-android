package io.gnosis.safe.ui.assets.collectibles

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.appDispatchers
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
}
