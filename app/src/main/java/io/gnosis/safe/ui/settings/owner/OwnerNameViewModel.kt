package io.gnosis.safe.ui.settings.owner

import androidx.lifecycle.ViewModel
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.settings.app.SettingsHandler
import pm.gnosis.model.Solidity
import java.math.BigInteger
import javax.inject.Inject

class OwnerNameViewModel
@Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker
) : ViewModel() {

    suspend fun importOwner(address: Solidity.Address, name: String, key: BigInteger, fromSeedPhrase: Boolean) {
        credentialsRepository.saveOwner(address, key, name)
        settingsHandler.showOwnerBanner = false
        settingsHandler.showOwnerScreen = false
        tracker.logKeyImported(fromSeedPhrase)
        tracker.setNumKeysImported(1)
        notificationRepository.registerOwner(key)
    }
}
