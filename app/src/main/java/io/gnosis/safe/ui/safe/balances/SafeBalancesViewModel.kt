package io.gnosis.safe.ui.safe.balances

import io.gnosis.data.models.Safe
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

class SafeBalancesViewModel @Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeBalancesState>(appDispatchers) {

    private val safeRepository = repositories.safeRepository()

    override fun initialState(): SafeBalancesState = SafeBalancesState.SafeLoading(null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    SafeBalancesState.ActiveSafe(
                        safe, if (safe == null)
                            ViewAction.NavigateTo(
                                SafeBalancesFragmentDirections.actionSafeBalancesFragmentToNoSafeFragment()
                            )
                        else null
                    )
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
}
