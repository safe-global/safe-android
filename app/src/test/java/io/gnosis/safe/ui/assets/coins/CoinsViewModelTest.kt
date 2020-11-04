package io.gnosis.safe.ui.assets.coins

import io.gnosis.data.models.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.MainCoroutineScopeRule
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.*
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigDecimal
import java.math.BigInteger

class CoinsViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private lateinit var viewModel: CoinsViewModel

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

        viewModel = CoinsViewModel(tokenRepository, safeRepository, appDispatchers)

        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            tokenRepository.loadBalanceOf(safe1.address)
            safeRepository.getActiveSafe()
            tokenRepository.loadBalanceOf(safe10.address)
        }
    }

    @Test
    fun `load (tokenRepository failure) should emit throwable`() {
        viewModel = CoinsViewModel(tokenRepository, safeRepository, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        val throwable = Throwable()
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe1")
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { tokenRepository.loadBalanceOf(any()) } throws throwable

        viewModel.load()

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(
            CoinsState(loading = true, refreshing = false, viewAction = BaseStateViewModel.ViewAction.ShowError(error = throwable))
        )
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            tokenRepository.loadBalanceOf(safe.address)
        }
    }

    @Test
    fun `load (active safe failure) should emit throwable`() {
        viewModel = CoinsViewModel(tokenRepository, safeRepository, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        val throwable = Throwable()
        coEvery { safeRepository.getActiveSafe() } throws throwable

        viewModel.load()

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(
            CoinsState(loading = false, refreshing = false, viewAction = BaseStateViewModel.ViewAction.ShowError(error = throwable))
        )
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            tokenRepository wasNot Called
        }
    }

    @Test
    fun `load - should emit balance list`() {
        viewModel = CoinsViewModel(tokenRepository, safeRepository, appDispatchers)
        val stateObserver = TestLiveDataObserver<BaseStateViewModel.State>()
        val balances = listOf(buildBalance(0), buildBalance(1), buildBalance(2))
        val safe = Safe(Solidity.Address(BigInteger.ONE), "safe1")
        coEvery { safeRepository.getActiveSafe() } returns safe
        coEvery { tokenRepository.loadBalanceOf(any()) } returns CoinBalances(BigDecimal.ZERO, balances)

        viewModel.load()

        viewModel.state.observeForever(stateObserver)
        stateObserver.assertValues(
            CoinsState(loading = false, refreshing = false, viewAction = UpdateBalances(balances, BigDecimal.ZERO))
        )
        coVerifySequence {
            safeRepository.activeSafeFlow()
            safeRepository.getActiveSafe()
            tokenRepository.loadBalanceOf(safe.address)
        }
    }

    private fun buildBalance(index: Long) =
        Balance(
            buildTokenInfo(index),
            BigInteger.valueOf(index),
            BigDecimal.valueOf(index)
        )

    private fun buildTokenInfo(index: Long) =
        TokenInfo(
            TokenType.ERC20,
            Solidity.Address(BigInteger.valueOf(index)),
            15,
            "symbol$index",
            "name$index",
            "logo.uri.$index"
        )
}
