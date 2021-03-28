package io.gnosis.safe.ui.settings.app.passcode

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.*
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.app.passcode.PasscodeViewModel.ResetPasscodeState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
    fun `disablePasscode() (with right passcode) should disable passcode`() {
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
    fun `disablePasscode() (with wrong passcode) should not disable passcode`() {
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
    fun `onForgotPasscode() () then `() {


    }
}
