package io.gnosis.safe.ui.settings.app.passcode

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import javax.inject.Inject

class PasscodeViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val encryptionManager: HeimdallEncryptionManager,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<PasscodeViewModel.ResetPasscodeState>(appDispatchers) {

    override fun initialState(): ResetPasscodeState = ResetPasscodeState(viewAction = null)

    fun disablePasscode(passcode: String) {
        safeLaunch {
            val success = encryptionManager.unlockWithPassword(passcode.toByteArray())
            if (success) {
                settingsHandler.usePasscode = false
                tracker.setPasscodeIsSet(false)
                tracker.logPasscodeDisabled()
                updateState { ResetPasscodeState(PasswordDisabled) }
            } else {
                updateState { ResetPasscodeState(PasswordWrong) }
            }
        }
    }

    fun onForgotPasscode() {
        safeLaunch {

            encryptionManager.removePassword()
            encryptionManager.lock()
            settingsHandler.usePasscode = false

            credentialsRepository.owners().forEach {
                credentialsRepository.removeOwner(it)
            }
            // Make sure all owners are deleted at this point
            if (credentialsRepository.ownerCount() == 0) {
                updateState { ResetPasscodeState(AllOwnersRemoved) }
            } else {
                throw OwnerRemovalFailed
            }
            notificationRepository.unregisterOwner()
        }
    }

    data class ResetPasscodeState(override var viewAction: ViewAction?) : State
    object AllOwnersRemoved : ViewAction
    object OwnerRemovalFailed : Throwable()
    object PasswordDisabled : ViewAction
    object PasswordChanged : ViewAction
    object PasswordWrong : ViewAction
}
