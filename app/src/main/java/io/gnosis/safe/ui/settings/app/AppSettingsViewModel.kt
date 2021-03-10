package io.gnosis.safe.ui.settings.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import javax.inject.Inject

class AppSettingsViewModel @Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker
) : ViewModel() {

    val signingOwner = MutableLiveData<SigningOwner?>()
    val defaultFiat = MutableLiveData<String>()

    fun loadUserDefaultFiat() {
        viewModelScope.launch {
            val userDefaultFiat = settingsHandler.userDefaultFiat
            defaultFiat.postValue(userDefaultFiat)
        }
    }

    fun loadSigningOwner() {
        viewModelScope.launch {
            if (credentialsRepository.ownerCount() > 0) {
                val ownerCredentials = credentialsRepository.owners()[0]
                signingOwner.postValue(SigningOwner(ownerCredentials.address))
            } else {
                signingOwner.postValue(null)
            }
        }
    }

    fun removeSigningOwner() {
        viewModelScope.launch {
            //FIXME: adjust for multiple owners
            credentialsRepository.removeOwner(credentialsRepository.owners()[0])
            notificationRepository.unregisterOwner()
            tracker.setNumKeysImported(0)
            signingOwner.postValue(null)
        }
    }
}

data class SigningOwner(
    val address: Solidity.Address
)
