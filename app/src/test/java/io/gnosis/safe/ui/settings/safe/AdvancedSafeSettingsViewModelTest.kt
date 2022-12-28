package io.gnosis.safe.ui.settings.safe

import io.gnosis.data.models.*
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class AdvancedSafeSettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val safeRepository = mockk<SafeRepository>(relaxed = true)
    private lateinit var viewModel: AdvancedSafeSettingsViewModel
    private val defaultActiveSafe = Safe(Solidity.Address(BigInteger.ONE), "test")

    @Test
    fun `init - (no input) should emit viewAction None is Loading`() = runBlocking {
        val stateObserver = TestLiveDataObserver<AdvancedSafeSettingsState>()
        viewModel = AdvancedSafeSettingsViewModel(safeRepository, appDispatchers)

        viewModel.state.observeForever(stateObserver)

        stateObserver.assertValueCount(1)
        with(stateObserver.values()[0]) {
            assertEquals(BaseStateViewModel.ViewAction.None, viewAction)
            assertEquals(isLoading, true)
        }

    }

    @Test
    fun `load - (safeRepository failure) should emit viewAction None is Loading`() = runBlocking {
        val stateObserver = TestLiveDataObserver<AdvancedSafeSettingsState>()
        val throwable = Throwable()
        coEvery { safeRepository.getSafeInfo(any()) } throws throwable
        coEvery { safeRepository.getActiveSafe() } returns defaultActiveSafe
        viewModel = AdvancedSafeSettingsViewModel(safeRepository, appDispatchers)

        viewModel.state.observeForever(stateObserver)
        viewModel.load()

        stateObserver.assertValueCount(2)
        with(stateObserver.values()[1]) {
            assertEquals(BaseStateViewModel.ViewAction.ShowError(throwable), viewAction)
            assertEquals(isLoading, true) // State viewModel overrides action, not the rest of the parameters
        }
    }

    @Test
    fun `load - (safeRepository success) should emit viewAction with SafeInfo`() = runBlocking {
        val stateObserver = TestLiveDataObserver<AdvancedSafeSettingsState>()
        val safeInfo = SafeInfo(
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            BigInteger.TEN,
            2,
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            emptyList(),
            AddressInfo(Solidity.Address(BigInteger.ONE)),
            null,
            "1.1.1",
            VersionState.OUTDATED
        )
        val chain = Chain.DEFAULT_CHAIN
        coEvery { safeRepository.getSafeInfo(any()) } returns safeInfo
        coEvery { safeRepository.getActiveSafe() } returns defaultActiveSafe
        viewModel = AdvancedSafeSettingsViewModel(safeRepository, appDispatchers)

        viewModel.state.observeForever(stateObserver)
        viewModel.load()

        stateObserver.assertValueCount(2)
        with(stateObserver.values()[1]) {
            assertEquals(LoadSafeInfo(chain, safeInfo), viewAction)
            assertEquals(isLoading, false)
        }
    }

    @Test
    fun `isDefaultFallbackHandler - (any address) should return false` () {
        viewModel = AdvancedSafeSettingsViewModel(safeRepository, appDispatchers)

        val actual = viewModel.isDefaultFallbackHandler(Solidity.Address(BigInteger.ONE))

        assertEquals(false, actual)
    }

    @Test
    fun `isDefaultFallbackHandler - (default fallback handler) should return true` () {
        viewModel = AdvancedSafeSettingsViewModel(safeRepository, appDispatchers)

        val actual = viewModel.isDefaultFallbackHandler(SafeRepository.DEFAULT_FALLBACK_HANDLER)

        assertEquals(true, actual)
    }
}
