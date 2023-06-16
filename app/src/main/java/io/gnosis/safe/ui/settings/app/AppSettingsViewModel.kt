package io.gnosis.safe.ui.settings.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.HeimdallIntercom
import io.gnosis.safe.Tracker
import io.intercom.android.sdk.UnreadConversationCountListener
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppSettingsViewModel @Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    private val tracker: Tracker
) : ViewModel(), UnreadConversationCountListener {

    val signingOwnerCount = MutableLiveData<Int?>()
    val defaultFiat = MutableLiveData<String>()
    val intercomCount = MutableLiveData<Int>()

    init {
        HeimdallIntercom.addUnreadConversationCountListener(this)
    }

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

    fun loadIntercomCount() {
        onCountUpdate(HeimdallIntercom.unreadConversationCount())
    }

    override fun onCountUpdate(count: Int) {
        intercomCount.postValue(count)
    }

    fun openIntercomMessenger() {
        HeimdallIntercom.present()
        tracker.logIntercomChatOpened()
    }

    override fun onCleared() {
        super.onCleared()
        HeimdallIntercom.removeUnreadConversationCountListener(this)
    }
}
