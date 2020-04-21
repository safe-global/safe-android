package io.gnosis.safe.ui.safe.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.launch
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class AddSafeViewModel
@Inject constructor(
    repositories: Repositories
) : BaseStateViewModel<AddSafeState>() {

    private val safeRepository = repositories.safeRepository()

    fun submitAddress(address: String) {
        viewModelScope.launch {
            runCatching {
                val validSafe = safeRepository.isValidSafe(address.asEthereumAddress()!!)
                updateState {
                    if (validSafe) {
                        CaptureSafe(
                            ViewAction.NavigateTo(
                                AddSafeFragmentDirections.actionAddSafeFragmentToAddSafeNameFragment(address)
                            )
                        )

                    } else {
                        InvalidAddress
                    }
                }
            }
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

object InvalidAddress : AddSafeState() {
    override var viewAction: BaseStateViewModel.ViewAction? = null
}
