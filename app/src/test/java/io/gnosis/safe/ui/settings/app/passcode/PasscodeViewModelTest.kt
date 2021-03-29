package io.gnosis.safe.ui.settings.app.passcode

import io.gnosis.data.models.Owner
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.*
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.app.passcode.PasscodeViewModel.*
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.utils.asEthereumAddress

class PasscodeViewModelTest {

    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()


    private val credentialsRepository = mockk<CredentialsRepository>(relaxed = true)
    private val encryptionManager = mockk<HeimdallEncryptionManager>(relaxed = true)
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val settingsHandler = mockk<SettingsHandler>(relaxed = true)
    private val tracker = mockk<Tracker>(relaxed = true)

    private lateinit var viewModel: PasscodeViewModel

    private val examplePasscode = "123456"

    @Before
    fun setUp() {
        viewModel = PasscodeViewModel(credentialsRepository, notificationRepository, encryptionManager, settingsHandler, tracker, appDispatchers)
    }

    @Test
    fun `disablePasscode (with right passcode) should disable passcode`() {
        coEvery { encryptionManager.unlockWithPassword(examplePasscode.toByteArray()) } returns true
        val testObserver = TestLiveDataObserver<ResetPasscodeState>()
        viewModel.state.observeForever(testObserver)

        viewModel.disablePasscode(examplePasscode)

        testObserver.assertValues(
            ResetPasscodeState(null),
            ResetPasscodeState(PasscodeViewModel.PasswordDisabled)
        )

        coVerify { settingsHandler.usePasscode = false }
        coVerify { tracker.setPasscodeIsSet(false) }
        coVerify { tracker.logPasscodeDisabled() }
    }

    @Test
    fun `disablePasscode (with wrong passcode) should not disable passcode`() {
        // given: wrong passcode
        coEvery { encryptionManager.unlockWithPassword(examplePasscode.toByteArray()) } returns false
        val testObserver = TestLiveDataObserver<ResetPasscodeState>()
        viewModel.state.observeForever(testObserver)

        viewModel.disablePasscode(examplePasscode)

        testObserver.assertValues(
            ResetPasscodeState(null),
            ResetPasscodeState(PasscodeViewModel.PasswordWrong)
        )
    }

    @Test
    fun `onForgotPasscode - (successful owner deletion) should remove passcode and delete owner data `() {
        coEvery { credentialsRepository.owners() } returns listOf(Owner(address = "0x00".asEthereumAddress()!!, type = Owner.Type.LOCALLY_STORED))
        coEvery { credentialsRepository.ownerCount() } returns 0
        every { encryptionManager.removePassword() } just Runs
        every { encryptionManager.lock() } just Runs
        coEvery { credentialsRepository.removeOwner(any()) } just Runs

        val testObserver = TestLiveDataObserver<ResetPasscodeState>()
        viewModel.state.observeForever(testObserver)

        viewModel.onForgotPasscode()

        testObserver.assertValues(
            ResetPasscodeState(null),
            ResetPasscodeState(AllOwnersRemoved)
        )
        coVerify(exactly = 1) { credentialsRepository.removeOwner(any()) }
        coVerify(exactly = 1) { notificationRepository.unregisterOwner() }
        verify(exactly = 1) { encryptionManager.removePassword() }
        verify(exactly = 1) { encryptionManager.lock() }
        coVerify(exactly = 1) { settingsHandler.usePasscode = false }
        coVerify(exactly = 1) { tracker.setPasscodeIsSet(false) }
        coVerify(exactly = 1) { tracker.logPasscodeDisabled() }
    }

    @Test
    fun `onForgotPasscode - (owner deletion failed) should remove passcode and delete owner data `() {
        coEvery { credentialsRepository.owners() } returns listOf(Owner(address = "0x00".asEthereumAddress()!!, type = Owner.Type.LOCALLY_STORED))
        coEvery { credentialsRepository.ownerCount() } returns 1
        every { encryptionManager.removePassword() } just Runs
        every { encryptionManager.lock() } just Runs
        coEvery { credentialsRepository.removeOwner(any()) } just Runs

        val testObserver = TestLiveDataObserver<ResetPasscodeState>()
        viewModel.state.observeForever(testObserver)

        viewModel.onForgotPasscode()

        testObserver.assertValues(
            ResetPasscodeState(null),
            ResetPasscodeState(OwnerRemovalFailed)
        )
        coVerify(exactly = 1) { credentialsRepository.removeOwner(any()) }
        coVerify(exactly = 1) { notificationRepository.unregisterOwner() }
        verify(exactly = 0) { encryptionManager.removePassword() }
        verify(exactly = 0) { encryptionManager.lock() }
        coVerify(exactly = 0) { settingsHandler.usePasscode = false }
        coVerify(exactly = 0) { tracker.setPasscodeIsSet(false) }
        coVerify(exactly = 0) { tracker.logPasscodeDisabled() }
    }
}
