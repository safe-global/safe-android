package io.gnosis.safe.ui.settings.app.fiat

import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppFiatViewModelTest {

    private val settingsHandler = mockk<SettingsHandler>()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    @Test
    fun `fetchSupportedFiatCodes - success`() = runBlocking {
        val supportedFiat = listOf("USD", "EUR", "RUB", "CHD")
        val stateObserver = TestLiveDataObserver<AppFiatFragmentState>()
        coEvery { settingsHandler.loadSupportedFiatCodes() } returns supportedFiat

        val viewModel = AppFiatViewModel(settingsHandler, appDispatchers)
        viewModel.state().observeForever(stateObserver)
        viewModel.fetchSupportedFiatCodes()

        with(stateObserver.values()) {
            assertEquals(1, size)
            assertEquals(AppFiatFragmentState(FiatList(supportedFiat)), get(0))
        }
        coVerify(exactly = 1) { settingsHandler.loadSupportedFiatCodes() }
    }

    @Test
    fun `fetchSupportedFiatCodes - backend error`() = runBlocking {
        val throwable = Throwable()
        val stateObserver = TestLiveDataObserver<AppFiatFragmentState>()
        coEvery { settingsHandler.loadSupportedFiatCodes() } throws throwable

        val viewModel = AppFiatViewModel(settingsHandler, appDispatchers)
        viewModel.state().observeForever(stateObserver)
        viewModel.fetchSupportedFiatCodes()

        with(stateObserver.values()) {
            assertEquals(1, size)
            assertEquals(AppFiatFragmentState(BaseStateViewModel.ViewAction.ShowError(throwable)), get(0))
        }
        coVerify(exactly = 1) { settingsHandler.loadSupportedFiatCodes() }
    }

    @Test
    fun fetchDefaultUserFiat() = runBlocking {
        val stateObserver = TestLiveDataObserver<AppFiatFragmentState>()
        coEvery { settingsHandler.userDefaultFiat } returns "EUR"

        val viewModel = AppFiatViewModel(settingsHandler, appDispatchers)
        viewModel.state().observeForever(stateObserver)
        viewModel.fetchDefaultUserFiat()

        with(stateObserver.values()) {
            assertEquals(1, size)
            assertEquals(AppFiatFragmentState(SelectFiat("EUR")), get(0))
        }
        coVerify(exactly = 1) { settingsHandler.userDefaultFiat }
    }

    @Test
    fun selectedFiatCodeChanged() {
        val stateObserver = TestLiveDataObserver<AppFiatFragmentState>()
        val fiatCodeSpy = slot<String>()
        coEvery { settingsHandler.userDefaultFiat = capture(fiatCodeSpy) } answers {}

        val viewModel = AppFiatViewModel(settingsHandler, appDispatchers)
        viewModel.state().observeForever(stateObserver)
        viewModel.selectedFiatCodeChanged("CAD")

        with(stateObserver.values()) {
            assertEquals(1, size)
            assertEquals(AppFiatFragmentState(SelectFiat("CAD")), get(0))
        }
        assertEquals("CAD", fiatCodeSpy.captured)
    }
}
