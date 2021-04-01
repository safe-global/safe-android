package io.gnosis.safe.ui.settings.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.gnosis.data.repositories.CredentialsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppSettingsViewModel @Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler
) : ViewModel() {

    val signingOwnerCount = MutableLiveData<Int?>()
    val defaultFiat = MutableLiveData<String>()

    fun loadUserDefaultFiat() {
        viewModelScope.launch {
            val userDefaultFiat = settingsHandler.userDefaultFiat
            defaultFiat.postValue(userDefaultFiat)
        }
    }

    fun loadSigningOwner() {
        viewModelScope.launch {
            val ownerCount = credentialsRepository.ownerCount()
            signingOwnerCount.postValue(ownerCount)
        }
    }
}
