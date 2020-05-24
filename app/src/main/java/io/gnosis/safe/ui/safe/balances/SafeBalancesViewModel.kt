package io.gnosis.safe.ui.safe.balances

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

class SafeBalancesViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeBalancesState>(appDispatchers) {

    override fun initialState(): SafeBalancesState = SafeBalancesState.SafeLoading(null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    if (safe == null)
                        SafeBalancesState.NoActiveSafe(null)
                    else
                        SafeBalancesState.ActiveSafe(safe, null)
                }
            }
        }
    }
}

sealed class SafeBalancesState : BaseStateViewModel.State {

    data class SafeLoading(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeBalancesState()

    data class ActiveSafe(
        val safe: Safe?,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeBalancesState()

    data class NoActiveSafe(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeBalancesState()
}
