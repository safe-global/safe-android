package io.gnosis.safe.ui.safe.add

import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
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
                updateState { AddSafeState(ViewAction.Loading(true)) }
                val newAddress = address.asEthereumAddress() ?: throw InvalidSafeAddress
                val validSafe = safeRepository.isValidSafe(newAddress)
                val isAddressUsed = safeRepository.isSafeAddressUsed(newAddress)
                val viewAction = when {
                    isAddressUsed -> ViewAction.ShowError(UsedSafeAddress)
                    validSafe -> ViewAction.NavigateTo(AddSafeFragmentDirections.actionAddSafeFragmentToAddSafeNameFragment(address))
                    else -> ViewAction.ShowError(InvalidSafeAddress)
                }
                updateState {
                    AddSafeState(viewAction)
                }
            }.onFailure {
                updateState { AddSafeState(ViewAction.ShowError(it)) }
            }
        }
    }

    override fun initialState(): State = AddSafeState(ViewAction.Loading(false))
}

object InvalidSafeAddress : Throwable()
object UsedSafeAddress : Throwable()

data class AddSafeState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
