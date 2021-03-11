package io.gnosis.safe.ui.settings.owner

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.*
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.CloseScreen
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.owner.list.OwnerSelectionState
import io.gnosis.safe.ui.settings.owner.list.OwnerSelectionViewModel
import io.gnosis.safe.utils.MnemonicKeyAndAddressDerivator
import io.mockk.*
import org.junit.Rule
import org.junit.Test
import pm.gnosis.model.Solidity
import java.math.BigInteger

class OwnerSelectionViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    private val derivator = mockk<MnemonicKeyAndAddressDerivator>()
    private val credentialsRepository = mockk<CredentialsRepository>()
    private val notificationRepository = mockk<NotificationRepository>()
    private val settingsHandler = mockk<SettingsHandler>()

    private val tracker = mockk<Tracker>()

    private lateinit var viewModel: OwnerSelectionViewModel

    @Test
    fun `importOwner - should store owner & track event`() {

        coEvery { derivator.keyForIndex(any()) } returns BigInteger.ONE
        coEvery { derivator.addressesForPage(any(), any()) } returns listOf(Solidity.Address(BigInteger.ZERO))
        coEvery { credentialsRepository.saveOwner(any(), any()) } just Runs
        coEvery { notificationRepository.registerOwner(any()) } just Runs
        coEvery { settingsHandler.showOwnerBanner = false } just Runs
        coEvery { settingsHandler.showOwnerScreen = false } just Runs
        coEvery { tracker.logKeyImported(any()) } just Runs
        coEvery { tracker.setNumKeysImported(any()) } just Runs

        viewModel = OwnerSelectionViewModel(derivator, credentialsRepository, notificationRepository, settingsHandler, tracker, appDispatchers)
        val testObserver = TestLiveDataObserver<OwnerSelectionState>()
        viewModel.state.observeForever(testObserver)

        viewModel.importOwner()

        testObserver.assertValues(
            OwnerSelectionState(Loading(true)),
            OwnerSelectionState(CloseScreen)
        )

        coVerifySequence {
            derivator.keyForIndex(any())
            derivator.addressesForPage(any(), any())
            credentialsRepository.saveOwner(any(), any())
            settingsHandler.showOwnerBanner = false
            settingsHandler.showOwnerScreen = false
            tracker.logKeyImported(true)
            tracker.setNumKeysImported(any())
            notificationRepository.registerOwner(any())
        }
    }
}
