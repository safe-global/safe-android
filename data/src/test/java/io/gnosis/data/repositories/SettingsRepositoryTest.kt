package io.gnosis.data.repositories

import android.content.SharedPreferences
import io.gnosis.data.backend.GatewayApi
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager

class SettingsRepositoryTest {

    private val gatewayApi = mockk<GatewayApi>()
    private val prefs = mockk<SharedPreferences>()
    private val preferencesManager = mockk<PreferencesManager>().also {
        every { it.prefs } returns prefs
    }

    private val settingsRepository = SettingsRepository(gatewayApi, preferencesManager)

    @Test
    fun `loadSupportedFiatCodes - backend error`() = runBlocking {
        val backendError = Throwable()
        coEvery { gatewayApi.loadSupportedCurrencies() } throws backendError

        val actual = runCatching { settingsRepository.loadSupportedFiatCodes() }

        coVerify(exactly = 1) { gatewayApi.loadSupportedCurrencies() }
        assertEquals(backendError, actual.exceptionOrNull())
    }

    @Test
    fun `loadSupportedFiatCodes - successful`() = runBlocking {
        val supportedFiats = listOf("USD", "EUR", "CAD")
        coEvery { gatewayApi.loadSupportedCurrencies() } returns supportedFiats

        val actual = runCatching { settingsRepository.loadSupportedFiatCodes() }
        val expected = supportedFiats.sorted()

        coVerify(exactly = 1) { gatewayApi.loadSupportedCurrencies() }
        assertEquals(expected, actual.getOrNull())
    }

    @Test
    fun `getUserDefaultFiat - no value set`() = runBlocking {
        every { prefs.getString(any(), any()) } returns null

        val actual = settingsRepository.getUserDefaultFiat()
        val expected = "USD"

        verify(exactly = 1) { prefs.getString(SettingsRepository.USER_DEFAULT_FIAT, "USD") }
        assertEquals(expected, actual)
    }

    @Test
    fun `getUserDefaultFiat - value set`() = runBlocking {
        every { prefs.getString(any(), any()) } returns "EUR"

        val actual = settingsRepository.getUserDefaultFiat()
        val expected = "EUR"

        verify(exactly = 1) { prefs.getString(SettingsRepository.USER_DEFAULT_FIAT, "USD") }
        assertEquals(expected, actual)
    }
}
