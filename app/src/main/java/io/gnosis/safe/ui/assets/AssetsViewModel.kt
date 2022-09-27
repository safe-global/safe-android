package io.gnosis.safe.ui.assets

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class AssetsViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeBalancesState>(appDispatchers) {

    override fun initialState(): SafeBalancesState = SafeBalancesState.SafeLoading(null)

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState {
                    SafeBalancesState.ActiveSafe(safe, null)
                }
            }
        }
    }

    fun updateTotalBalance(balance: String) {
        safeLaunch {
            updateState {
                SafeBalancesState.TotalBalance(balance, null)
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

    data class TotalBalance(
        val totalBalance: String,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) :  SafeBalancesState()
}
