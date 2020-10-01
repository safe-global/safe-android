package io.gnosis.safe.ui.assets

import androidx.core.content.edit
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject

class SafeBalancesViewModel @Inject constructor(
        private val safeRepository: SafeRepository,
        private val encryptionManager: EncryptionManager,
        private val preferencesManager: PreferencesManager,
        appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeBalancesState>(appDispatchers) {

    override fun initialState(): SafeBalancesState = SafeBalancesState.SafeLoading(null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    SafeBalancesState.ActiveSafe(safe, null)
                }
            }
        }
        val hardcodedPassword = "Hardcoded Passwort"

        //remove old key
        preferencesManager.prefs.edit { remove("encryption_manager.string.password_encrypted_app_key") }
        preferencesManager.prefs.edit { remove("encryption_manager.string.password_checksum") }
        val result = encryptionManager.setupPassword("Hardcoded Passwort".toByteArray())
        Timber.i("---> setUpPassword: result: $result")

        if (encryptionManager.unlocked()) {
            Timber.i("---> encryptionManager.unlocked()")


            val cleartext = "1234567890"
            Timber.i("---> cleartext: $cleartext")
            Timber.i("---> cleartext(hex): ${cleartext.toByteArray().toHexString()}")

            val encrypted = encryptionManager.encrypt(cleartext.toByteArray())
            Timber.i("---> encrypted: ${encrypted.data.toHexString()}")

            preferencesManager.prefs.edit { putString("encryption_manager.string.encrypted.value", encrypted.data.toHexString()) }
            preferencesManager.prefs.edit { putString("encryption_manager.string.encrypted.iv", encrypted.iv.toHexString()) }

            val decrypted = encryptionManager.decrypt(encrypted)

            Timber.i("---> decrypted: ${String(decrypted)}")
        } else {
            Timber.i("---> !encryptionManager.unlocked()")

            val result = encryptionManager.unlockWithPassword(hardcodedPassword.toByteArray())
            Timber.i("---> unlockWithPassword: result: $result")

            val encrypted = preferencesManager.prefs.getString("encryption_manager.string.encrypted.value", "")!!.hexToByteArray()
            val iv = preferencesManager.prefs.getString("encryption_manager.string.encrypted.iv", "")!!.hexToByteArray()
            Timber.i("---> encrypted: ${encrypted.toHexString()}")

            val decrypted = encryptionManager.decrypt(EncryptionManager.CryptoData(encrypted, iv))
            Timber.i("---> decrypted: ${String(decrypted)}")

        }
    }
}

sealed class SafeBalancesState : BaseStateViewModel.State {

    data class SafeLoading(
            override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeBalancesState()

    data class ActiveSafe(
            val safe: Safe?,
            override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeBalancesState()
}
