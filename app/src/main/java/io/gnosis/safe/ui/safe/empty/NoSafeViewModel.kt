package io.gnosis.safe.ui.safe.empty

import io.gnosis.data.models.Safe
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

class NoSafeViewModel @Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafesState>(appDispatchers) {

    private val safeRepository = repositories.safeRepository()

    override fun initialState() = SafesState(null, 0, ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                val safeNum = safeRepository.getSafes().count()
                updateState {
                    SafesState(
                        safe, safeNum, if (safe == null) ViewAction.None else ViewAction.NavigateTo(
                            NoSafeFragmentDirections.actionNoSafeFragmentToSafeBalancesFragment()
                        )
                    )
                }
            }
        }
    }
}

data class SafesState(
    val activeSafe: Safe?,
    val safeNum: Int,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

