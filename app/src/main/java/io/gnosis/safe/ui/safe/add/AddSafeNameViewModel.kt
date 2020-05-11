package io.gnosis.safe.ui.safe.add

import io.gnosis.data.models.Safe
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.model.Solidity
import javax.inject.Inject

class AddSafeNameViewModel
@Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers,
    private val tracker: Tracker
) : BaseStateViewModel<BaseStateViewModel.State>(appDispatchers) {

    private val safeRepository = repositories.safeRepository()

    fun submitAddressAndName(address: Solidity.Address, localName: String) {
        safeLaunch {
            localName.takeUnless { it.isBlank() } ?: run {
                updateState { AddSafeNameState(ViewAction.ShowError(InvalidName())) }
                return@safeLaunch
            }
            updateState { AddSafeNameState(ViewAction.Loading(true)) }
            runCatching {
                val safe = Safe(address, localName.trim())
                safeRepository.addSafe(safe)
                safeRepository.setActiveSafe(safe)
            }.onFailure {
                updateState { AddSafeNameState(ViewAction.ShowError(it)) }
            }.onSuccess {
                updateState { AddSafeNameState(ViewAction.CloseScreen) }
                tracker.logSafeAdd(safeRepository.getSafes().count())
            }
        }
    }

    override fun initialState(): State = AddSafeNameState(ViewAction.Loading(false))
}

class InvalidName : Throwable()

data class AddSafeNameState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
