package io.gnosis.safe.ui.settings.app.passcode

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import javax.inject.Inject


class ChangePasscodeViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val encryptionManager: HeimdallEncryptionManager,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<ChangePasscodeViewModel.ChangePasscodeState>(appDispatchers) {

    override fun initialState(): ChangePasscodeState = ChangePasscodeState(viewAction = null)

    fun removeOwner() {
        safeLaunch {
            credentialsRepository.owners().forEach {
                credentialsRepository.removeOwner(it)
            }
            // Make sure all owners are deleted at this point
            if (credentialsRepository.ownerCount() == 0) {
                encryptionManager.removePassword()
                settingsHandler.usePasscode = false
                updateState { ChangePasscodeState(AllOwnersRemoved) }
            } else {
                updateState { ChangePasscodeState(OwnerRemovalFailed) }
            }
            notificationRepository.unregisterOwner()
        }
    }

    fun verifyPasscode(passcode: String) {
        safeLaunch {
            val success = encryptionManager.unlockWithPassword(passcode.toByteArray())
            if (success) {
                updateState { ChangePasscodeState(PasscodeVerified) }
            } else {
                updateState { ChangePasscodeState(PasscodeWrong) }
            }
        }
    }

    fun disableAndSetNewPasscode(newPasscode: String, oldPasscode: String) {
        safeLaunch {
            val success = encryptionManager.setupPassword(newPasscode = newPasscode.toByteArray(), oldPasscode = oldPasscode.toByteArray())

            if (success) {
                tracker.setPasscodeIsSet(true)
                updateState { ChangePasscodeState(PasscodeChanged) }
            } else {
                updateState { ChangePasscodeState(PasscodeWrong) }
            }
        }
    }

    data class ChangePasscodeState(override var viewAction: ViewAction?) : State
    object PasscodeChanged : ViewAction
    object PasscodeVerified : ViewAction
    object PasscodeWrong : ViewAction
    object AllOwnersRemoved : ViewAction
    object OwnerRemovalFailed : ViewAction
}
