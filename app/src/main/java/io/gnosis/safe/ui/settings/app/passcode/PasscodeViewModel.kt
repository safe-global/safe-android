package io.gnosis.safe.ui.settings.app.passcode

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.app.passcode.PasscodeViewModel.PasscodeState
import javax.inject.Inject

class PasscodeViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val encryptionManager: HeimdallEncryptionManager,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<PasscodeState>(appDispatchers) {

    override fun initialState(): PasscodeState = PasscodeState(viewAction = null)

    fun disablePasscode(passcode: String) {
        safeLaunch {
            val success = encryptionManager.unlockWithPassword(passcode.toByteArray())
            if (success) {
                settingsHandler.usePasscode = false
                tracker.setPasscodeIsSet(false)
                tracker.logPasscodeDisabled()
                updateState { PasscodeState(PasscodeDisabled) }
            } else {
                updateState { PasscodeState(PasscodeWrong) }
            }
        }
    }

    fun onForgotPasscode() {
        safeLaunch {
            credentialsRepository.owners().forEach {
                credentialsRepository.removeOwner(it)
                tracker.logKeyDeleted()
                tracker.setNumKeysImported(credentialsRepository.ownerCount())
            }
            notificationRepository.unregisterOwners()
            // Make sure all owners are deleted at this point
            if (credentialsRepository.ownerCount() == 0) {
                encryptionManager.removePassword()
                encryptionManager.lock()
                settingsHandler.usePasscode = false
                tracker.setPasscodeIsSet(false)
                tracker.logPasscodeDisabled()
                updateState { PasscodeState(AllOwnersRemoved) }
            } else {
                throw OwnerRemovalFailed
            }
        }
    }

    fun setupPasscode(password: String) {
        safeLaunch {
            encryptionManager.removePassword()
            val success = encryptionManager.setupPassword(password.toByteArray())
            encryptionManager.lock()

            if (success) {
                settingsHandler.usePasscode = true
                settingsHandler.showPasscodeBanner = false

                tracker.setPasscodeIsSet(true)
                tracker.logPasscodeEnabled()

                updateState { PasscodeState(PasscodeSetup) }
            } else {
                throw PasscodeSetupFailed
            }
        }
    }

    fun unlockWithPasscode(passcode: String) {
        safeLaunch {
            if (encryptionManager.unlockWithPassword(passcode.toByteArray())) {
                updateState { PasscodeState(PasscodeCorrect) }
            } else {
                updateState { PasscodeState(PasscodeWrong) }
            }
        }
    }

    fun disableAndSetNewPasscode(newPasscode: String, oldPasscode: String) {
        safeLaunch {
            val success = encryptionManager.setupPassword(newPasscode = newPasscode.toByteArray(), oldPasscode = oldPasscode.toByteArray())

            if (success) {
                tracker.setPasscodeIsSet(true)
                updateState { PasscodeState(PasscodeChanged) }
            } else {
                updateState { PasscodeState(PasscodeWrong) }
            }
        }
    }

    data class PasscodeState(override var viewAction: ViewAction?) : State
    object AllOwnersRemoved : ViewAction
    object OwnerRemovalFailed : Throwable()
    object PasscodeDisabled : ViewAction
    object PasscodeWrong : ViewAction
    object PasscodeCorrect : ViewAction
    object PasscodeSetup : ViewAction
    object PasscodeSetupFailed : Throwable()
    object PasscodeChanged : ViewAction

}
