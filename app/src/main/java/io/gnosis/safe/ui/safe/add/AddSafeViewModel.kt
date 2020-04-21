package io.gnosis.safe.ui.safe.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.di.modules.ApplicationModule
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import javax.inject.Inject

class AddSafeViewModel
@Inject constructor(
    repositories: Repositories
) : BaseStateViewModel<AddSafeState>() {

    private val safeRepository = repositories.safeRepository()

    fun submitAddress(safeAddress: Solidity.Address) {
        viewModelScope.launch {
            safeRepository.isValidSafe(safeAddress)
        }
    }

    override val state: LiveData<AddSafeState> = liveData {
        for (event in stateChannel.openSubscription()) emit(event)
    }

    override fun initialState(): AddSafeState = CaptureSafe(null)

}

sealed class AddSafeState : BaseStateViewModel.State

data class CaptureSafe(
    override var viewAction: BaseStateViewModel.ViewAction?
) : AddSafeState()
