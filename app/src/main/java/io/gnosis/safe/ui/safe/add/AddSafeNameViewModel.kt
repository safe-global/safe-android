package io.gnosis.safe.ui.safe.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.gnosis.data.models.Safe
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class AddSafeNameViewModel
@Inject constructor(
    repositories: Repositories
) : BaseStateViewModel<BaseStateViewModel.State>() {

    private val safeRepository = repositories.safeRepository()

    fun submitAddressAndName(address: Solidity.Address, localName: String) {
        viewModelScope.launch {
            updateState { CaptureSafeName(ViewAction.Loading(true)) }
            runCatching {
                safeRepository.addSafe(Safe(address, localName))
            }.onFailure {
                updateState { CaptureSafeName(ViewAction.ShowError(it)) }
            }.onSuccess {
                updateState { CaptureSafeName(ViewAction.CloseScreen) }
            }
        }
    }

    override val state: LiveData<State> = liveData {
        for (event in stateChannel.openSubscription()) emit(event)
    }

    override fun initialState(): State = CaptureSafeName(ViewAction.Loading(false))

}


data class CaptureSafeName(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
