package io.gnosis.safe.ui.settings.app.passcode

import android.content.Context
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.security.BiometricPasscodeManager
import io.gnosis.data.security.HeimdallEncryptionManager
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.app.passcode.PasscodeCommand.*
import io.gnosis.safe.ui.settings.app.passcode.PasscodeViewModel.PasscodeState
import timber.log.Timber
import javax.inject.Inject

class PasscodeViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val encryptionManager: HeimdallEncryptionManager,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker,
    private val safeRepository: SafeRepository,
    private val biometricPasscodeManager: BiometricPasscodeManager,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<PasscodeState>(appDispatchers) {

    override fun initialState(): PasscodeState = PasscodeState(viewAction = null)

    fun configurePasscode(passcode: String, command: PasscodeCommand) {
        safeLaunch {
            val success = encryptionManager.unlockWithPassword(passcode.toByteArray())
            if (success) {
                when (command) {
                    DISABLE -> {
                        settingsHandler.usePasscode = false
                        tracker.setPasscodeIsSet(false)
                        tracker.logPasscodeDisabled()
                        updateState { PasscodeState(PasscodeDisabled) }
                    }
                    APP_DISABLE -> {
                        settingsHandler.requirePasscodeToOpen = false
                        updateState { PasscodeState(PasscodeCommandExecuted) }
                    }
                    CONFIRMATION_DISABLE -> {
                        settingsHandler.requirePasscodeForConfirmations = false
                        updateState { PasscodeState(PasscodeCommandExecuted) }
                    }
                    CONFIRMATION_ENABLE -> {
                        settingsHandler.requirePasscodeForConfirmations = true
                        updateState { PasscodeState(PasscodeCommandExecuted) }
                    }
                    APP_ENABLE -> {
                        settingsHandler.requirePasscodeToOpen = true
                        updateState { PasscodeState(PasscodeCommandExecuted) }
                    }
                    BIOMETRICS_ENABLE -> {
                        settingsHandler.useBiometrics = true
                        encryptPasscodeWithBiometricKey(passcode)
                        updateState { PasscodeState(PasscodeCommandExecuted) }
                    }
                    BIOMETRICS_DISABLE -> {
                        settingsHandler.useBiometrics = false
                        updateState { PasscodeState(PasscodeCommandExecuted) }
                    }
                    EXPORT_ENABLE -> {
                        settingsHandler.requirePasscodeToExportKeys = true
                        updateState { PasscodeState(PasscodeCommandExecuted) }
                    }
                    EXPORT_DISABLE -> {
                        settingsHandler.requirePasscodeToExportKeys = false
                        updateState { PasscodeState(PasscodeCommandExecuted) }
                    }
                }
            } else {
                updateState { PasscodeState(PasscodeWrong) }
            }
        }
    }

    fun onForgotPasscode() {
        safeLaunch {
            tracker.logPasscodeReset()
            credentialsRepository.owners().forEach {
                credentialsRepository.removeOwner(it)
            }
            safeRepository.clearUserData()
            tracker.logKeyDeleted()
            tracker.setNumKeysImported(0)
            tracker.setNumKeysGenerated(0)
            notificationRepository.unregisterOwners()
            // Make sure all owners are deleted at this point
            if (credentialsRepository.ownerCount() == 0) {
                encryptionManager.removePassword()
                encryptionManager.lock()
                settingsHandler.usePasscode = false
                settingsHandler.useBiometrics = false
                settingsHandler.requirePasscodeToOpen = false
                settingsHandler.requirePasscodeForConfirmations = false
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
                settingsHandler.requirePasscodeForConfirmations = true
                settingsHandler.requirePasscodeToOpen = true
                settingsHandler.askForPasscodeSetupOnFirstLaunch = false

                tracker.setPasscodeIsSet(true)
                tracker.logPasscodeEnabled()

                updateState { PasscodeState(PasscodeSetup(password)) }
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

    fun encryptPasscodeWithBiometricKey(newPasscode: String) {
        if (settingsHandler.useBiometrics) {
            val cipher = biometricPasscodeManager.getInitializedRSACipherForEncryption(BiometricPasscodeManager.KEY_NAME)
            val encrypted = biometricPasscodeManager.encryptData(newPasscode, cipher)
            biometricPasscodeManager.persistEncryptedPasscodeToSharedPrefs(
                encrypted,
                BiometricPasscodeManager.FILE_NAME,
                Context.MODE_PRIVATE,
                BiometricPasscodeManager.KEY_NAME
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun biometricAuthentication(biometricPrompt: BiometricPrompt, promptInfo: BiometricPrompt.PromptInfo) {
        try {
            val cipher = biometricPasscodeManager.getInitializedRSACipherForDecryption(BiometricPasscodeManager.KEY_NAME)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } catch (e: KeyPermanentlyInvalidatedException) {
            // This happens when the user enrolled new fingerprints
            settingsHandler.useBiometrics = false
            biometricPasscodeManager.deleteKey(BiometricPasscodeManager.KEY_NAME)
        } catch (e: Throwable) {
            settingsHandler.useBiometrics = false
            Timber.e(e, "Biometric passcode")
        }
    }

    fun decryptPasscode(authenticationResult: BiometricPrompt.AuthenticationResult) {
        val encryptedPasscode = biometricPasscodeManager.retrieveEncryptedPasscodeFromSharedPrefs(
            BiometricPasscodeManager.FILE_NAME,
            Context.MODE_PRIVATE,
            BiometricPasscodeManager.KEY_NAME
        )
        try {
            encryptedPasscode?.let { cipherTestWrapper ->
                authenticationResult.cryptoObject?.cipher?.let { cipher ->
                    val passcode = biometricPasscodeManager.decryptData(cipherTestWrapper.ciphertext, cipher)
                    unlockWithPasscode(passcode)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "cannot decrypt passcode")
            settingsHandler.useBiometrics = false
        }
    }

    fun enableBiometry() {
        settingsHandler.useBiometrics = true
    }

    data class PasscodeState(override var viewAction: ViewAction?) : State
    object AllOwnersRemoved : ViewAction
    object OwnerRemovalFailed : Throwable()
    object PasscodeDisabled : ViewAction
    object PasscodeCommandExecuted : ViewAction
    object PasscodeWrong : ViewAction
    object PasscodeCorrect : ViewAction
    data class PasscodeSetup(val passcode: String) : ViewAction
    object PasscodeSetupFailed : Throwable()
    object PasscodeChanged : ViewAction
}

enum class PasscodeCommand {
    DISABLE,
    APP_DISABLE,
    CONFIRMATION_DISABLE,
    APP_ENABLE,
    CONFIRMATION_ENABLE,
    BIOMETRICS_ENABLE,
    BIOMETRICS_DISABLE,
    EXPORT_ENABLE,
    EXPORT_DISABLE
}
