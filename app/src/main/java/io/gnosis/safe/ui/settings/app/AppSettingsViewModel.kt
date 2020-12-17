package io.gnosis.safe.ui.settings.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SettingsRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.utils.OwnerCredentialsRepository
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import javax.inject.Inject

class AppSettingsViewModel @Inject constructor(
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val tracker: Tracker
) : ViewModel() {

    val signingOwner = MutableLiveData<SigningOwner?>()
    val defaultFiat = MutableLiveData<String>()

    fun loadUserDefaultFiat() {
        viewModelScope.launch {
            val userDefaultFiat = settingsRepository.getUserDefaultFiat()
            defaultFiat.postValue(userDefaultFiat)
        }
    }

    fun loadSigningOwner() {
        if (ownerCredentialsRepository.hasCredentials()) {
            val ownerCredentials = ownerCredentialsRepository.retrieveCredentials()!!
            signingOwner.postValue(SigningOwner(ownerCredentials.address))
        } else {
            signingOwner.postValue(null)
        }
    }

    fun removeSigningOwner() {
        viewModelScope.launch {
            ownerCredentialsRepository.removeCredentials()
            notificationRepository.unregisterOwner()
            tracker.setNumKeysImported(0)
            signingOwner.postValue(null)
        }
    }
}

data class SigningOwner(
    val address: Solidity.Address
)
