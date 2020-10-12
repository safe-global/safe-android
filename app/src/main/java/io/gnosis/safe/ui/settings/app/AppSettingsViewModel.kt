package io.gnosis.safe.ui.settings.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.gnosis.safe.utils.OwnerCredentialsRepository
import pm.gnosis.model.Solidity
import javax.inject.Inject

class AppSettingsViewModel @Inject constructor(
    private val ownerCredentialsRepository: OwnerCredentialsRepository
) : ViewModel() {

    val signingOwner = MutableLiveData<SigningOwner?>()

    fun loadSigningOwner() {
        if (ownerCredentialsRepository.hasCredentials()) {
            val ownerCredentials = ownerCredentialsRepository.retrieveCredentials()!!
            signingOwner.postValue(SigningOwner(ownerCredentials.address))
        } else {
            signingOwner.postValue(null)
        }
    }

    fun removeSigningOwner() {
        ownerCredentialsRepository.removeCredentials()
        signingOwner.postValue(null)
    }
}

data class SigningOwner(
    val address: Solidity.Address
)
