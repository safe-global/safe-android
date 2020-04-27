package io.gnosis.safe.ui.safe.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.launch
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class AddSafeViewModel
@Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<BaseStateViewModel.State>(appDispatchers) {

    private val safeRepository = repositories.safeRepository()

    fun submitAddress(address: String) {
        safeLaunch {
            runCatching {
                updateState { CaptureSafe(ViewAction.Loading(true)) }
                val validSafe = safeRepository.isValidSafe(address.asEthereumAddress() ?: throw InvalidSafeAddress())
                updateState {
                    if (validSafe) {
                        CaptureSafe(
                            ViewAction.NavigateTo(
                                AddSafeFragmentDirections.actionAddSafeFragmentToAddSafeNameFragment(address)
                            )
                        )
                    } else {
                        CaptureSafe(ViewAction.ShowError(InvalidSafeAddress()))
                    }
                }
            }.onFailure {
                updateState { CaptureSafe(ViewAction.ShowError(it)) }
            }
        }
    }

    override val state: LiveData<State> = liveData {
        for (event in stateChannel.openSubscription()) emit(event)
    }

    override fun initialState(): State = CaptureSafe(ViewAction.Loading(false))

}

class InvalidSafeAddress : Throwable()

data class CaptureSafe(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
